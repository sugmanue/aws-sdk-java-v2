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
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.backoff.DefaultBackoffStrategies;
import software.amazon.awssdk.utils.Validate;

/**
 * Built-in implementations of the {@link RetryStrategy} interface.
 */
@SdkPublicApi
public final class DefaultRetryStrategy {
    public static final int TOKEN_BUCKET_SIZE = 500;
    public static final Duration MAX_BACKOFF = Duration.ofSeconds(20);

    public enum RetryMode {
        /**
         * The STANDARD retry mode, shared by all SDK implementations, and characterized by:
         * <ol>
         *     <li>Up to 2 retries, regardless of service.</li>
         *     <li>Throttling exceptions are treated the same as other exceptions for the purposes.</li>
         * </ol>
         */
        STANDARD,
    }

    private DefaultRetryStrategy() {
    }

    /**
     * Create a new builder pre-configured for the standard retry strategy.
     */
    public static StandardRetryStrategy.Builder standardStrategyBuilder() {
        return StandardRetryStrategy.builder()
                                    .maxAttempts(maxAttempts(RetryMode.STANDARD))
                                    .backoffStrategy(DefaultBackoffStrategies.exponentialDelay(baseDelay(RetryMode.STANDARD),
                                                                                               MAX_BACKOFF))
                                    .circuitBreakerEnabled(true);
    }

    static final class Standard {
        private static final int MAX_ATTEMPTS = 3;
        private static final Duration BASE_DELAY = Duration.ofMillis(100);
    }

    static int maxAttempts(RetryMode retryMode) {
        int maxAttempts;
        switch (retryMode) {
            case STANDARD:
                maxAttempts = Standard.MAX_ATTEMPTS;
                break;
            default:
                throw new IllegalStateException("Unsupported RetryMode: " + retryMode);
        }

        Validate.isPositive(maxAttempts, "Maximum attempts must be positive, but was " + maxAttempts);

        return maxAttempts;
    }

    static Duration baseDelay(RetryMode retryMode) {
        switch (retryMode) {
            case STANDARD:
                return Standard.BASE_DELAY;
            default:
                throw new IllegalStateException("Unsupported RetryMode: " + retryMode);
        }
    }
}
