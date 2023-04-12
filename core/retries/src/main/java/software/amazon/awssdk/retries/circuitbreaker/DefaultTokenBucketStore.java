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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * A store to keep token buckets per scope.
 */
@SdkInternalApi
public final class DefaultTokenBucketStore implements TokenBucketStore {
    private static final int DEFAULT_MAX_CAPACITY = 500;
    private final int tokenBucketMaxCapacity;
    private final Map<String, DefaultTokenBucket> scopeToTokenBucket;

    public DefaultTokenBucketStore(int tokenBucketMaxCapacity) {
        this.scopeToTokenBucket = new ConcurrentHashMap<>();
        this.tokenBucketMaxCapacity = Validate.isPositive(tokenBucketMaxCapacity, "tokenBucketMaxCapacity");
    }

    DefaultTokenBucketStore(Builder builder) {
        this.tokenBucketMaxCapacity = builder.tokenBucketMaxCapacity;
        this.scopeToTokenBucket = new ConcurrentHashMap<>();
    }

    @Override
    public TokenBucket tokenBucketForScope(String scope) {
        return scopeToTokenBucket.computeIfAbsent(scope,
                                                  key -> new DefaultTokenBucket(tokenBucketMaxCapacity));
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static TokenBucketStore.Builder builder() {
        return new Builder();
    }

    public static class Builder implements TokenBucketStore.Builder {
        private int tokenBucketMaxCapacity;

        Builder() {
            tokenBucketMaxCapacity = DEFAULT_MAX_CAPACITY;
        }

        Builder(DefaultTokenBucketStore store) {
            this.tokenBucketMaxCapacity = store.tokenBucketMaxCapacity;
        }

        @Override
        public Builder tokenBucketMaxCapacity(int tokenBucketMaxCapacity) {
            this.tokenBucketMaxCapacity = tokenBucketMaxCapacity;
            return this;
        }

        @Override
        public TokenBucketStore build() {
            return new DefaultTokenBucketStore(this);
        }
    }

}
