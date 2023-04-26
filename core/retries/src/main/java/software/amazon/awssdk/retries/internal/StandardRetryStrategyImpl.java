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
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucketStore;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class StandardRetryStrategyImpl extends GenericRetryStrategy implements StandardRetryStrategy {
    private static final Logger LOG = Logger.loggerFor(StandardRetryStrategy.class);

    public StandardRetryStrategyImpl(GenericRetryStrategy.Builder builder) {
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
        builder.setTreatAsThrottling(t -> false);
        return builder;
    }

    public static class Builder extends GenericRetryStrategy.Builder implements StandardRetryStrategy.Builder {
        private Builder() {
        }

        private Builder(StandardRetryStrategyImpl strategy) {
            super(strategy);
        }

        @Override
        public StandardRetryStrategy.Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            setBackoffStrategy(backoffStrategy);
            return this;
        }

        @Override
        public StandardRetryStrategy.Builder circuitBreakerEnabled(Boolean circuitBreakerEnabled) {
            setCircuitBreakerEnabled(circuitBreakerEnabled);
            return this;
        }

        @Override
        public StandardRetryStrategy.Builder retryOnException(Predicate<Throwable> shouldRetry) {
            setRetryOnException(shouldRetry);
            return this;
        }

        @Override
        public StandardRetryStrategy.Builder maxAttempts(int maxAttempts) {
            setMaxAttempts(maxAttempts);
            return this;
        }

        public Builder tokenBucketExceptionCost(int exceptionCost) {
            setTokenBucketExceptionCost(exceptionCost);
            return this;
        }

        public Builder tokenBucketStore(TokenBucketStore tokenBucketStore) {
            setTokenBucketStore(tokenBucketStore);
            return this;
        }


        @Override
        public StandardRetryStrategy build() {
            return new StandardRetryStrategyImpl(this);
        }
    }
}
