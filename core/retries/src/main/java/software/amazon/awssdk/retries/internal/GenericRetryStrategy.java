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

package software.amazon.awssdk.retries.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.api.internal.AcquireInitialTokenResponseImpl;
import software.amazon.awssdk.retries.internal.circuitbreaker.AcquireResponse;
import software.amazon.awssdk.retries.internal.circuitbreaker.ReleaseResponse;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucket;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterAcquireResponse;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucket;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterUpdateResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * generic retry strategy
 */
@SdkInternalApi
public class GenericRetryStrategy {
    private final Logger log;
    private final List<Predicate<Throwable>> predicates;
    private final int maxAttempts;
    private final boolean circuitBreakerEnabled;
    private final BackoffStrategy backoffStrategy;
    private final int tokenBucketMaxCapacity;
    private final int exceptionCost;
    private final int throttlingExceptionCost;
    private final Predicate<Throwable> treatAsThrottling;
    private final TokenBucketStore tokenBucketStore;
    private final RateLimiterTokenBucketStore rateLimiterTokenBucketStore;

    protected GenericRetryStrategy(Logger log, Builder builder) {
        this.log = log;
        this.predicates = Collections.unmodifiableList(Validate.paramNotNull(builder.predicates, "predicates"));
        this.maxAttempts = Validate.isPositive(builder.maxAttempts, "maxAttempts");
        this.circuitBreakerEnabled = builder.circuitBreakerEnabled;
        this.backoffStrategy = Validate.paramNotNull(builder.backoffStrategy, "backoffStrategy");
        this.exceptionCost = builder.exceptionCost;
        this.throttlingExceptionCost = builder.throttlingExceptionCost;
        this.tokenBucketMaxCapacity = builder.tokenBucketMaxCapacity;
        this.treatAsThrottling = Validate.paramNotNull(builder.treatAsThrottling, "treatAsThrottling");
        this.tokenBucketStore = Validate.paramNotNull(builder.tokenBucketStore, "tokenBucketStore");
        this.rateLimiterTokenBucketStore = Validate.paramNotNull(builder.rateLimiterTokenBucketStore, "adaptiveTokenBucketStore");
    }

