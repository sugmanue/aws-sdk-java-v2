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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Represents a collection of token buckets keyed by scope.
 */
@SdkInternalApi
public interface TokenBucketStore extends ToCopyableBuilder<TokenBucketStore.Builder, TokenBucketStore> {

    /**
     * Returns the token bucket for the given scope.
     *
     * @param scope The scope used to retrieve the token bucket.
     * @return the token bucket for the given scope.
     */
    TokenBucket tokenBucketForScope(String scope);

    @Override
    Builder toBuilder();

    static TokenBucketStore.Builder builder() {
        return DefaultTokenBucketStore.builder();
    }

    interface Builder extends CopyableBuilder<TokenBucketStore.Builder, TokenBucketStore> {
        Builder tokenBucketMaxCapacity(int tokenBucketMaxCapacity);
    }

}
