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

package software.amazon.awssdk.protocol.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;

public class StreamingRequestInputStreamLengthAwareTest {
    private ProtocolRestJsonClient client;
    private SdkHttpClient mockHttpClient;


    @BeforeEach
    public void setup() throws IOException {
        mockHttpClient = mock(SdkHttpClient.class);

        HttpExecuteResponse response = HttpExecuteResponse.builder()
                                                          .response(SdkHttpResponse.builder()
                                                                                   .statusCode(200)
                                                                                   .putHeader("Content-Length", "2")
                                                                                   .build())
                                                          .responseBody(AbortableInputStream.create(
                                                              new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))))
                                                          .build();

        ExecutableHttpRequest mockExecutableRequest = mock(ExecutableHttpRequest.class);
        when(mockExecutableRequest.call()).thenReturn(response);

        when(mockHttpClient.prepareRequest(any())).thenReturn(mockExecutableRequest);

        client = ProtocolRestJsonClient.builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
            .region(Region.US_WEST_2)
            .httpClient(mockHttpClient)
            .build();
    }

    @AfterEach
    public void teardown() {
        client.close();
    }

    @Test
    public void streamingRequest_specifiedLengthTruncatesStream_lengthHonored() throws IOException {
        int bodyLength = 16;

        client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                       bodyFromInputStream(32, bodyLength));
        ArgumentCaptor<HttpExecuteRequest> requestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);

        verify(mockHttpClient).prepareRequest(requestCaptor.capture());

        HttpExecuteRequest executeRequest = requestCaptor.getValue();
        ContentStreamProvider streamProvider = executeRequest.contentStreamProvider().get();
        String contentLengthHeader = executeRequest.httpRequest().firstMatchingHeader("Content-Length").get();

        assertThat(contentLengthHeader).isEqualTo(Integer.toString(bodyLength));
        assertThat(drainStream(streamProvider.newStream())).isEqualTo(bodyLength);
    }


    public static Stream<Arguments> requestBodies() {
        return Stream.of(Arguments.of(bodyFromInputStream(32, 64), 64),
                         Arguments.of(bodyFromContentProvider(32, 50), 50));
    }

    @ParameterizedTest
    @MethodSource("requestBodies")
    public void streamingRequest_streamLessThanContentLength_shouldThrowException(RequestBody body, long contentLength) throws IOException {
        assertThatThrownBy(() -> client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                                                body)).isInstanceOf(IllegalStateException.class)
                                                                      .hasMessageContaining("The request content has fewer "
                                                                                            + "bytes than the specified "
                                                                                            + "content-length");
    }

    private static RequestBody bodyFromInputStream(int streamLength, int contentLength) {
        InputStream is = new ByteArrayInputStream(new byte[streamLength]);
        return RequestBody.fromInputStream(is, contentLength);
    }

    private static RequestBody bodyFromContentProvider(int streamLength, int contentLength) {
        InputStream is = new ByteArrayInputStream(new byte[streamLength]);
        return RequestBody.fromContentProvider(() -> is, contentLength, Mimetype.MIMETYPE_OCTET_STREAM);
    }

    private static int drainStream(InputStream is) throws IOException {
        byte[] buff = new byte[32];

        int totalRead = 0;
        while (true) {
            int read = is.read(buff);
            if (read == -1) {
                break;
            }
            totalRead += read;
        }

        return totalRead;
    }
}
