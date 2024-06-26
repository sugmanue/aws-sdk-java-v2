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

package software.amazon.awssdk.services.s3.checksums;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.CLIENT_TYPE;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_CONFIG;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.CONTENT_LENGTH_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.SERVER_SIDE_ENCRYPTION_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.getObjectChecksumEnabledPerRequest;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.getObjectChecksumEnabledPerResponse;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.responseChecksumIsValid;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.shouldRecordChecksum;
import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AWS_KMS;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ChecksumsEnabledValidatorTest {

    @Test
    public void getObjectChecksumEnabledPerRequest_nonGetObjectRequest_returnsFalse() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectAclRequest.builder().build(),
                                                      new ExecutionAttributes())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerRequest_checksumValidationEnabledChecksumModeDisabled_returnsTrue() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectRequest.builder().build(),
                                                      new ExecutionAttributes())).isTrue();
    }

    @Test
    public void getObjectChecksumEnabledPerRequest_checksumValidationEnabledChecksumModeEnabled_returnsFalse() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectRequest.builder().checksumMode(ChecksumMode.ENABLED).build(),
                                                      new ExecutionAttributes())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerRequest_checksumValidationDisabledChecksumModeDisabled_returnsFalse() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectRequest.builder().build(),
                                                      getExecutionAttributesWithChecksumDisabled())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerRequest_checksumValidationDisabledChecksumModeEnabled_returnsFalse() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectRequest.builder().checksumMode(ChecksumMode.ENABLED).build(),
                                                      getExecutionAttributesWithChecksumDisabled())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerResponse_nonGetObjectRequestFalse() {
        assertThat(getObjectChecksumEnabledPerResponse(GetObjectAclRequest.builder().build(),
                                                       getSdkHttpResponseWithChecksumHeader())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerResponse_responseContainsChecksumHeader_returnTrue() {
        assertThat(getObjectChecksumEnabledPerResponse(GetObjectRequest.builder().build(),
                                                       getSdkHttpResponseWithChecksumHeader())).isTrue();
    }

    @Test
    public void getObjectChecksumEnabledPerResponse_responseNotContainsChecksumHeader_returnFalse() {
        assertThat(getObjectChecksumEnabledPerResponse(GetObjectRequest.builder().build(),
                                                       SdkHttpFullResponse.builder().build())).isFalse();
    }

    @Test
    public void putObjectChecksumEnabled_defaultShouldRecord() {
        assertThat(shouldRecordChecksum(PutObjectRequest.builder().build(),
                                        ClientType.SYNC,
                                        getSyncExecutionAttributes(),
                                        emptyHttpRequest().build())).isTrue();
    }

    @Test
    public void putObjectChecksumEnabled_nonPutObjectRequest_false() {
        assertThat(shouldRecordChecksum(PutBucketAclRequest.builder().build(),
                                        ClientType.SYNC,
                                        getSyncExecutionAttributes(),
                                        emptyHttpRequest().build())).isFalse();
    }

    @Test
    public void putObjectChecksumEnabled_disabledFromConfig_false() {
        ExecutionAttributes executionAttributes = getExecutionAttributesWithChecksumDisabled();

        assertThat(shouldRecordChecksum(PutObjectRequest.builder().build(),
                                        ClientType.SYNC,
                                        executionAttributes,
                                        emptyHttpRequest().build())).isFalse();
    }

    @Test
    public void putObjectChecksumEnabled_wrongClientType_false() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();

        assertThat(shouldRecordChecksum(PutObjectRequest.builder().build(),
                                        ClientType.ASYNC,
                                        executionAttributes,
                                        emptyHttpRequest().build())).isFalse();
    }

    @Test
    public void putObjectChecksumEnabled_serverSideCustomerEncryption_false() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();
        SdkHttpRequest response = emptyHttpRequest().putHeader(SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER, "test")
                                                    .build();

        assertThat(shouldRecordChecksum(PutObjectRequest.builder().build(),
                                        ClientType.SYNC,
                                        executionAttributes,
                                        response)).isFalse();
    }

    @Test
    public void putObjectChecksumEnabled_serverSideEncryption_false() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();
        SdkHttpRequest response = emptyHttpRequest().putHeader(SERVER_SIDE_ENCRYPTION_HEADER, AWS_KMS.toString())
                                                    .build();

        assertThat(shouldRecordChecksum(PutObjectRequest.builder().build(),
                                        ClientType.SYNC,
                                        executionAttributes,
                                        response)).isFalse();
    }

    @Test
    public void putObject_crc32ValueSupplied_shouldNotValidateMd5() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();

        assertThat(shouldRecordChecksum(PutObjectRequest.builder()
                                                        .checksumCRC32("checksumVal")
                                                        .build(),
                                        ClientType.SYNC,
                                        executionAttributes,
                                        emptyHttpRequest().build())).isFalse();
    }

    @Test
    public void putObject_crc32cValueSupplied_shouldNotValidateMd5() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();

        assertThat(shouldRecordChecksum(PutObjectRequest.builder()
                                                        .checksumCRC32C("checksumVal")
                                                        .build(),
                                        ClientType.SYNC,
                                        executionAttributes,
                                        emptyHttpRequest().build())).isFalse();
    }

    @Test
    public void putObject_sha1ValueSupplied_shouldNotValidateMd5() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();

        assertThat(shouldRecordChecksum(PutObjectRequest.builder()
                                                        .checksumSHA1("checksumVal")
                                                        .build(),
                                        ClientType.SYNC,
                                        executionAttributes,
                                        emptyHttpRequest().build())).isFalse();
    }

    @Test
    public void putObject_sha256ValueSupplied_shouldNotValidateMd5() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();

        assertThat(shouldRecordChecksum(PutObjectRequest.builder()
                                                        .checksumSHA256("checksumVal")
                                                        .build(),
                                        ClientType.SYNC,
                                        executionAttributes,
                                        emptyHttpRequest().build())).isFalse();
    }

    @Test
    public void responseChecksumIsValid_defaultTrue() {
        assertThat(responseChecksumIsValid(SdkHttpResponse.builder().build())).isTrue();
    }

    @Test
    public void responseChecksumIsValid_serverSideCustomerEncryption_false() {
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .putHeader(SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER, "test")
                                                  .build();

        assertThat(responseChecksumIsValid(response)).isFalse();
    }

    @Test
    public void responseChecksumIsValid_serverSideEncryption_false() {
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .putHeader(SERVER_SIDE_ENCRYPTION_HEADER, AWS_KMS.toString())
                                                  .build();

        assertThat(responseChecksumIsValid(response)).isFalse();
    }

    private ExecutionAttributes getSyncExecutionAttributes() {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(CLIENT_TYPE, ClientType.SYNC);
        return executionAttributes;
    }

    private ExecutionAttributes getExecutionAttributesWithChecksumDisabled() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();
        executionAttributes.putAttribute(SERVICE_CONFIG, S3Configuration.builder().checksumValidationEnabled(false).build());
        return executionAttributes;
    }

    private SdkHttpResponse getSdkHttpResponseWithChecksumHeader() {
        return SdkHttpResponse.builder()
                              .putHeader(CONTENT_LENGTH_HEADER, "100")
                              .putHeader(CHECKSUM_ENABLED_RESPONSE_HEADER, ENABLE_MD5_CHECKSUM_HEADER_VALUE)
                              .build();
    }

    private SdkHttpRequest.Builder emptyHttpRequest() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.GET)
                                 .protocol("https")
                                 .host("localhost")
                                 .port(80);
    }

}
