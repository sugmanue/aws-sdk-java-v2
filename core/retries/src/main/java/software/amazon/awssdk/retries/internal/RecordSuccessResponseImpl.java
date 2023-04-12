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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.circuitbreaker.ReleaseResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation of the {@link RecordSuccessResponse} interface.
 */
@SdkInternalApi
public final class RecordSuccessResponseImpl implements RecordSuccessResponse {
    private final RetryToken token;
    private final ReleaseResponse releaseResponse;

    private RecordSuccessResponseImpl(RetryToken token, ReleaseResponse releaseResponse) {
        this.token = Validate.paramNotNull(token, "token");
        this.releaseResponse = Validate.paramNotNull(releaseResponse, "releaseResponse");
    }

    @Override
    public RetryToken token() {
        return token;
    }

    public ReleaseResponse circuitBreakerReleaseResponse() {
        return releaseResponse;
    }

    /**
     * Creates a new {@link RecordSuccessResponseImpl} with the given token and responses.
     */
    static RecordSuccessResponse create(RetryToken token, ReleaseResponse releaseResponse) {
        return new RecordSuccessResponseImpl(token, releaseResponse);
    }
}