    public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
        logAcquireInitialToken(request);
        return AcquireInitialTokenResponseImpl.create(
            DefaultRetryToken.builder().scope(request.scope()).build(), Duration.ZERO);
    }

    public RefreshRetryTokenResponse refreshRetryToken(RefreshRetryTokenRequest request) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        AcquireResponse acquireResponse = requestAcquireCapacity(request, token);

        // Check if we meet the preconditions needed for retrying. The following calls will throw if the expected condition is
        // not meet.
        // 1) is retryable?
        throwOnNonRetryableException(request, acquireResponse);
        // 2) max attempts reached?
        throwOnMaxAttemptsReached(request, acquireResponse);
        // 3) can we acquire a token?
        throwOnAcquisitionFailure(request, acquireResponse);

        // Refresh the retry token and compute the final delay.
        DefaultRetryToken refreshedToken = refreshToken(request, acquireResponse);
        Duration backoff = backoffStrategy.computeDelay(refreshedToken.attempt());
        Duration suggested = request.suggestedDelay().orElse(Duration.ZERO);
        Duration rateLimiterDelay = computeRateLimiterDelay(token, request);
        // Final delay, we take the maximum between the suggested and the
        // computed by the backoff strategy, and we add up the delay computed
        // by the rate limiter.
        Duration finalDelay = maxOf(suggested, backoff).plus(rateLimiterDelay);

        logRefreshTokenSuccess(refreshedToken, acquireResponse, finalDelay);
        return RefreshRetryTokenResponse.create(refreshedToken, finalDelay);
    }

    public RecordSuccessResponse recordSuccess(RecordSuccessRequest request) {
        DefaultRetryToken token = asStandardRetryToken(request.token());

        // Update the adaptive token bucket.
        updateRateLimiterTokenBucket(token);

        // Update the circuit breaker token bucket.
        ReleaseResponse releaseResponse = updateCircuitBreakerTokenBucket(token);

        // Refresh the retry token
        DefaultRetryToken refreshedToken = refreshRetryTokenAfterSuccess(token, releaseResponse);

        // Log success and return.
        logRecordSuccess(token, releaseResponse);
        return RecordSuccessResponse.create(refreshedToken);
    }


    /**
     * Returns a builder to fine-tune this retry strategy.
     *
     * @return a builder for this retry strategy.
     */
    public static Builder builder() {
        return new Builder();
    }

    private Duration maxOf(Duration left, Duration right) {
        if (left.compareTo(right) >= 0) {
            return left;
        }
        return right;
    }

    private RateLimiterUpdateResponse updateRateLimiterTokenBucket(DefaultRetryToken token) {
        RateLimiterTokenBucket rateLimiterTokenBucket = rateLimiterTokenBucketStore.tokenBucketForScope(token.scope());
        return rateLimiterTokenBucket.updateRateAfterSuccess();
    }

    private Duration computeRateLimiterDelay(DefaultRetryToken token, RefreshRetryTokenRequest request) {
        Throwable failure = request.failure();
        Duration rateLimiterDelay = Duration.ZERO;
        if (treatAsThrottling.test(failure)) {
            RateLimiterTokenBucket rateLimiterTokenBucket = rateLimiterTokenBucketStore.tokenBucketForScope(token.scope());
            rateLimiterTokenBucket.updateRateAfterThrottling();
            RateLimiterAcquireResponse rateLimiterAcquireResponse = rateLimiterTokenBucket.tryAcquire(1);
            rateLimiterDelay = rateLimiterAcquireResponse.delay();
        }
        return rateLimiterDelay;
    }

    private ReleaseResponse updateCircuitBreakerTokenBucket(DefaultRetryToken token) {
        TokenBucket bucket = tokenBucketStore.tokenBucketForScope(token.scope());
        int capacityReleased = token.capacityAcquired();
        return bucket.release(capacityReleased);
    }

    private DefaultRetryToken refreshRetryTokenAfterSuccess(DefaultRetryToken token, ReleaseResponse releaseResponse) {
        return token.toBuilder()
                    .capacityAcquired(0)
                    .capacityRemaining(releaseResponse.currentCapacity())
                    .state(DefaultRetryToken.TokenState.SUCCEEDED)
                    .build();
    }

    private void throwOnAcquisitionFailure(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        if (acquireResponse.acquisitionFailed()) {
            Throwable failure = request.failure();
            DefaultRetryToken refreshedToken =
                token.toBuilder()
                     .capacityRemaining(acquireResponse.capacityRemaining())
                     .capacityAcquired(acquireResponse.capacityAcquired())
                     .state(DefaultRetryToken.TokenState.TOKEN_ACQUISITION_FAILED)
                     .addFailure(failure)
                     .build();
            String message = acquisitionFailedMessage(acquireResponse);
            log.error(() -> message, failure);
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
    }

    private void throwOnMaxAttemptsReached(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        if (maxAttemptsReached(token)) {
            Throwable failure = request.failure();
            DefaultRetryToken refreshedToken =
                token.toBuilder()
                     .capacityRemaining(acquireResponse.capacityRemaining())
                     .capacityAcquired(acquireResponse.capacityAcquired())
                     .state(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                     .addFailure(failure)
                     .build();
            String message = maxAttemptsReachedMessage(refreshedToken);
            log.error(() -> message, failure);
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
    }

    private void throwOnNonRetryableException(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        Throwable failure = request.failure();
        if (isNonRetryableException(request)) {
            String message = nonRetryableExceptionMessage(token);
            log.error(() -> message, failure);
            DefaultRetryToken refreshedToken =
                token.toBuilder()
                     .capacityRemaining(acquireResponse.capacityRemaining())
                     .capacityAcquired(acquireResponse.capacityAcquired())
                     .state(DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION)
                     .addFailure(failure)
                     .build();
            throw new TokenAcquisitionFailedException(message, refreshedToken, failure);
        }
        int attempt = token.attempt();
        log.warn(() -> String.format("Request attempt %d encountered retryable failure.", attempt), failure);
    }

    private String nonRetryableExceptionMessage(DefaultRetryToken token) {
        return String.format("Request attempt %d encountered non-retryable failure", token.attempt());
    }

    private String maxAttemptsReachedMessage(DefaultRetryToken token) {
        return String.format("Request will not be retried. Retries have been exhausted "
                             + "(cost: 0, capacity: %d/%d)",
                             token.capacityAcquired(),
                             token.capacityRemaining());
    }

    private String acquisitionFailedMessage(AcquireResponse acquireResponse) {
        return String.format("Request will not be retried to protect the caller and downstream service. "
                             + "The cost of retrying (%d) "
                             + "exceeds the available retry capacity (%d/%d).",
                             acquireResponse.capacityRequested(),
                             acquireResponse.capacityRemaining(),
                             acquireResponse.maxCapacity());
    }

    private void logAcquireInitialToken(AcquireInitialTokenRequest request) {
        // Request attempt 1 token acquired (backoff: 0ms, cost: 0, capacity: 500/500)
        TokenBucket tokenBucket = tokenBucketStore.tokenBucketForScope(request.scope());
        log.debug(() -> String.format("Request attempt 1 token acquired "
                                      + "(backoff: 0ms, cost: 0, capacity: %d/%d)",
                                      tokenBucket.currentCapacity(), tokenBucket.maxCapacity()));
    }

    private void logRefreshTokenSuccess(DefaultRetryToken token, AcquireResponse acquireResponse, Duration delay) {
        log.debug(() -> String.format("Request attempt %d token acquired "
                                      + "(backoff: %dms, cost: %d, capacity: %d/%d)",
                                      token.attempt(), delay.toMillis(),
                                      acquireResponse.capacityAcquired(),
                                      acquireResponse.capacityRemaining(),
                                      acquireResponse.maxCapacity()));
    }

    private void logRecordSuccess(DefaultRetryToken token, ReleaseResponse release) {
        log.debug(() -> String.format("Request attempt %d succeeded (cost: -%d, capacity: %d/%d)",
                                      token.attempt(), release.capacityReleased(),
                                      release.currentCapacity(), release.maxCapacity()));

    }

    private boolean maxAttemptsReached(DefaultRetryToken token) {
        return token.attempt() >= maxAttempts;
    }

    private boolean isNonRetryableException(RefreshRetryTokenRequest request) {
        Throwable failure = request.failure();
        return predicates.stream().noneMatch(predicate -> predicate.test(failure));
    }

    static DefaultRetryToken asStandardRetryToken(RetryToken token) {
        return Validate.isInstanceOf(DefaultRetryToken.class, token,
                                     "RetryToken is of unexpected class (%s), "
                                     + "This token was not created by this retry strategy.",
                                     token.getClass().getName());
    }

    private AcquireResponse requestAcquireCapacity(RefreshRetryTokenRequest request, DefaultRetryToken token) {
        TokenBucket tokenBucket = tokenBucketStore.tokenBucketForScope(token.scope());
        Throwable failure = request.failure();
        if (treatAsThrottling.test(failure)) {
            return tokenBucket.tryAcquire(throttlingExceptionCost);
        }
        return tokenBucket.tryAcquire(exceptionCost);
    }

    private DefaultRetryToken refreshToken(RefreshRetryTokenRequest request, AcquireResponse acquireResponse) {
        DefaultRetryToken token = asStandardRetryToken(request.token());
        return token.toBuilder()
                    .increaseAttempt()
                    .state(DefaultRetryToken.TokenState.IN_PROGRESS)
                    .capacityAcquired(acquireResponse.capacityAcquired())
                    .capacityRemaining(acquireResponse.capacityRemaining())
                    .addFailure(request.failure())
                    .build();
    }

    public static class Builder {
        private List<Predicate<Throwable>> predicates;
        private int maxAttempts;
        private boolean circuitBreakerEnabled;
        private int tokenBucketMaxCapacity;
        private int exceptionCost;
        private int throttlingExceptionCost;
        private Predicate<Throwable> treatAsThrottling;
        private BackoffStrategy backoffStrategy;
        private TokenBucketStore tokenBucketStore;
        private RateLimiterTokenBucketStore rateLimiterTokenBucketStore;

        Builder() {
            predicates = new ArrayList<>();
        }

        Builder(GenericRetryStrategy strategy) {
            this.predicates = new ArrayList<>(strategy.predicates);
            this.maxAttempts = strategy.maxAttempts;
            this.circuitBreakerEnabled = strategy.circuitBreakerEnabled;
            this.tokenBucketMaxCapacity = strategy.tokenBucketMaxCapacity;
            this.exceptionCost = strategy.exceptionCost;
            this.treatAsThrottling = strategy.treatAsThrottling;
            this.backoffStrategy = strategy.backoffStrategy;
            this.tokenBucketStore = strategy.tokenBucketStore;
            this.rateLimiterTokenBucketStore = strategy.rateLimiterTokenBucketStore;
        }

        public void setRetryOnException(Predicate<Throwable> shouldRetry) {
            this.predicates.add(shouldRetry);
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public void setTreatAsThrottling(Predicate<Throwable> treatAsThrottling) {
            this.treatAsThrottling = treatAsThrottling;
        }

        public void setTokenBucketStore(TokenBucketStore tokenBucketStore) {
            this.tokenBucketStore = tokenBucketStore;
        }

        public void setAdaptiveTokenBucketStore(RateLimiterTokenBucketStore rateLimiterTokenBucketStore) {
            this.rateLimiterTokenBucketStore = rateLimiterTokenBucketStore;
        }

        public void setCircuitBreakerEnabled(Boolean enabled) {
            if (enabled == null) {
                circuitBreakerEnabled = true;
            } else {
                circuitBreakerEnabled = enabled;
            }
        }

        public void setBackoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
        }

        public void setTokenBucketMaxCapacity(int maxCapacity) {
            this.tokenBucketMaxCapacity = maxCapacity;
        }

        public void setTokenBucketExceptionCost(int exceptionCost) {
            this.exceptionCost = exceptionCost;
        }

        public void setTokenBucketThrottlingExceptionCost(int throttlingExceptionCost) {
            this.throttlingExceptionCost = throttlingExceptionCost;
        }
    }
}
