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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkProtectedApi
public interface RateLimiterTokenBucketStore
    extends
    ToCopyableBuilder<RateLimiterTokenBucketStore.Builder, RateLimiterTokenBucketStore> {

    RateLimiterTokenBucket tokenBucketForScope(String scope);

    interface Clock {
        /**
         * Returns the current time in seconds, and should include sub second resolution.
         *
         * @return the current time in seconds, and should include sub second resolution
         */
        double time();
    }

    static Builder builder() {
        return DefaultRateLimiterTokenBucketStore.builder();
    }

    interface Builder extends CopyableBuilder<Builder, RateLimiterTokenBucketStore> {

        Builder clock(RateLimiterTokenBucketStore.Clock clock);

        Builder disabled(boolean disabled);

        Builder failFast(boolean failFast);

        RateLimiterTokenBucketStore build();
    }
}
