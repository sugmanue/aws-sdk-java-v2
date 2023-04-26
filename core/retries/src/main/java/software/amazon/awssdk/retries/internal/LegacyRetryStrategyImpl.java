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

import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucketStore;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class LegacyRetryStrategyImpl extends GenericRetryStrategy implements LegacyRetryStrategy {
    private static final Logger LOG = Logger.loggerFor(LegacyRetryStrategy.class);

    public LegacyRetryStrategyImpl(GenericRetryStrategy.Builder builder) {
        super(LOG, builder);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        Builder builder = new Builder();
        builder.setAdaptiveTokenBucketStore(
            RateLimiterTokenBucketStore.builder()
                                       .disabled(true)
                                       .build());
        return builder;
    }

    public static class Builder extends GenericRetryStrategy.Builder implements LegacyRetryStrategy.Builder {
        private Builder() {
        }

        private Builder(LegacyRetryStrategyImpl strategy) {
            super(strategy);
        }

        @Override
        public LegacyRetryStrategy.Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            setBackoffStrategy(backoffStrategy);
            return this;
        }

        @Override
        public LegacyRetryStrategy.Builder circuitBreakerEnabled(Boolean circuitBreakerEnabled) {
            setCircuitBreakerEnabled(circuitBreakerEnabled);
            return this;
        }

        @Override
        public LegacyRetryStrategy.Builder retryOnException(Predicate<Throwable> shouldRetry) {
            setRetryOnException(shouldRetry);
            return this;
        }

        @Override
        public LegacyRetryStrategy.Builder maxAttempts(int maxAttempts) {
            setMaxAttempts(maxAttempts);
            return this;
        }

        @Override
        public LegacyRetryStrategy.Builder treatAsThrottling(Predicate<Throwable> treatAsThrottling) {
            setTreatAsThrottling(treatAsThrottling);
            return this;
        }

        public Builder tokenBucketExceptionCost(int exceptionCost) {
            setTokenBucketExceptionCost(exceptionCost);
            return this;
        }

        public Builder tokenBucketThrottlingExceptionCost(int exceptionCost) {
            setTokenBucketThrottlingExceptionCost(exceptionCost);
            return this;
        }

        public Builder tokenBucketStore(TokenBucketStore tokenBucketStore) {
            setTokenBucketStore(tokenBucketStore);
            return this;
        }

        @Override
        public LegacyRetryStrategy build() {
            return new LegacyRetryStrategyImpl(this);
        }
    }
}
