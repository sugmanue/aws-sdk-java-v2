/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.retries.internal.ratelimiter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;

@SdkProtectedApi
public class DefaultRateLimiterTokenBucket implements RateLimiterTokenBucket {
    private final AtomicReference<PersistentState> stateReference;
    private final RateLimiterTokenBucketStore.Clock clock;

    private final boolean failFast;

    DefaultRateLimiterTokenBucket(RateLimiterTokenBucketStore.Clock clock, boolean failFast) {
        this.clock = clock;
        this.failFast = failFast;
        this.stateReference = new AtomicReference<>(new PersistentState());
    }

    @Override
    public RateLimiterAcquireResponse tryAcquire(double amountToAcquire) {
        StateUpdate<Duration> update = updateState(ts -> ts.tokenBucketAcquire(clock, amountToAcquire));
        Duration delay = update.result;
        if (failFast && !delay.isZero()) {
            String message =
                String.format("Adaptive mode with fail fast enabled. Will not wait for %dms",
                              delay.toMillis());
            throw new TokenAcquisitionFailedException(message);
        }
        return new DefaultRateLimiterAcquireResponse(update.oldState, update.newState, update.result);
    }

    @Override
    public RateLimiterUpdateResponse updateRateAfterThrottling() {
        StateUpdate<Void> update = consumeState(ts -> ts.updateClientSendingRate(clock, true));
        return new DefaultRateLimiterUpdateResponse(update.oldState, update.newState);
    }

    @Override
    public RateLimiterUpdateResponse updateRateAfterSuccess() {
        StateUpdate<Void> update = consumeState(ts -> ts.updateClientSendingRate(clock, false));
        return new DefaultRateLimiterUpdateResponse(update.oldState, update.newState);
    }

    StateUpdate<Void> consumeState(Consumer<TransientState> mutator) {
        return updateState(ts -> {
            mutator.accept(ts);
            return null;
        });
    }

    <T> StateUpdate<T> updateState(Function<TransientState, T> mutator) {
        PersistentState current;
        PersistentState updated;
        T result;
        do {
            current = stateReference.get();
            TransientState transientState = current.toTransient();
            result = mutator.apply(transientState);
            updated = transientState.toPersistent();
        } while (!stateReference.compareAndSet(current, updated));

        return new StateUpdate<>(current, updated, result);
    }

    static class StateUpdate<T> {
        private final PersistentState oldState;
        private final PersistentState newState;
        private final T result;

        StateUpdate(PersistentState oldState, PersistentState newState, T result) {
            this.oldState = oldState;
            this.newState = newState;
            this.result = result;
        }
    }


    private static class TransientState {
        private static final double MIN_FILL_RATE = 0.5;
        private static final double MIN_CAPACITY = 1.0;
        private static final double SMOOTH = 0.8;
        private static final double BETA = 0.7;
        private static final double SCALE_CONSTANT = 0.4;
        private double fillRate;
        private double maxCapacity;
        private double currentCapacity;
        private boolean lastTimestampIsSet;
        private double lastTimestamp;
        private boolean enabled;
        private double measuredTxRate;
        private double lastTxRateBucket;
        private long requestCount;
        private double lastMaxRate;
        private double lastThrottleTime;
        private double timeWindow;
        private double newTokenBucketRate;

        TransientState(PersistentState state) {
            this.fillRate = state.fillRate;
            this.maxCapacity = state.maxCapacity;
            this.currentCapacity = state.currentCapacity;
            this.lastTimestampIsSet = state.lastTimestampIsSet;
            this.lastTimestamp = state.lastTimestamp;
            this.enabled = state.enabled;
            this.measuredTxRate = state.measuredTxRate;
            this.lastTxRateBucket = state.lastTxRateBucket;
            this.requestCount = state.requestCount;
            this.lastMaxRate = state.lastMaxRate;
            this.lastThrottleTime = state.lastThrottleTime;
            this.timeWindow = state.timeWindow;
            this.newTokenBucketRate = state.newTokenBucketRate;
        }

        PersistentState toPersistent() {
            return new PersistentState(this);
        }

        Duration tokenBucketAcquire(RateLimiterTokenBucketStore.Clock clock, double amount) {
            if (!this.enabled) {
                return Duration.ZERO;
            }
            refill(clock);
            double waitTime = 0.0;
            if (this.currentCapacity < amount) {
                waitTime = (amount - this.currentCapacity) / this.fillRate;
            }
            this.currentCapacity -= amount;
            return Duration.ofNanos((long) (waitTime * 1_000_000_000.0));
        }

