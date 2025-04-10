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

package software.amazon.awssdk.services.polly.internal.presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.presigner.PollyPresigner;
import software.amazon.awssdk.services.polly.presigner.model.PresignedSynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.presigner.model.SynthesizeSpeechPresignRequest;

/**
 * Tests for {@link DefaultPollyPresigner}.
 */
class DefaultPollyPresignerTest {
    private static final String EXPECTED_SIGNATURE = "06e9f816eaa937ba5a7eb5e536d3339276bd5f90c2a351b9e21cd57a0a6921be";
    private static final SynthesizeSpeechRequest BASIC_SYNTHESIZE_SPEECH_REQUEST = SynthesizeSpeechRequest.builder()
            .voiceId("Salli")
            .outputFormat(OutputFormat.PCM)
            .text("Hello presigners!")
            .build();

    private IdentityProvider<AwsCredentialsIdentity> credentialsProvider;

    @BeforeEach
    public void methodSetup() {
        credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    @Test
    void presign_requestLevelCredentials_honored() {
        IdentityProvider<AwsCredentialsIdentity> requestCedentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("akid2", "skid2")
        );

        Instant fixedTime = Instant.parse("2024-02-20T22:00:00Z");
        PollyPresigner presigner = DefaultPollyPresigner.builder(Clock.fixed(fixedTime, ZoneId.of("UTC")))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();

        SynthesizeSpeechRequest synthesizeSpeechRequest = BASIC_SYNTHESIZE_SPEECH_REQUEST.toBuilder()
                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                        .credentialsProvider(requestCedentialsProvider).build())
                .build();

        SynthesizeSpeechPresignRequest presignRequest = SynthesizeSpeechPresignRequest.builder()
                .synthesizeSpeechRequest(synthesizeSpeechRequest)
                .signatureDuration(Duration.ofHours(3))
                .build();

