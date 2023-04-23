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
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.retries.internal.ratelimiter.RateLimiterTokenBucketStore;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public class AdaptiveRetryStrategyImpl extends GenericRetryStrategy implements AdaptiveRetryStrategy {
    static final Duration BASE_DELAY = Duration.ofSeconds(1);
    static final Duration MAX_BACKOFF = Duration.ofSeconds(20);
    private static final Logger LOG = Logger.loggerFor(AdaptiveRetryStrategy.class);

    public AdaptiveRetryStrategyImpl(GenericRetryStrategy.Builder builder) {
        super(LOG, builder);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        Builder builder = new Builder();
        builder.setAdaptiveTokenBucketStore(
            RateLimiterTokenBucketStore.builder()
                                       .build());
        builder.setBackoffStrategy(BackoffStrategy.exponentialDelay(BASE_DELAY, MAX_BACKOFF));
        return builder;
    }

    public static class Builder extends GenericRetryStrategy.Builder implements AdaptiveRetryStrategy.Builder {

        private Builder() {
        }

        private Builder(AdaptiveRetryStrategyImpl strategy) {
            super(strategy);
        }

        @Override
        public AdaptiveRetryStrategy.Builder retryOnException(Predicate<Throwable> shouldRetry) {
            setRetryOnException(shouldRetry);
            return this;
        }

        @Override
        public AdaptiveRetryStrategy.Builder maxAttempts(int maxAttempts) {
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
        public AdaptiveRetryStrategy.Builder treatAsThrottling(Predicate<Throwable> treatAsThrottling) {
            setTreatAsThrottling(treatAsThrottling);
            return this;
        }

        @Override
        public AdaptiveRetryStrategy build() {
            return new AdaptiveRetryStrategyImpl(this);
        }
    }
}
