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

import static software.amazon.awssdk.retries.internal.ratelimiter.DefaultRateLimiterTokenBucket.PersistentState;

import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class DefaultRateLimiterUpdateResponse implements RateLimiterUpdateResponse {
    private final PersistentState before;
    private final PersistentState after;

    public DefaultRateLimiterUpdateResponse(PersistentState before, PersistentState after) {
        this.before = before;
        this.after = after;
    }

    public PersistentState before() {
        return before;
    }

    public PersistentState after() {
        return after;
    }

    @Override
    public double measuredTxRate() {
        return after.measuredTxRate();
    }

    @Override
    public double fillRate() {
        return after.fillRate();
    }
}
