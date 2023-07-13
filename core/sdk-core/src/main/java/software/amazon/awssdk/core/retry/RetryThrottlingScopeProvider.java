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

package software.amazon.awssdk.core.retry;

import software.amazon.awssdk.http.SdkHttpFullRequest;

@FunctionalInterface
public interface RetryThrottlingScopeProvider {
    /**
     * Returns the throttling scope for the given request. This scope will be used to create the rate limiting token bucket.
     * Requests operating on the same resource SHOULD resolve to the same scope while operations operating on different
     * resources should resolve to different scopes to avoid "Action at a distance" where being throttled on one resource leads
     * to slowing down requests to other unrealted resources.
     */
    String throttlingScope(SdkHttpFullRequest request);
}