        void updateClientSendingRate(RateLimiterTokenBucketStore.Clock clock, boolean throttlingResponse) {
            updateMeasuredRate(clock);
            double calculatedRate;
            if (throttlingResponse) {
                double rateToUse;
                if (!this.enabled) {
                    rateToUse = this.measuredTxRate;
                } else {
                    rateToUse = Math.min(this.measuredTxRate, this.fillRate);
                }

                this.lastMaxRate = rateToUse;
                calculateTimeWindow();
                this.lastThrottleTime = clock.time();
                calculatedRate = cubicThrottle(rateToUse);
                this.enabled = true;
            } else {
                calculateTimeWindow();
                calculatedRate = cubicSuccess(clock.time());
            }

            double newRate = Math.min(calculatedRate, 2 * this.measuredTxRate);
            updateRate(clock, newRate);
        }

        void refill(RateLimiterTokenBucketStore.Clock clock) {
            double timestamp = clock.time();
            if (this.lastTimestampIsSet) {
                double fillAmount = (timestamp - this.lastTimestamp) * this.fillRate;
                this.currentCapacity = Math.min(this.maxCapacity, this.currentCapacity + fillAmount);
            }
            this.lastTimestamp = timestamp;
            this.lastTimestampIsSet = true;
        }

        void updateRate(RateLimiterTokenBucketStore.Clock clock, double newRps) {
            refill(clock);
            this.fillRate = Math.max(newRps, MIN_FILL_RATE);
            this.maxCapacity = Math.max(newRps, MIN_CAPACITY);
            this.currentCapacity = Math.min(this.currentCapacity, this.maxCapacity);
            this.newTokenBucketRate = newRps;
        }

        void updateMeasuredRate(RateLimiterTokenBucketStore.Clock clock) {
            double time = clock.time();
            this.requestCount += 1;
            double timeBucket = Math.floor(time * 2) / 2;
            if (timeBucket > this.lastTxRateBucket) {
                double currentRate = this.requestCount / (timeBucket - this.lastTxRateBucket);
                this.measuredTxRate = (currentRate * SMOOTH) + (this.measuredTxRate * (1 - SMOOTH));
                this.requestCount = 0;
                this.lastTxRateBucket = timeBucket;
            }
        }

        void calculateTimeWindow() {
            this.timeWindow = Math.pow((this.lastMaxRate * (1 - BETA)) / SCALE_CONSTANT, 1.0 / 3);
        }

        double cubicSuccess(double timestamp) {
            double delta = timestamp - this.lastThrottleTime;
            return (SCALE_CONSTANT * Math.pow(delta - this.timeWindow, 3)) + this.lastMaxRate;
        }

        double cubicThrottle(double rateToUse) {
            return rateToUse * BETA;
        }
    }

    static class PersistentState {
        private final double fillRate;
        private final double maxCapacity;
        private final double currentCapacity;
        private final boolean lastTimestampIsSet;
        private final double lastTimestamp;
        private final boolean enabled;
        private final double measuredTxRate;
        private final double lastTxRateBucket;
        private final long requestCount;
        private final double lastMaxRate;
        private final double lastThrottleTime;
        private final double timeWindow;
        private final double newTokenBucketRate;

        PersistentState() {
            this.fillRate = 0;
            this.maxCapacity = 0;
            this.currentCapacity = 0;
            this.lastTimestampIsSet = false;
            this.lastTimestamp = 0;
            this.enabled = false;
            this.measuredTxRate = 0;
            this.lastTxRateBucket = 0;
            this.requestCount = 0;
            this.lastMaxRate = 0;
            this.lastThrottleTime = 0;
            this.timeWindow = 0;
            this.newTokenBucketRate = 0;
        }

        PersistentState(TransientState state) {
            this.fillRate = state.fillRate;
            this.maxCapacity = state.maxCapacity;
            this.currentCapacity = state.currentCapacity;
            this.lastTimestampIsSet = state.lastTimestampIsSet;
            this.lastTimestamp = state.lastTimestamp;
            this.enabled = state.enabled;
            this.measuredTxRate = state.measuredTxRate;
            this.lastTxRateBucket = state.lastTxRateBucket;
            this.requestCount = state.requestCount;
            this.lastMaxRate = state.lastMaxRate;
            this.lastThrottleTime = state.lastThrottleTime;
            this.timeWindow = state.timeWindow;
            this.newTokenBucketRate = state.newTokenBucketRate;
        }

        TransientState toTransient() {
            return new TransientState(this);
        }

        public double fillRate() {
            return fillRate;
        }

        public double measuredTxRate() {
            return measuredTxRate;
        }

    }
}