        PresignedSynthesizeSpeechRequest presignedSynthesizeSpeechRequest = presigner.presignSynthesizeSpeech(presignRequest);
        String query = presignedSynthesizeSpeechRequest.url().getQuery();
        assertThat(query).contains("X-Amz-Credential=akid2");
        assertThat(query).contains("X-Amz-Signature=" + EXPECTED_SIGNATURE);
    }

    @Test
    void presign_requestLevelSignerAndCredentials_honored() {
        IdentityProvider<AwsCredentialsIdentity> requestCedentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("akid2", "skid2")
        );

        Instant fixedTime = Instant.parse("2024-02-20T22:00:00Z");
        PollyPresigner presigner = DefaultPollyPresigner.builder(Clock.fixed(fixedTime, ZoneId.of("UTC")))
                                                        .region(Region.US_EAST_1)
                                                        .credentialsProvider(credentialsProvider)
                                                        .build();

        SynthesizeSpeechRequest synthesizeSpeechRequest =
            BASIC_SYNTHESIZE_SPEECH_REQUEST.toBuilder()
                                           .overrideConfiguration(AwsRequestOverrideConfiguration
                                                                      .builder()
                                                                      .signer(Aws4Signer.create())
                                                                      .credentialsProvider(requestCedentialsProvider).build())
                                           .build();

        SynthesizeSpeechPresignRequest presignRequest = SynthesizeSpeechPresignRequest.builder()
                                                                                      .synthesizeSpeechRequest(synthesizeSpeechRequest)
                                                                                      .signatureDuration(Duration.ofHours(3))
                                                                                      .build();

        PresignedSynthesizeSpeechRequest presignedSynthesizeSpeechRequest = presigner.presignSynthesizeSpeech(presignRequest);
        String query = presignedSynthesizeSpeechRequest.url().getQuery();
        assertThat(query).contains("X-Amz-Credential=akid2");
        assertThat(query).contains("X-Amz-Signature=" + EXPECTED_SIGNATURE);
    }

    @Test
    void presign_requestLevelHeaders_included() {
        PollyPresigner presigner = DefaultPollyPresigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();

        SynthesizeSpeechRequest synthesizeSpeechRequest = BASIC_SYNTHESIZE_SPEECH_REQUEST.toBuilder()
                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                        .putHeader("Header1", "Header1Value")
                        .putHeader("Header2", "Header2Value")
                        .build())
                .build();

        SynthesizeSpeechPresignRequest presignRequest = SynthesizeSpeechPresignRequest.builder()
                .synthesizeSpeechRequest(synthesizeSpeechRequest)
                .signatureDuration(Duration.ofHours(3))
                .build();

        PresignedSynthesizeSpeechRequest presignedSynthesizeSpeechRequest = presigner.presignSynthesizeSpeech(presignRequest);

        assertThat(presignedSynthesizeSpeechRequest.httpRequest().headers().keySet()).contains("Header1", "Header2");
    }

    @Test
    void presign_includesRequestLevelHeaders_notBrowserCompatible() {
        PollyPresigner presigner = DefaultPollyPresigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();

        SynthesizeSpeechRequest synthesizeSpeechRequest = BASIC_SYNTHESIZE_SPEECH_REQUEST.toBuilder()
                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                        .putHeader("Header1", "Header1Value")
                        .putHeader("Header2", "Header2Value")
                        .build())
                .build();

        SynthesizeSpeechPresignRequest presignRequest = SynthesizeSpeechPresignRequest.builder()
                .synthesizeSpeechRequest(synthesizeSpeechRequest)
                .signatureDuration(Duration.ofHours(3))
                .build();

        PresignedSynthesizeSpeechRequest presignedSynthesizeSpeechRequest = presigner.presignSynthesizeSpeech(presignRequest);

        assertThat(presignedSynthesizeSpeechRequest.isBrowserExecutable()).isFalse();
    }

    @Test
    void presign_includesRequestLevelQueryParams_included() {
        PollyPresigner presigner = DefaultPollyPresigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .build();

        SynthesizeSpeechRequest synthesizeSpeechRequest = BASIC_SYNTHESIZE_SPEECH_REQUEST.toBuilder()
                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                        .putRawQueryParameter("QueryParam1", "Param1Value")
                        .build())
                .build();

        SynthesizeSpeechPresignRequest presignRequest = SynthesizeSpeechPresignRequest.builder()
                .synthesizeSpeechRequest(synthesizeSpeechRequest)
                .signatureDuration(Duration.ofHours(3))
                .build();

        PresignedSynthesizeSpeechRequest presignedSynthesizeSpeechRequest = presigner.presignSynthesizeSpeech(presignRequest);

        assertThat(presignedSynthesizeSpeechRequest.httpRequest().rawQueryParameters().keySet()).contains("QueryParam1");
    }

    @Test
    void presign_endpointOverriden() {
        PollyPresigner presigner = DefaultPollyPresigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create("http://some-other-polly-endpoint.aws:1234"))
                .build();

        SynthesizeSpeechPresignRequest presignRequest = SynthesizeSpeechPresignRequest.builder()
                .synthesizeSpeechRequest(BASIC_SYNTHESIZE_SPEECH_REQUEST)
                .signatureDuration(Duration.ofHours(3))
                .build();

        PresignedSynthesizeSpeechRequest presignedSynthesizeSpeechRequest = presigner.presignSynthesizeSpeech(presignRequest);

        URL presignedUrl = presignedSynthesizeSpeechRequest.url();

        assertThat(presignedUrl.getProtocol()).isEqualTo("http");
        assertThat(presignedUrl.getHost()).isEqualTo("some-other-polly-endpoint.aws");
        assertThat(presignedUrl.getPort()).isEqualTo(1234);
    }

    @Test
    void close_closesCustomCloseableCredentialsProvider() throws IOException {
        TestCredentialsProvider mockCredentialsProvider = mock(TestCredentialsProvider.class);

        PollyPresigner presigner = DefaultPollyPresigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(mockCredentialsProvider)
                .build();

        presigner.close();

        verify(mockCredentialsProvider).close();
    }

    @Test
    void presigner_credentialsProviderSetAsAwsCredentialsProvider_delegatesCorrectly() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        DefaultPollyPresigner presigner1 = (DefaultPollyPresigner)
            DefaultPollyPresigner.builder()
                                 .region(Region.US_EAST_1)
                                 .credentialsProvider(credentialsProvider)
                                 .build();

        DefaultPollyPresigner presigner2 = (DefaultPollyPresigner)
            DefaultPollyPresigner.builder()
                                 .region(Region.US_EAST_1)
                                 .credentialsProvider((IdentityProvider<AwsCredentialsIdentity>) credentialsProvider)
                                 .build();

        assertThat(presigner1.credentialsProvider()).isSameAs(presigner2.credentialsProvider());
    }

    @Test
    void presigner_credentialsProviderSetToNullByBuilder_createsDefaultCredentialsProvider() {
        DefaultPollyPresigner presigner = (DefaultPollyPresigner)
            DefaultPollyPresigner.builder()
                                 .region(Region.US_EAST_1)
                                 .credentialsProvider(null)
                                 .build();


        IdentityProvider<? extends AwsCredentialsIdentity> awsCredentialsProvider = presigner.credentialsProvider();
        assertThat(awsCredentialsProvider).isNotNull();
    }

    private interface TestCredentialsProvider extends IdentityProvider<AwsCredentialsIdentity>, Closeable {
    }
}
