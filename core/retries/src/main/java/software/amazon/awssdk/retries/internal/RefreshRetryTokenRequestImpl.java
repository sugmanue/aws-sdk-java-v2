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
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkInternalApi
public final class RefreshRetryTokenRequestImpl
    implements RefreshRetryTokenRequest,
               ToCopyableBuilder<RefreshRetryTokenRequestImpl.Builder, RefreshRetryTokenRequestImpl> {
    private final RetryToken token;
    private final Duration suggestedDelay;
    private final Throwable failure;

    private RefreshRetryTokenRequestImpl(Builder builder) {
        this.token = Validate.paramNotNull(builder.token, "token");
        this.suggestedDelay = builder.suggestedDelay;
        if (this.suggestedDelay != null) {
            Validate.isPositive(this.suggestedDelay, "suggestedDelay");
        }
        this.failure = Validate.paramNotNull(builder.failure, "failure");
    }

    @Override
    public RetryToken token() {
        return token;
    }

    @Override
    public Optional<Duration> suggestedDelay() {
        return Optional.ofNullable(suggestedDelay);
    }

    @Override
    public Throwable failure() {
        return failure;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Returns a new builder to create a new instance of {@link RefreshRetryTokenRequest}
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements CopyableBuilder<Builder, RefreshRetryTokenRequestImpl> {
        private RetryToken token;
        private Duration suggestedDelay;
        private Throwable failure;

        Builder(RefreshRetryTokenRequestImpl refreshRetryTokenRequest) {
            this.token = refreshRetryTokenRequest.token;
            this.suggestedDelay = refreshRetryTokenRequest.suggestedDelay;
            this.failure = refreshRetryTokenRequest.failure;
        }

        Builder() {
        }

        public Builder token(RetryToken token) {
            this.token = token;
            return this;
        }

        public Builder suggestedDelay(Duration duration) {
            this.suggestedDelay = duration;
            return this;
        }

        public Builder failure(Throwable throwable) {
            this.failure = throwable;
            return this;
        }

        @Override
        public RefreshRetryTokenRequestImpl build() {
            return new RefreshRetryTokenRequestImpl(this);
        }
    }

}
