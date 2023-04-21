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

package software.amazon.awssdk.retries;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Built-in implementations of the {@link RetryStrategy} interface.
 */
@SdkPublicApi
public final class DefaultRetryStrategy {
    public static final int TOKEN_BUCKET_SIZE = 500;
    public static final Duration MAX_BACKOFF = Duration.ofSeconds(20);
    public static final Duration BASE_DELAY = Duration.ofMillis(100);

    private DefaultRetryStrategy() {
    }

    /**
     * Create a new builder pre-configured for the legacy retry strategy.
     *
     * <p>Example Usage
     * <pre>
     * StandardRetryStrategy retryStrategy =
     *     RetryStrategies.standardStrategyBuilder()
     *                    .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                    .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                    .build();
     * </pre>

     public static RetryStrategy.Builder<InternalRetryStrategy.Builder, InternalRetryStrategy> legacyStrategyBuilder() {
     return InternalRetryStrategy.builder()
     .maxAttempts(Legacy.MAX_ATTEMPTS)
     .backoffStrategy(DefaultBackoffStrategies.exponentialDelay(Legacy.BASE_DELAY, MAX_BACKOFF))
     .circuitBreakerEnabled(true)
     .tokenBucketExceptionCost(Legacy.DEFAULT_EXCEPTION_TOKEN_COST)
     .tokenBucketThrottleExceptionCost(Legacy.THROTTLE_EXCEPTION_TOKEN_COST)
     .tokenBucketMaxCapacity(TOKEN_BUCKET_SIZE)
     .adaptiveTokenBucketStore(RateLimiterTokenBucketStore
     .builder()
     .disabled(true)
     .build())
     .tokenBucketStore(TokenBucketStore
     .builder()
     .tokenBucketMaxCapacity(TOKEN_BUCKET_SIZE).build());
     }
     */

    /**
     * Create a new builder for a {@code StandardRetryStrategy}.
     *
     * <p>Example Usage
     * <pre>
     * StandardRetryStrategy retryStrategy =
     *     RetryStrategies.adaptiveStrategyBuilder()
     *                    .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                    .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                    .build();
     * </pre>
     */
    public static StandardRetryStrategy.Builder standardStrategyBuilder() {
        return StandardRetryStrategy.builder()
                                    .maxAttempts(Standard.MAX_ATTEMPTS)
                                    .backoffStrategy(BackoffStrategy.exponentialDelay(BASE_DELAY, MAX_BACKOFF))
                                    .circuitBreakerEnabled(true);
    }

    /**
     * Create a new builder for a {@code AdaptiveRetryStrategy}.
     *
     * <p>Example Usage
     * <pre>
     * AdaptiveRetryStrategy retryStrategy =
     *     RetryStrategies.adaptiveStrategyBuilder()
     *                    .retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                    .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                    .build();
     * </pre>
     */
    public static AdaptiveRetryStrategy.Builder adaptiveRetryStrategyBuilder() {
        return AdaptiveRetryStrategy.builder()
                                    .maxAttempts(Standard.MAX_ATTEMPTS);
    }

    static final class Legacy {
        private static final int MAX_ATTEMPTS = 4;
        private static final Duration BASE_DELAY = Duration.ofMillis(100);
        private static final Duration THROTTLED_BASE_DELAY = Duration.ofMillis(500);
    }

    static final class Standard {
        private static final int MAX_ATTEMPTS = 3;
    }
}
