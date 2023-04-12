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

package software.amazon.awssdk.retries.circuitbreaker;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Represents a token bucket. Tokens can be acquired from the bucket as long as there is sufficient capacity in * the bucket.
 */
@SdkProtectedApi
public interface TokenBucket {
    /**
     * Try to acquire a certain number of tokens from this bucket. If there aren't sufficient tokens in this bucket then
     * {@link AcquireResponse#acquisitionFailed()} returns {@code true}.
     */
    AcquireResponse tryAcquire(int amountToAcquire);

    /**
     * Release a certain number of tokens back to this bucket. If this number of tokens would exceed the maximum number of tokens
     * configured for the bucket, the bucket is instead set to the maximum value and the additional tokens are discarded.
     */
    ReleaseResponse release(int amountToRelease);

    /**
     * Retrieve a snapshot of the current number of tokens in the bucket. Because this number is constantly changing, it's
     * recommended to refer to the {@link AcquireResponse#capacityRemaining()} returned by the {@link #tryAcquire(int)} method
     * whenever possible.
     */
    int currentCapacity();

    /**
     * Retrieve the maximum capacity of the bucket configured when the bucket was created.
     */
    int maxCapacity();
}
