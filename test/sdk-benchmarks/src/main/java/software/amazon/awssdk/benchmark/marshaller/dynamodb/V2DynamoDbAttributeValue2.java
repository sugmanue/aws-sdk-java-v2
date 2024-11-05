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

package software.amazon.awssdk.benchmark.marshaller.dynamodb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.protocols.json.internal.unmarshall.SdkClientJsonProtocolAdvancedOption;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BackupInUseException;
import software.amazon.awssdk.services.dynamodb.model.BackupNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ContinuousBackupsUnavailableException;
import software.amazon.awssdk.services.dynamodb.model.DuplicateItemException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ExportConflictException;
import software.amazon.awssdk.services.dynamodb.model.ExportNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalTableAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.GlobalTableNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.IdempotentParameterMismatchException;
import software.amazon.awssdk.services.dynamodb.model.ImportConflictException;
import software.amazon.awssdk.services.dynamodb.model.ImportNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.IndexNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.InternalServerErrorException;
import software.amazon.awssdk.services.dynamodb.model.InvalidExportTimeException;
import software.amazon.awssdk.services.dynamodb.model.InvalidRestoreTimeException;
import software.amazon.awssdk.services.dynamodb.model.ItemCollectionSizeLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.LimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.PointInTimeRecoveryUnavailableException;
import software.amazon.awssdk.services.dynamodb.model.PolicyNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReplicaAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.ReplicaNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.RequestLimitExceededException;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableAlreadyExistsException;
import software.amazon.awssdk.services.dynamodb.model.TableInUseException;
import software.amazon.awssdk.services.dynamodb.model.TableNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactionConflictException;
import software.amazon.awssdk.services.dynamodb.model.TransactionInProgressException;
import software.amazon.awssdk.services.dynamodb.transform.PutItemRequestMarshaller;
import software.amazon.awssdk.utils.IoUtils;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
public class V2DynamoDbAttributeValue2 {

    private static final SmithyRpcV2CborProtocolFactory RPCV2_PROTOCOL_FACTORY2 =
        init(SmithyRpcV2CborProtocolFactory.builder()).build();

    private static final AwsJsonProtocolFactory JSON_PROTOCOL_FACTORY = AwsJsonProtocolFactory
        .builder()
        .clientConfiguration(SdkClientConfiguration
                                 .builder()
                                 .option(SdkClientOption.ENDPOINT, URI.create("https://localhost"))
                                 .option(SdkClientJsonProtocolAdvancedOption.ENABLE_FAST_UNMARSHALLER, true)
                                 .build())
        .defaultServiceExceptionSupplier(DynamoDbException::builder)
        .protocol(AwsJsonProtocol.AWS_JSON)
        .protocolVersion("1.0")
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("ResourceInUseException")
                             .exceptionBuilderSupplier(ResourceInUseException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("TableAlreadyExistsException")
                             .exceptionBuilderSupplier(TableAlreadyExistsException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("GlobalTableAlreadyExistsException")
                             .exceptionBuilderSupplier(GlobalTableAlreadyExistsException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("InvalidRestoreTimeException")
                             .exceptionBuilderSupplier(InvalidRestoreTimeException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("ReplicaAlreadyExistsException")
                             .exceptionBuilderSupplier(ReplicaAlreadyExistsException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("ConditionalCheckFailedException")
                             .exceptionBuilderSupplier(ConditionalCheckFailedException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("BackupNotFoundException")
                             .exceptionBuilderSupplier(BackupNotFoundException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("IndexNotFoundException")
                             .exceptionBuilderSupplier(IndexNotFoundException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("LimitExceededException")
                             .exceptionBuilderSupplier(LimitExceededException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("GlobalTableNotFoundException")
                             .exceptionBuilderSupplier(GlobalTableNotFoundException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("ItemCollectionSizeLimitExceededException")
                             .exceptionBuilderSupplier(ItemCollectionSizeLimitExceededException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("ReplicaNotFoundException")
                             .exceptionBuilderSupplier(ReplicaNotFoundException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("TableNotFoundException")
                             .exceptionBuilderSupplier(TableNotFoundException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("BackupInUseException")
                             .exceptionBuilderSupplier(BackupInUseException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("ResourceNotFoundException")
                             .exceptionBuilderSupplier(ResourceNotFoundException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("ContinuousBackupsUnavailableException")
                             .exceptionBuilderSupplier(ContinuousBackupsUnavailableException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("TableInUseException")
                             .exceptionBuilderSupplier(TableInUseException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("ProvisionedThroughputExceededException")
                             .exceptionBuilderSupplier(ProvisionedThroughputExceededException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("PointInTimeRecoveryUnavailableException")
                             .exceptionBuilderSupplier(PointInTimeRecoveryUnavailableException::builder).build())
        .registerModeledException(
            ExceptionMetadata.builder().errorCode("InternalServerError")
                             .exceptionBuilderSupplier(InternalServerErrorException::builder).build())
        .build();

    private static final PutItemRequestMarshaller PUT_ITEM_REQUEST_MARSHALLER
        = new PutItemRequestMarshaller(getJsonProtocolFactory());

    private static HttpResponseHandler<GetItemResponse> getItemResponseJsonResponseHandler() {
        return RPCV2_PROTOCOL_FACTORY2.createResponseHandler(JsonOperationMetadata.builder()
                                                                                  .isPayloadJson(true)
                                                                                  .hasStreamingSuccessResponse(false)
                                                                                  .build(),
                                                             GetItemResponse::builder);
    }

    @Benchmark
    public Object putItem(PutItemState s) {
        return putItemRequestMarshaller().marshall(s.getReq());
    }

    @Benchmark
    public Object getItem(GetItemState s) throws Exception {
        SdkHttpFullResponse resp = fullResponse(s.testItem);
        return getItemResponseJsonResponseHandler().handle(resp, new ExecutionAttributes());
    }

    @State(Scope.Benchmark)
    public static class PutItemState {
        @Param( {"TINY", "SMALL", "HUGE"})
        private TestItem testItem;

        private PutItemRequest req;

        @Setup
        public void setup() {
            req = PutItemRequest.builder().item(testItem.getValue()).build();
        }

        public PutItemRequest getReq() {
            return req;
        }
    }

    @State(Scope.Benchmark)
    public static class GetItemState {
        @Param( {"TINY", "SMALL", "HUGE"})
        private TestItemUnmarshalling testItem;
    }

    public enum TestItem {
        TINY,
        SMALL,
        HUGE,
        HUGE2;

        private static final AbstractItemFactory<AttributeValue> FACTORY = new V2ItemFactory();

        private Map<String, AttributeValue> av;

        static {
            TINY.av = FACTORY.tiny();
            SMALL.av = FACTORY.small();
            HUGE.av = FACTORY.huge();
            HUGE2.av = FACTORY.huge2();
        }

        public Map<String, AttributeValue> getValue() {
            return av;
        }
    }

    public enum TestItemUnmarshalling {
        TINY,
        SMALL,
        HUGE,
        HUGE2;

        private byte[] utf8;

        static {
            TINY.utf8 = toUtf8ByteArray(TestItem.TINY.av);
            SMALL.utf8 = toUtf8ByteArray(TestItem.SMALL.av);
            HUGE.utf8 = toUtf8ByteArray(TestItem.HUGE.av);
            HUGE2.utf8 = Base64.getDecoder().decode("v2RJdGVtv2doYXNoS2V5v2FTcG1laHhjamVleWt3cWpwcmP/anN0cmluZ0F0dHK/YVN5P"
                                                    +
                                                    "/1ra291ZGltanB0d3lzeW91dWV5dHdsaWlmZm12bWFyZWFuZ2RmdGZhdmFqb2lud3VrbGluZmZpaG1ma3llc3llY2R3dmx2Z3Z3bGdmbmRsaWFxcGJ0aHNkYnZsZ2ZqZW91d3lid2Jjam5jd25sc3pmaGpzeWluZWVuamRzemNsdWNqcWxnY3ZtbGFhenVkY2h4Z2xucHZ3a2ZqeGRiYXVxYXhscm1icGhyaXNqdXZmaXB3cWR5ZWN6YXNjb2ZkcndiZXN3bGlqeWplbXFnZ2ZhbXVjZXpuenp6bG9jdWRnc2p4dnRmb3BpdWJjcWVyem54aG1maXVjd3hlY2NibW15ZGpyamV1cWp4eWxreWtmYnhwanZmcmNxaGh5aGJkYmN5cWJ4eGpvenhueGFwYmt5c3FsdG1xbnFibGZneG5sd2JqZ3ZzdXBrenNrbGtucnlmaHpyb21ka2F4b2l2Z2lxdHN0anJtcXphdXhueGFzZmJjZWd6d3locm94c2NxZnVreGtnZXFzYXN3bGl4ZHB1Y29ncXhvdmZhbXFpcWtzZW5hY3J6bnNjd210bmNjY2FqeXVxeHBuamtmZHpycHZ1ZGp4dmlpaGZkemxsb2x2cGllaGRtempoaHFlbG52ZnBhbXlrcmRhZXJnb25jcm5zcXBjeWtxcGF5ZWh6eHFxcGh3Y2R2bnlsc2J4dXdoZnhodnJxbWJ2Znpoc2x6ZWZsa2VoZHl5cmV4eXF3ZnpqZmhiaXRla255cWhsdXlxbXVjb2FsaWFqcG15c2ZrcHBpbmdxcnZ1ZWJxZ2VuZmRuY3Z1dWJ1c3hhbWttc2JjYmZmbWpmZHZjb2pndW5scm91Z3NhZnptbnZxb2RzbWV2aXdkeHRmbmZibm5qbHZ3cmhrcWlhanh5YWtwdmlneWdwbXltemV0eXNsYXhxYnpudmZtcm12a2VqbG5ud3RpZXl4eWtnY2tudnlzcGJjbmhjYWZnZnZ2bmNhZ2x6amd2dnRyeHdueXNycWV5dnFnYnFvY2Z2ZW15eWFxZ3RtZmtvYm9iZ3B4Z3Rybml2cG92eG9qcm1pdnZ3a2RscnV4c2hwaHB2Z3l6cGtzc2d0YnJiaHNkaGVkcW1ocmliemRleWt4c2ducHZhbG9meHpteHNuam5pbmloamlicHFnY3dmbGpkcnFpbnFvZHdyeGNndmx5eXZiemFwZHN1amZpbm5qcnR5ZmNwdXdjeXZ2dHd6c3JiendmcWpuemp0Y3ZteWNrcWtpZ3pzanNuZGh0d2d4amlhcHppbm53b3Jkcmh4eHF1b3l3aGxxZmJtZGZlZHhza2pqbXN3dWhsc29keXNueHRkdm1henB2cnVvendncHJlcW9ydWNudHVqeWFkb3JhZGlodnl3cnlnaGticGFzdHNlemF3ZG9qbHhseWd2c3NwZ2VvdGtqeWZneGVxaHd1bnpjeGlwYXp3ZGZmb2N4YWdpd2xrZGJ0bGZ6ZmN1b2hjdWlweHllendwb2hoaWRtY3NnZ3ZucWdta3N5a2Z0ZGF1Ynh4b3BhZ21nbGdkd3FpaWRqcXVydHZjb3Bud2pzaXdhcHF2bW5pZ2hjeG9ycmlwcmZva2ZwZGt4ZGVwbGltcHdmcm5ua2J3c2dva2hpZmR6dmpsamhxdHdiZ3NhbWtlcGh3c2JhZnVwaWVmdXVtZm9ydmZhYmV1emhjYmJjbnV4eWlpeHVwanltdHpsdGVyd3Rkc2hpZXdxenV3aGprcWR4bmd3a3dqcmd4bXNob3NramhtcXFjbWFleWZuc3F6cGFua3JvZnBxZndvYnBlb3VteG9taGlzY3ltYWNocnNscmhqc3ZnZHV2Z2N0bGFhcnR6Y2Fuc3FpamNoZ3p5cXBwbWpzeWVrdG53cnJ1emRjYW1wY29naGJxaWtzYXFqbGN6emd0cWZsbmJ0dHB5ZWl4ZGlsdW1idHZpdWRkemV2YWp4cHBteHlodG5oZ2FreGpudGJrZ3Vka2hwb2h1ZmJwdnlheWhwaWttdGJneWF1cnd6dnR5bnRwaGl1b2l4eWJjem5sYXdrcGFjY2pnemZwd2xueXlpcXlxa2JnbW1lcW1raGxoem1remNib2Rvc2R6bGZ2YXFwamJmaWthcmJ1eHZ4d25tbXZ3d3N5ZWlobmtocXVvbWt2bXN1Zmh6ZW1zZWVoZmZvdHpleHpsbHd5Y3ZrY2F6bGFybW9saWpubnJqaHF2bndvb3FpcGltcXRnYm5sdnN4ZWRyemJka2R3dm9ibm9tZHZzeWd2d3hiZWhsY2thZWl1b3d4YWF4ZXVjZGh4ZG1iZWhrb2Vyempia3pha3J3cm9xb3R3bXZ4Y3FtanBoeWVma29hdXRvc3hzY3h4dGx3YXV3cXhmanZ1ZWZ3eGl1c2pudWFpdWJ3cnV4anltb2p3d2J2Y2p1d3N5aGhtaGJqcmFnZHd3cWl0ZGtqcG5rZ2Fjbmpxc2xyZml4ZXZqaXJkb2ducnh1dW54bXJ2anlld3BtdHphdnB3c3Zva29vYWZzdXhremRpZGN3eHdoYmNzbGd6b3picXhjY3ppeGlqeXJpbXp5Z2F1amlzeGFpYWVha3d2cmdjY3ZzdmZocmNnY2lpamFva3plem96YWhxdW1ybHJ3bXl3YWh4ZHdxcHh3bmdveHNjcXN6bXNwZm1nZWx1Ym5tb3ZocG93bmtwZWV0YmNycGpwaGV1amF0cWVnZGx2aWVmdG1sdW94bWtzbWRpY3RrYnBqeXZhdXdiZmdqbnVic3Rmemh1bHF4aWZjd2VkYnNreXZyY3B1a3RsaGVnZ3JqbXVnYWRjbHVqbHRrZW9udGdjb2h0dmJrd29qbWZ2bmZ5ZW9tbXFud2Zkc2FnZGdzZnRqZXRoZHl1bHpnaHF0eXNiZHJ5ZXFrd3BwYnJpdHBjZHJ5emVyYnRubGV1bXNsb2N2ZHVzcGd1YnBwaGlnaHR5ZGJ5aXBrZ2Nid2xqanhoZGxhd2FubmNrcmdveWF1Z3l3bmJib2NwY2Z5c3d2cmN6YnRybmxua2ZrYXluaXZkcmdvcmlqbW91aGNibWphaW9teWtsZnZveWF0bW56d216dHdyb3l4bndheWxyaHhic29rZ2tuenV0aG52d3FvcWF5aHB1eGlzd3VpdGNydGdlbHRneXhlc25od2F2anJ2a2lhbWNvdGN6ZWVobm9tZHZ4bGprd3J2b2hxa2tzZXpnZGZwZ2x0eHFhZnRraGxxeHF6cmF1aGRiYnh1dWtxaW93cHh6bnN6aHlmYnJod2JlZGZyaXpkdXBidXRjc3djc3lwdXNoZGlncnZmcWhzeXhsY2Frd3lnbGlmaGtqaGdwd25idHdmbWFycmpoamJ3aXd6c2RpdGpxa2RibG1xYWt5aXV3amd0ZnlqcWJsZ2JuaWFocXFqbWZwb3d6cndydnVibHVrem91a3l3Zmdkcm5qYmdreGJudGZmdmpkZ3Nra21jY255dnJrb2N2YWxxeGh1cGZkcWhqeHNqd254ZXpkeGd5YWxtYW9zZHJ4b2l4aHBobHZ0aGl3anNrdnRxZnhjaGpyeGx5aXJlbG54b3ZoeWJxd3Bpb3h5dHJ5Y3VsZ2xrZXF2dXlsbG1ibG5jcWNkbmFtem1xbXFsYXZyanBybWRqcm1pbXpjeXJjcnVvZ3N1bmZ2d296aHJ3eXdoaHl2cWt4aGh5dWtzb3Jjc3BkbmFja29xZ2FnY3psamd5ZHp1Z3NwemxkZGJqaXZ1Z3d3ZnR2aHp2c2JoZ3BpbGxmcnZra25jcmlqa21hcGdreG5mamlmYWd1dGlueXd2bXVidWdndXdmYm10ZmNwY2JnaHNqeGlmaGZxa2p3bml4eWd2dGJsYmJ6bXFpYWFoamh3enh2anJpYmhxaGRidm1jeHNzcWdtYndyY3ZoaW5uZ2hhcnZldnNnYW9kamZpYWF1eHZ4ZXZrY21qZWd5cXVtZWVlY2lwYmtsbXNkd2F5dW1uaXhkbW9oa21hZWVieXF5Y2x3bXJkZG16YXhkanN5cmljdHd3YXFwcXhsbnl6eWRkaHVmc2JxcWpuYmpheWh6bHF2cmVsemNocWZsbHVvdm13amF4YWt2ZmFnandmaG9oeW9oc2prcnNzZXBjbGlveGllamNnaHVvd2ZtaGRzY3Z4YnNydWdxZ3h3d3Zxb21ybWR5Z3h4Y2NnYWVucXJsenBydHRnenBzY2l2YXNseHp6d2d4a2l2bXl5ZG50bXpteWFzcWdwemp3bGluZHRkaXZidHF4a2NrdXV1ZGh4bWdhdXl6eWpxY2picG5mZ3dscWdoaHlvd2N2bnZsdWZvYmtjYW1weGRxdGp2Z210b3Frc250aXV0ZWl0ZWxtZXRqaGtkdW9sd3RuZHhneXF2c3BxYmR5ZWRveXBranFtaGNraW9td3NwcWh3bXRycWJtaHd5dG1qY3J4cXp2dmVtZG9rbXppYWlxb2J6ZHN2cWpoanl6Z3prZG5za25oaWdrZmhpYXR2ZHZwZGV6eHB6aWlmZHdqb3pqdHNnbWdpYXVxY212ZGJoZWVmbGZzbWluZ2xtb2hqcHNmcmdneHFzY2Zvbmx3enVydXhkdmRhY2pqc2Z0dnN0bnFodmp1cmhuZGhwc2J5ZGp0bHBmaHl2YnZkb2RveGN4bHh3cmh5ZGpmd3Ztbnh1YWdzc2ZsYmNpd3Z1eWVqaWlla2dpZmtsdmJzemZ5Y3R3eWV3c2psenJtd2tzeGlpeGl3b3B0anJ0dnRha2F6cmVicHVtd3V1Y3NhdGRuYWtqdGlob2RveHBjc3lqZG1nZ3pucHprZ3JlZ3NvbWxjaXJreGNueGFwdG5laXRvY2Z0eG5jbGZoanpmdGp6dnNkeW9neW9odGppeGF1cGdzbnhrd2JzbWVwam1rdHdyd3BvYnBjYWNuem1pc2ZhcGRhd2dwdHJxdGlxcGlvd2FidGVoZHpwYmpwZ3djaXhicm5ya3dldW56cnducnBvanpibmpwdm9hb2JtZW9vZ2xvaHlwbWpkcnF1ZWpzcXFuc3BjbXhucXB5cmN0bnN3cWVxa3J5aGFkeWhnaG9rbG5jcmdpd2x0eGRsbGxyanhjeHNic2VlbmxxaW5mcnhmdnFybmluZWppYmJ2Y2ltZnpvaHNoZHNjbGVrcWlnc3l4a25peXl3aG11bWZzbm1zbGx0b2hpaXhnd3VqZnF4Ynp3dGtod2d4cHVjeGpuZ3BubW5oaWxpZmJlY2hzYnp5ZnB6bWFjdG5qemd1cmxtc2dvd3FobHNpYW1oaXRldHBtYm16YndremVlYW54cGRyY3VraWNkeGFsamprZmtzdWFpd2N6YnlweHh0ZXBveGNka3dtZWp2b3V5cWdwemx4ZWRlaWN5dWFlbnZvZWhuaG5qYnplcXhjc2R6cnhvZHF1Z2tyYWpyY2R1dXVyb2J0ZG92cm16dmFtZ3dia29mamF4aGVpenVxc3dldnVpb3drYXZuYmNhbWtiYWVobG14ZnZ6ZWh2bWRtcHZhcWlveXlkbGhsc2l3c2VuZ3Nldmt2aXhoY3RzcXNqdHZhemNubXpmZndhZ2VsZ3ptZmN0bW5rZXVyaW5xY2NhbGNkZ2JlYXRldGxsZ2NkZWx0dnZudXprbW10dGx6c2txY294c2dzeGJyaG1ydGhzbHJtcmVuY2tzb3ZqZ2ppcm1udHd1eHF5c3dkZ2h5cGhzZnNnZG5scGZxaHZ0bWZ4eHFyeW9kZmt5aGdtb3R0cW1pcnNxZmFueXZqcXVicmVoYnp5cXJuc2Vsd3Z1dm1wc3hvY2p2a3l1cGR3eXlhZW5hc3ZxZWtiZGlpZ3pkY2xyZW13cXR2cHZrZWtwa2Z6anF1ZWhqZXphdm5tcWxmeWxkd2Jzcm9kd2p0dHltZmFzcWdqZ2x0dGFxbHNqZmhneW5yYXZkanlzcXBjYWthYmRicnp4cGV0ZHpubnVjdW92bnJ5dWJhd2l1anRsYXB4aHRwb3pqZnhrbWdmcHpmdW9oeWNidHBienF0b2x5aGdkd3RkcW5xbmlkY25nYXpqcmdiZHF3d3NubWh1aGJ4bnZ2dW9ucnF3dXNhdmViZXdqeHJnanFpdmZucXN4eHhnY2Ztd3Vmdm9iaWRxZGl1cm9jcnVqZG10a2J4bWJ3ZnJ0bXNwbXB1c2xtaW10Z2pidGRlZXZvdWd4bmhtcmlmcXFvZmNzZHR1bHZsY2lvdHRzaHFoaGJ2dGZzbmhobmVwZWJsaHNsZ3ZneHJ0ZXdkdm1obGFqcWdpb2l6cXJ6eXl4bGx6aWt4bHZxdWFrcXBsY3hvaHJibXpmdGlrZHR4ZHF5eXZ6bmdhcmFqZXl5YXVjeWpkcGVwcWx1YnZlcnN1aGZ6c25oZXd1dXJzYW5vYnBzeXFjbGJuZHNpeXNwcHRndmNicGJjeHBqcXhrYXphbHZkbm1sa3FlaGF2dHNld2V2bWZueWtobnN3c3lya2hjbnh1cnd4aHVvc2pxbXloZmpzaGprZ3NmZGppZXZwYWV5anVuaXdha3ZncWhsYnhlZmdhaHVxY2dhem16bW9hZm1vYmJ6am5sYnRmd256ZWR6emhhdWNyY2ppaGJxdG5rcWdydm1obXF4c25iZ2pkdmFqZXZvZGh5ZWdzd3l3a3hudWZhY2d4Z3JqamVhdnVmamZnbWZibXFueGtlZ296dnl4dmRkd25keXh3eHp4b3BkcWZ4YW5sb2Jqamp0cWZ0cmJwemdjY2tpanZja3pmY2dsZXdwbnNzZWJteWJvc3NpcnBobmtoeHhiZmx3c2RobXJscHdyZ2dyaHhycWFxZGNjZmR3a2tnYXducmVnaWxyY3Zqb2x2cGpiZ3lua2JrdXdvaXFoeWpldnV0bmZtaHh6dWNycHlwc2d1ampleHN5Y3VxdGxmb294YnlhbXBuZXlhb3Z3dGRsZG5rb3phd3d0YnhhcWFncXdsa3pubm92d21kZGZ5d3R5eHBpZG1udmlhdGFicWpnZ2lhdm95d3F3Z3ZjYWx3YWtycnVrdGNldHd4bGFuZXp2Y3lvbXFqd3N3cmFoZ3Vza29nb2xycGx1ZWJibnJjY3Fla2VmZHJlemJkcmluZ2V2cmF6cXduYnpjY2F2eWFndGtjbWxieWVrbXh0dG1tZW94bnJld3JmcHh6aGZteGh5dGdpZHV6em1saG54bnhuempubWZsZWxhZ2hvc3puem15dWx0ZWlkbmFiZ2preWlpYXpiZnVyaGNxYnZmdmZ2ZXhodmpza2Frd3VnZmhmZHFrbm51dHNrcWJ4b2p2cXJsc2R3YnN1b3NmZXVnYW5qYXdsdWVmY2h0YnFqeHlzeXd5c295YnpueGZwc3pucGduaWZnZW9peWdueXdrY2Fibm5sbGxma3BzZmhnamlnYnlyZGt6dHJrbnVma2lxZWtxemxneGZieWl0dXRtbWRwdmppYXR1a2xqYmt0dGp0d21nYWdzZGpzZmN5emlwYnlhendpYWNqbWxocGRqZGRsaHdud2Z0ZnJvd3VkdmVpY2d6YmxjZXRhYnZieHdmYWVsaWN5cnJweWNtbXppZXhneGdoZWJpeXhvbWRndGh6a3F5emZpa2NmZHdnbnB0dHhmZXFkaWFvYWpzdW95dnl6bm9odXNib2xpdmdlc2Vqam95enN0b3BkYnZiYmJ2b25kYnVvdHZlZnlrd2x5ZXNmZHRxcXRjdmVhdWtuZmZobnhzZWVianNkY2l2eWRqdXNubW5yb3JwY2hmY3h5bmZtaGt0ZGJ4YWpraXhsbnRrbWZveW5zZWZ3aWRidXpmcG9lZmZvdXZwd290d3h2cWhkcnl6YWlweWJ1YXRzcXBma3ZtdXF1eGh0ZWNvb2hrdWtxbm9xZXR2anNybWpocmdtb2V1Z3pzcGtwYWptYnRvcXBna3JncGJzeWdjZ3libW5pc29jYnJiZ2JzdnV1ZHF6ZXdidm9obXJvb29obm5tb2N5bXRjb2Nob2V6amVsam1zYmZ1eWVibHdha3V6dGRqZ2NtcmRncHVoeGRyZG90amNhZXNqYXlld2ptc3BodHBkeHVtY3hpaWJ6dHlqZXl4d2RvZHdnZGJhbXlzanp2bXd3ZGhpdnd2bmhncmxreWp0aGtkemF3aGN3YXVwc3NvYnF6ZGh5bXN5eHhxZG15emNvb2RvaGtoYXN0aWpndmd4c215enNueGVvZWxiZHdzbXprdnpocnFrZmRiaGt6bG53Z3lpb3NjdGl3YXVyZHlydW1yd3FmZnVvd2lhaHppd3F2c2dvcmlvdGp2ZXh5ZHN4c3Vza25ybHRtdnNsbWJjYnN5ZXpibXRpZG9obmFrcXR3Z3Z4d2pnYXpxaWphaWN3cXFqb3N0c3FoamFseWNwenZybmZ5dG5zaHdja3FhcHNwbnJ2aXJsZWJzZGJiZmVpbGp6aXpkbmRyend5aGd4YndjcXBuY2hmZWZxbHBtbnZzeXB1bmthYnB2cGRybmJ1bnRrZ3BkeGt0c2FsdGVoenh5eW1qY2ppemR0emx6cnhkZXBteG5xY2JycGpvcnpqem1lc3VxaHp3YXlzdHlvdXljaXpmdnNjamp0ZGlnZ3Rpa2p2YXRmc2Jtd3ZybmJ3ZnpuYnN6aXZ6d3J4a3BkZHZldG5obHJweWp3cGFsamhwbHhwaHp2bnVteHBrY29lYW51Z3NwY3B1bndndGFkdnZ6eWxqc3Jtb3Nuamd0emZoZmRyYmhqenR0d3hpbXRycXVhdXJsdnBuYmNvZnN0ZHhxbmV5ZmV1cHNmaGRnZ3dneW1kdGl3bXRkZWFjbHVhaWZlZnB1ZWRmaWpnbW5scndqZXlnandlZHlwa2hkeWZ6cGVjbnJnamx0bWhicnVkb2Njbmhyc3Zwc2ZrYWx0ZmJtZGdpamt4ZXdlb2F0dG93eGtjc3J2aHd1YWp6d2VwbWtyd3ZhbGVwd2d4cXlkZ2Nibmd5am5uenpwbmRra2ZrcXprZHVlYnN6cnppY3dxY29ma3RlemhqcnpxZnpxaW92bGl2YnBxdHBxb2FmcXNmd3N1d2xraHRxb3Nmam1nZXBna2xwbHd4Y3h2aHdqc3RnenNueXFrZGx5a2hiZHl1d2l5YWdscHlub2dhYWdob2h5eHFkbXhvdW5iYXdxaHlscmxxcHdxbWp2dnR6d2ZpdnplcG5odG9sdnhtaHpzeWN1a3hvY2ZmbWRvaW1rdGxteG5wYnNqYml6dWxpZGFtZHpidXN3eHZheGJ1ZXd0YWZ5bnFkdWx2Ym9kaXZidmxkd3BpbWR6aWFjc3RycGpyc3dqanNuYnJzd3JwYWplanRwcmJ1bG1ueXNlcHJtY3d1bWZ6ZmZvcm5ud3BmZW5xbmlqaWVwc293eHRxbnh3bGJzdXZ3bm92dmxqY3l2aGd0ZXFpYmt2dmV4d2pnbmxvem9ldnphZm50YWJiZ2twam55a2dpaXVhZXN6a29qc3ZubXpnbmRtZGdzaWJqZXJhb3R6aHd5YWhpcGlrdG1leml4bGp2cGxhbGd6Y2lndXpweWplcGprdmN4ZWtta3JzcXZqcGZ5cG9ycmVqZWlhaW9tcWR2ZmhnbXJudmVoZnN0YWZoeW1sam9kZ3dsbGJwbGFjZGZmeG11Ynl2ZWpuZmVib2JlZ2dybnlscm95cW9ieHp4bWlxdmFydXpxeG54cHVpa3N0Z29hdW1pc2lpa25qcGdxb3Z4dWJ2bHlseHhrYnppZGl2a2h1enhmcGlueHNzcmp1ZWtkdHZwcmp5anFqdXhsc2V6bGF3ZHRvbGd4emlzdHZlaHh0d3V5b3F4d2F6Y2F0ZWtlaXhvdGVtZXZvdHNpbXB3YXR5ZGViZWRhc2ZramdncnJiaWJ6dGtveWlkd2VwZ2hib2pjeGRnbndzcXRkY3ZnZXZsbnlza3Rmbmd2bWNqaXp3YnNqaXZvZHFvaWN4ZGF2b2lqcmpseml4dWN2dHJ6bWJhb2ZudG1mc2ZpdndtdWZtdHNxdnF2dGVzYmxmdW5pa3lwaXhudXFxd2VxaXJnemd3b2NvZXp4bWVqc2dhZW5yeW9hd2lyZGhkdHF4bmxqamFjdnNkaGZ1enBjb3FvbXFuaXJoYmlkenpzd2t6aG55eHJtZmNleHZ5bXZxcmxhZ3BidWR2dHV5YmZtYWJiZWhvZXdveHFudnZibGxzbHR6cnVzcnB4Z3JsenF0YWZ1eGRncmV6d2JtcmZ6bXN2ZnZkeXZya2F1d290dHF1ZXd2dG5kdXFoZWt2dXJldGZidnd1ZXRsamFzbmdha2R6b2p1d3J3amt2aGZ4c2x6Zm1wdWxzd3RwemptbnN6dXNnaHRtcXF4eXNzZHRsd3BycXRzbnZlb29wbG5lem1uamhlbGdxYnhybnpoanFpY3BucXl6dnZuaml5aXl2cXdicWZrYmdmand5cmJjdW56Z3pjd2FyeGVmdnVmbXBsdW1oaGV4eGFwbGV5anllanF5bmZzcnFvaXluY3pyaXFpbHdoemJ0eWp5b3V4cW1wdW5rbmV4c3d4a2l0eGd5a2pnY2lqcWt0eGxxb2VuY25ieGl4c3dxbmttZ3JodmxrcWR6cmlubm5zbWJwdGtscmdzdmxnemxmYmpvd2luanNsdWlhd3ZscXpzaHdlcXViemlzaGNzeGFiZGVteGRxbmFxenh6ZW13cHJ1YW11bG1kcHlvcXhtb2N0anZzYXd3bXF3anJycmRsdmxjZnFvdmhrY3d1YWJiemR4ZWNkbXhiZW11ZHJ0aGdhZ3hrYnlmZ2hqeHBuaHpocWdmemV5dmZvdHRpcnd5b213bW5nZ2dwcWtlZmRzeWhjamJ6c2hicG9lZ2NvaWFjZ3plemRpdHZ5empkbWtxYXlhaXZldXZweXVsaWZ5ZXlvbGlvcnRubG5ndXR0a3h6amd1dGxsaXN6aGh3cGZqcGdhYmVpcW51ZGNza2J0YXpyY3JvcWRscnZwY3N5emZoa2FpdmFucnp5aXZjc2tpd3l5bnN5cHVscWVlcmpvYm5tbWtwdW9wY29ieHF2ZmxmbHdqbmpkdXNnaG5kdnBmeXRmaHdubWhzcGFvZGFzbWV5a2FieW5rY3ZkbnBxbmNkaG1oYWppcGxudWh0dGlyd3luYWtneWxvc21wanJ1aHhkeHZ2YXdxb2pqdWFvZWplcXFseHNwdGZ6ZXdwdnRibnliamhneG5icmx0c3h2cmVwbm5xbnFmc290dXpidnNzYmFld3N5Z2h6emZqcHBtcGllYXVwcGd5bHdmeGFvdHZrdXN0eGZlcWJtaXBvbWRoYnBwbmZyd3RtZXJkZmZ6anN3ZmtuY3R4bHdwc3B4Z2FkeGNwb2ppYmVmb2tkd215ZGhob3ltZ2dxcnV0anpyYmdmYXZkaWJubGJsb2NkcnBhcmlyZGVhcXdxaHBsb3hqZ2Jyamx3bnVwdmtjZnFmdXVhYnFhZXFpdHB1Y3l1ZmRmenB1cGh4andramlubG51bnZ2cG92aHhkdXRoaXVvcXpvcmJkeXV5YWF6YWJpdmR5bWVuZW1ycXdxdGJlZHJicGJmb3FhY3F6bnVia2dma2l5aWZ5dm91dmtteW5oc3Z6bXJ3bmxpa2Vkc25ta29kbHBkcnhjZGNzZXhkcWxpbmhidnN2bWZqam55YndidWR4bHhueWt6b29jYXFjcXdncGZveWRzbGpxZ3hheGZob2Fmanppc3plb25sZHBzcWxmbW15cGV6bmJlZ29vaXh2Y2p6cGlrb2Z4eGp1dXJpeGd5emN3eGtibXBsa2dzaHpzZW9odG5zYnZnZXZvdXp3Z2draXJzbXVrYWJ2bmVxZGV6c3psZmVheGZxZ2djaG5kanBucmltbHJ5bGRnamhnc2Nid3ZncmxiZnZicmRna2RibWt4b3p4dHJscWdldXhlcm1uZXVmcWV1c2t4YW13Y2N0amJnd3ZxeGN1bXZnbGxndWNqZmJzcmdwamhlZHhsZHBkYW1ibHlpeHhsbXFqenV1eXJ0bW91Y3FjZXZmd2VvZmpwZ2hxdmR4Z3F0dXVyamJtb2lxdm9oYXBwZGFndHp2emhsaG13dmNjdGlqZ214aXdidWl5aWp2Y3Z6eWdzdnJjYmNwbXRnY2hwcHlnaG1tYmhsenVrZ25saG90anRsamFscWdyZ2l6c3J0cmFyZ2lybGF0YXNzdGpob2JhaWhscGNqcHNxbWVramd6anlqeG13amJydmVmZ2t0ZXdrdmphZnZjem9paWx4ZmxyZm1kd3B2Z2NjZnB4dmFmb3R1ZWpkd2NmaXFjZWFsY2JlemFpYXB5Z2RmdW9meW9keG52ZGphY3R0aG1oeWljdnBhY295ZnFnZG1xd25nemFmdnFta3B3dWxjZndtaHZwbWt4bmNucmp3b3Z4aHVkd2V6ZHd1Y2hwZWpnYnZubm15bWpsdnViY2hhbnZnbXZyeWVzZmd6b3Jmc3hianVkdWRrd2ZpdmxmZ3Rsa2JuanN5Y3N4amZ5ZXd5YWd4Z3h6ZmVyaGZraXp3anVkYmlpaGtwemxpemJ3Y2J2bmhvcmN5b2drdG1pY2FibnpycXpycWJsamt2d3ZraW91cXZteWl2bGNsZ3F6a3hxY2V0YmF5eWx3bHlxdmxza2N5eHpsY3VycndhdnFxcGlldXJwdGZ1aHFvbXBia3huaG5rbmNsbGR2aGthYWhhbW1vdHhucnRkenFrZmVuZnNpZ2hqaHJ5cXBhdHNxanlnZHRtcGJ1ZXJjaXR0ZG5tYmxqbHZwZHdvcmFzaGhnb3NmY2Vra3lka3Jia3p4Y3J1c3FtYnZ5bHl5cXpucWFzcGhiZndqenlldm9zZnZjd2F6YWxheWNiaHlueGhrcnd1bWhnYmNlaW5wbm5weHpubHd1dHBha29ib2t3c3F0dGJtbW9mZHhlc3J4ZHZnbW1kc3NhcnFoc2RnbXJocmFnYWVjbnR3dWF0cXJxd2xuamtscGtqcXBicXdrbnZhZmpibWZ1c25oY3R2ZXZxZnBjcmFvbWJ1eWx2Z3dzc3ltc2dncW9sdmtpY3FkY3RhZGN5bWN2aXRxZ3lxYXJhd253cHh6bHNyeHpxcGNxeXVvZmZmcXh0bHRjbGthd3ViemZzaGZmdW9rdWp5Z2ttc3h0eGJiYnNncWRueXZ0aWdqaWFqc25xb3N6cnRtd2hhcW5xd2lvYWxidmVzaHZtZnBicGJoZ21jbHNyZnJ1c2d3cHNhcm9tcGlweXF2em9ia21xcGJ5aWtuZXlmZXRlY25ob25sbm1kaGx0bHVlbmZpcW5qb3lwZHRxZ2ZqYXJlbHdnZGhtbHFqdnBncmFtc2plcXhqbmhqZnVvbXhieWR3am1teGlzZ2d1aWxyeHd1b2NkdWRmYWhlYXRkaGJvYnRjdHNwampzbmxreHRpZnN0d21zeGV6aXlja2tzbW9rb3Rqcmljem10bW15dmVleW90ZWV1bnJycHZmeWpiZmJwanFsYW1wbmZkZG5ta3l4emZpb3FiZ3p0eW1sYWhmbW1mdXhpYnpjY3pva2x3cm9haWFhbXFqaHR4cnhtZXlremp1aHB2aW9peXlrZXNjY2hmc2NnaXlrcHVscmVsam5ieHVraHp4cnd5aGpzYWl5eWx1cmVraW5nc254eHV0b2tjdWtud3l3YWt2dmRqbGVjc214d3JpYmt2aXFrdmRweW1saXFzZWFrZG9td25jaXphanJla3N2dHpta2NwdXBwZHhzcGZleW9ud2dod2lmemlqb3J6cHF5aW96eXJ3enJvZ2F2YXh4enlyZG11bXRyc3RxYmtrc2ptenJjd3dlc3dwZ2xxZGdkYmZsYXF3eWphYmJ0YWlja29sbXpxcmZjam93YWRtaW1haGticW96c3pwbHd1cHduZGd5Y2ZudGl6ZmNha29xa2lyY2FybW1qZ2VtbnhpdmllY3N0Z3NycWh2Y3NvZ2ZuZm5mYmR6ZWFqYWVham1idm16eXJlYmtrZnNxZHZrZXd6b2VxcGpmeHl6eGVzbHRnZmV4aHVpam5kdWljaXdnanBsbm11Z2Rwa3p6cGZtZGtwcGhpZWJya3Flc3JiZGVyZHpubnV4enl0YXNieHlybnRybXhjcWJ0bHlxeGllaGp3d2lxa2pieHlucmt1dHdmYnV0bmh2eHpxdWNhZmFwdXBicGR3aHJicG5yYnF3dHZ6a3NmZWljdWR2bWdnZWx6b3B5ZnlwZ2RnZXNxdWtqY2ltZWpmcmFsbmR0cGRjamF0bWl2aHN4bWVxdnBkZmp5ZG9lb2dxbWVneXRlZHBhYWN6c3Zxdmx1eWRxYmxjZWVneWd0Z3JhaXRrbHh2d2l2ZXJvbW9pcnF1YmlodmVwdXBlZWxhc2xtYXhzenNiZXJlbmNiam9sZ3d6bmZ5ZXV1d2pzZndqenliZWtzbXVsend0aWZydXljZmJxYmNneHZjbHV6ZGJjbWhtdWFya21heWpndnN2eXF6cHRkYmZqamtjdWVhY2Jucmp2YXJibndwbXNjd2h5dWJ1dWFlYW14d3hvbHhpaHpibHp1bmRza25heWR4dmxqemdkbGlsbHFzeXl6b21kbm5nb3JlZW95b3hjb3l4dnZmZWd5em5vcGZ4ZXpvbnF2Z2NlaG5pbXlsdHlpeGFiY3dzeGplcmNkc2VyanF2enZzZXJ4c2Z3bmJjdm9ibHB1dnVxYnVwcmFwZ3lpd3hubHpydmVwdnl2aXN1c3ZvbHlhanB3ZG91eGV3dHh2YXVjYXd5dWVpam9hemRwcGxvcWJqYXJwdGVpdHJqdnd4Y2ZtYmRhdmVjeWdpZnJrd2JkeGtwemZ1Z25ibHdiZGhrYWJ1bnBxZnlkdW1ha3dqaHdxdWhlY2VvYWRvbXJ1dW1ianhhY2twbmJ4b2djamtiYXlwa2pjdXlnZndhYWt1YmtucnZvcmthcWVocmRqeHd3eW9uZHJhc25qdWF6ZW9qZHFmdW9hcWZiYXBzZ2Fhdnl4aW1kYXBjc3Fsbmx3cmp0bHdnaXNyZGlraWp6ZXpsYmp1a2N5cWxpd2d6eGZtbmlwZ3R6bnltZmxrdGR5eWRyeWFoYnVkY2Nybml3Znd1eWJ6YXF3ZGJicG5rcnRwcWtwcWJyZXJvZHNpb2ZyaGZqbmJkb2NnamlpamNldm1ubW1rc29zZnNibXhoa3FxbnB0cmltZm1qdHlrbGpzdXl3eG5saGJ5cnRkb3pvc2licG5ibGt0Z3FsdXBja3VidWV2bWZ0b3B6eGJ1eWlhanVwZGZzcHZ0bnFybXZ5anJqY255Y3FhcXp6ZnFic21oeHBsaXJxc3Z2Y2d6YnViaGFzenJlenZwZ3VmYW14d3JwcHJnbXNpamJlaXBncHJvbnlmdGJzcHRqaXF3b3V4cGh6dWxsYWRhdXVjanlxbXZmdGF1aGJ6Zmdnbmtya2F4dXNraG9uc2drb3BpbGRra25jYXZ6ZHNjbXRra2RrdmJyc2Zibm5xZWxpZWFjcGp0Z3R3a2lub25yYnBqdHp2endub3p3eG5oZXdhd2hncnF4Zm5oaWx4YWdjaWhqZ3pucXh1eGl5Y3VqcXNkdm9nb3ZhY3BvZXVyd2xlc3NobHhua2Z1bHh5dnlpY3Ruaml4eG5pZ21iY3hrYWJqeHJxdmNzbG1naGxlbGJ6eHJkdWdxZXdzZnpienh2cGFtbG1qanlxcXRvamN2dnJlaG5xdm5lZHpoeGlpdndhbWp3aGx0Z2h5cGJ2aXNxaXR4enJybGVjbXhpY3hxbHd0d2xocHR3cnNldGl0bXJ0a2hva256dmdjaHV4ZXhpanp2d2FqaWlvbWFwYXNxamdmeHJkdmxvemp4d2hzaWFpZ2V1ZXNxc2NjcnlreHB5d3Zia2xtdWZpdGZhbm9kenhveGthcGx0d2ZrbWZsbXJhaWZhaGhrbnhjaHVzdWhscWd3YnZ5cXByaWFxbGxnZ3l5ZmNubHFlbXdwdWt0d3d1bmppenNzaWdmdmRxZHVzbWZvYmRvZ211dmRja3ViY2xmcHRlanhlcWt2b2Zhdm51eHBndXBqem1zbG1waG5hZmxsdGNrcmJ3bHZpZGJ5a3hiaW9sZmZuZXlkdnpoaGV3c2JucndseGd3d2plbWVna3d3d3FjZXhtaHhvZ3p4a3lmcWVqdXluemlrd2JxbHBmdXdnaHZmdGZqbnJ4YnBrZGNzamp6aGx6aHNocnltbm9oYXpqcndsanlka2JxeXlhcHZlY2xnand3bHFtbXZicnZibW5ramJqb3NjZWx6c2ppbWl5eXNzc3ZyeHV0Zmt4b3J5dHh4ZmhzaW53c2R2d2dsd3pkempjdWRjcXdtaWZqb25tcnRhcGR5cXFvYnhsZW5sYW5zc3RtbHVoaXZxam5lY2JhYmdpeGd5amlmZXp6bWtjanppam1tZmxkZXFkb3puZWxzeG1zcHJvaHprbmpvc2l1bG5ocmF1ZWh0dnlxYmNmcHN6c3N6aWpuY2R4bnJpdXN0eHppcWNyb25ubHpzdnJnc3NxdmV4Y29sYWlzYnFla3VqdGt5anhibW55eHdkZmhoY2Nlb2t1c3Rqb3NjZmJ5anJrY3FjdHNjc2Z0cm1venF5bWRid29ld29scGdvY3hkeWlmaW5wbHVrZW5kenN4YmdxbWhreG90YmtmZWpia2ZqY2FreWp6bnBvbG95cGp4b25rYXNubXFhdGN5enVwc3BlcHFlbW50cWFvb2ltaHhia25pcG12cWFkZmZucWNwZG1td29oZWFwZ2dja2JyYWl5bnBzem9maGJ1cGNzenJwaXptc3N4emFpeHFxcHlzbnludnlwdWlhYWltcHBzZGltcXNqZGtzcm5jcHhmY2p0cWZ5d3lzeWt4Ynh2dG1mbXRqdmdxZmp1aGpoY2Zic291emx6Y21paW56YXR1aW16dXVweWR6ZHR5cWR1ZWFyc3hldmRlY2hqc3ZpdHJodXpja2JtcXBlY3lpem16YWV5dmV5aGR0ZXJ5eHlrdHdleGZqcG5tc3RhcGZseW9neGd3ZGRhcGRnYmxkZXBidGVkcWZ1YmV6dWRwaHZidHRpZGRjZWpoYXVoanpnemhhaGllcGVoYnN4cHRueWd4bnl5a2J6bmFxZm9tZnF2dGp1ZHVyc2tieWpsbnRid3BudHRieWVlZ3V2Y2NjaW5yeXNibGhycHN5Y21ucGptbGJsY21rb3ZidnpxbWptb2V4ZmhjbXJyYWdncWx0ZHZlZW5laGxobG11ZHRnbWlucmF2ZnN3ZWlxbGdxd2lsaWt6YnZldHNuY2pxaGVwbW54dW5raWp1bnBxZHl4YmZ6bG9paHJrbGdyYnF5eGVlcm52cWN6d2Z1bXRvbHFieHB4ZG1xdHBvc3licXdidmpwenh3aGRwanp0cnVmcXNzcndjd3F0ZW5hdXJzYmNiZXN2bGJiZmNld3l2aHJqYmh6eXpndmlyaHdodXlnenBianhnZWhubW1wdnh1bWZsd294ZHJveGx3eW5sYmJkbWZybWRtbmxwdGJleXJvemFjdWJkbXR2ZXhpcG1qb3JjaXh4aXVwY2ZrYWhqZmhybWhqa3d5Y211Y2d1cXllcXZidGp3Z3ZoYW9kaHVmY21mZHhoa3V6dGNqemd1cmp1dGFlY21qandobGxqaXdxYWZ6YWl5cmtrcXR6ZHNmZWZwb2Zjb3p1dGlpdWhlY25wYnhkbXpha3ZxdXJjemJpaHVsYmpscmFmdnpqaGNpc2N3cWFzYW1kZGRieW1pa3ZvZGxsbmN2am16c3hrcmprd3l5dWlobWxpenZ5YWthdGh5Z2N3aXNuZWljaGZnc252ZWhvdXl5c2hpY3dvbmN2Z2ZxaGRscGthZGRxaGl5b3RqcHBqZnVxenFwdmljY29yYXprb3psd3Bpc2ppemdsZmV1dW13cHJpcW5hdmtsYXR2YmV5cHp2aHpqenNwYWZoZG1udnFkdXdmdW1jaGZ1anR1dWl6aW9pdXd1d2Nvb21heXBkb2N0cG9tc21tYmpoanlnZWhseHNmbmx2eGVjb2JlaG5rbXdnenZuZG5ocmJiaWJ2cGhncXJvZnpxc2VyZnBvZHBldnRibGp6aGdkY25xZnZhaGt2Z2ZncXpsb2dheHJseHlja2xpaG5pd3V1aHZzZHJhZ3hkZnhoZmVhbWVvZHVya2t6ZG13bWFlY25mYm9nbGpzYWdjc2J6Z2dia3pzcm9kanVoY2JtbHpwZ3l0dHdvcXBweHhseWl4cWZxd3p3bmF6ZHZudHBtcWpjd3phanB3cGJ6a2pmemtremFzam9wdWdsZnh5Y2VtZXhsZ3R3enRuc2d6ZmZrZmh4YmFzZ3NzdW1xZGJhb3d2aGdsbmtreXp1aWZ2b2NkeGxub2Fxb2Rrc3R6ZHRvcGFucm1scHpocXFuZ2hrYWl2aWtkYWF3YWV3eWhoY21rdnJwdnVvZnBoY3NqaHlncHF0enFjcnRza3lna3ltcnFvZXNnenpwZmNvcHpramNwZmt1cmhpa2d3d21xcXpweHpoZ2phdmxzdnJ1ZWp0cmZrdmp3b2Z5Y3FzbHlzdmd0bGRic2V0cHZ6emVkc3hmcGF4Z3V2ZXpuZm9ydW5qcWpjbm5yY3BiYmVodHhuaWdmbGtxcWxza3BkbXlhY21ncmdlaGlsbHV6anlqbWhtaWF6eXFvdHlrZmtud2Fja3BndnhuYm9zYWNnb25hZXZuZmV6andzdGhmYXRhY2xhYW50ZGVqenBienp2dmh1b3p3bnhubXBoY29ob2xrZ2xwaWJmbXBidWVsZWZiZm5td3hxcGxyendheHZteWN1eHhhbnRiZHhheWR3cndmY255ZGdpa2x4bHJsd2l4b2FhZGZ6eW1nb3N3a2lrb3N6YWx0eGRvbm56dmdydHpqdHBobWh1dHVsa3lkbWdyZWZvaHNudGx2ZXZ6dXFkdGh5anV5dGh1bXNjYWtnaGhrbmJ5dWR1eHN6em51cm5wampjam90Y3hvcWp3b3NramRneGlnaHVpaWR6d25ydnNmbnl0dm1rZmhyeXBqdXB2bm1wcHlzY2x4eGlzZ3Vkbmdwc3lpaWx3Ym5qcHd2aW5oY3NwaHVtdGxidXppcWtmb3NoeW9kbGt1Y3lweGhjdXhzbXZodmtlc2VtY2ZmYnFpeHd0dnRkaWdraXZmb2txdnVsdmpsdmhzamhpeHZlZXJxaGlsZXphYmdjYXJjeWxqc2tkc3BubHJ4eGRud251cmNvbnJiZnB0a2JqeWJjZ291ZWtmb2FicHZoYW1ncnd0a2FhaHV3eGV0bnlmdXR4dWhheHpjY2hpbHZtYnNlcW9qc2F2Ymx2YXlla2JkZ2NzcHVoYmZ2bmlvZWRrd3VsaWtkcXlhd3dyYnhnem94aW9kbWF5cmhmanJ5eW1nbXBib3Zoc2hqenNsaGphcGlseHFsdGJhdnNsbGthY2V0dW12c3ZzdHphdnV2anZ3cW5oYmFkcHp0a3p0bndkZWJxa2F6cGRpcW5kd2p0cGlsdW5yanFtc2x6YnZoeGxkeWRqaW53aWVjZHNseG14ZmJydmV1dmdsYm9nb3RocWZld3p4Z2FnaGdpenBpYWJ6enhsdmRyd29jZHFwYmd4Y21paXdlcWFoZHNlbnppZ216bmZsY2thdHhvb3pmdGZzdmhiZnl0bmxkc2dsYWxncWJ0dm5wanVidWFhenVkbmpqbmNqdmV3cHFzeW90aHlmcHB3aXhmZGFlcnpodmRxaXNpa3lqbHNqeGd3amZsc3N0ZmFjdHluY2xmb214Y3locmd6a2R4bXJpY29kaHNkZHRvZWFnanJ2bnRwbndnZnZ6YWtveXFienVrdW5kcWFrZ2xoaGVkYWhseGFoYWR5cmNtbnNhaHVieGJjdGtreWRlbHRvbGxybmN0empmbW9zaXFpbGp2dWRwZ3lzdnhvYWZhc21qaGhoa2J1Y3RseWlycHJwbWlkY25idXRtaXZvd2dpanhzamRwYWNmYWVrdW16dWh3c3dndXlremp6ZXpoZmRld2h1dm9mY2t6dnJheWp0bW5icG5hbHl2anh3ZmtwbGliYWxmaHBzbWtseGlybHNzaWp2bW5nam54dHFvdGJ5Z3lndHR3b25nZ2p2eXpydGN5dXNlc2dlYXZ4cmttYmdmZnlkdHVqZ3l1dXJ2dmFjYnh3aHZ2YXZ4dXNzaHZobnJlZWlhcW54ZmtlY2ZkaGZvbGxobWZqaGRyaXNvcWF6bHBka3lvYXpkdmdoZG5ub3lqYmRqbWpveGtrYWp6ZWNtcW91bm1xdWFyZmZ6d256dHdza3Zta2h4Ynhzb3NxeGxpcGVvZWZucmV4ZGxmc3Blc2dsbWlja3loc3RvdnNjdGNianZweHdodXhpb2l2a2d4YmJic211Z2JpZnNzaWNub3hiZ2NsYWFuZWh0bG1yd3VzaHBwZmxqcG55Z3VkdmNxb3JmcmFsZGh5anFiYnZmeXJna215am5hcXBicGh1c2JjcmVzc3B4ZmNwcXhud3lhZWpjbGRncWpudWx6a2ZpbWhkdmFteXhxdnJ6bmt2a2FzbGthcXFyZWpqb2Z5bWZ5dGF3cWx5Z3d2dWVsaHB2ZWN3a3psendmdGZqbWR2aHR6b2l3cXJ6bnJjem9lYXlsdWZ3ZGtkd2pwaHVpcWdycnh0cmNoYnd4b2tkeXZvZ2l4dWVqbWdvYmtla2xjbGh6Ynl2a3NzYXNqYWZyZnp0Z3V5ZWJ1c3d2eGh3c2Znamd3ZXN2aHhpaXJmbndxbG90c25zYmdrdmxienp6aGx0d2h4b3NsaWZ5ZXpiY2F3dnJtY2Vwa2Nyd3dwbWdjYXhydHBlbnFzYXRjdHl0cnFmYm1pcnNteG14ZHJ0d3F0Y3F4cG9hZ2RtemNkZ3BjYmNjdWluaGlwdXlkamp5eWVvY25qbWFpdmJzdmVkc3R6c2dmcWd1enJubW9zY3NjeGNiZnBhZWtscW9xcGZ2Z3hjcHVta3VoaGF1dGlhY291dnlhYWd3c3ZxbHBma2Rnenlld3hkdnVhaG1ucmhmcm1xa2Z5enNscGhkcmZrd3BjcXl5YWl6eGZ2enVhZ3BmcGlkZG10dXZ2YmxqcXhpdG14amNuYnh3Y2JkdHZ0bGJpdHpuYW9wc3F4eXZqbWFjdXVrYmtndnRha3FwdHhobmVrYnJvc3dudGhvZ3luampleHdlZXZlYnVqZ3l0emx6emV1b3JkZm9va2ZwaHdwaHNldHprdmdrc2dydm1tY3JleWNsZmV3ZGp0b3BwbWdibWhwd2JxbmNrZW5oeHpuaGd0ZWFxenpva2ttdW11c2xqZG5lcmZla3pncnBkbXdjbHJqaWZtbGtsc2lqY2NpZnp4d256Ymlnd3R3cHZraGh0dmJxdnh4c3RmaW9tcnpsZHRjZHBwZGF2cXNkY3R6ZXV1Z3loYWZscmR3bWdudml1bHNld2Nocmt6emZubGF4c2xueWNuZWhuYWZtcHhpcG9xeG9pdWRienZ4Z3p4cmd6YmRqdWxtZGlrcWNtaHhudWNwdm93em96amdvdGpramZsd250ZXJheGdmZ21neWdhaXl6bHlicGNhb3hxd2JkcGhocndnb2ttaWlja2Z0b2tmdG91c2t5ZnJjd2Jwdmtlb2ZsZnZ6dnN2bWdka2t2d2x4a2F3ZnBjYWZhb2F5bG1wdXRhempybHl1Y3R0dmxpbmtrZ3BvbXdzeHV5eHhnc215Y2F0aW12c3lxYXVzdmpxY3hiaHJhanhtZHps////");
        }

        public byte[] utf8() {
            return utf8;
        }
    }

    private SdkHttpFullResponse fullResponse(TestItemUnmarshalling item) {
        AbortableInputStream abortableInputStream = AbortableInputStream.create(new ByteArrayInputStream(item.utf8()));
        return SdkHttpFullResponse.builder()
                                  .statusCode(200)
                                  .content(abortableInputStream)
                                  .build();
    }

    private static byte[] toUtf8ByteArray(Map<String, AttributeValue> item) {
        SdkHttpFullRequest marshalled = putItemRequestMarshaller().marshall(PutItemRequest.builder().item(item).build());
        InputStream content = marshalled.contentStreamProvider().get().newStream();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[8192];
        int read;
        try {
            while ((read = content.read(buff)) != -1) {
                baos.write(buff, 0, read);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return baos.toByteArray();
    }

    private static byte[] toUtf8ByteArray2(Map<String, AttributeValue> item) {
        SdkHttpFullRequest marshalled = putItemRequestMarshaller().marshall(PutItemRequest.builder().item(item).build());
        InputStream content = marshalled.contentStreamProvider().get().newStream();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[8192];
        int read;
        try {
            String res = IoUtils.toUtf8String(content);
            if (res.length() > 0) {
                throw new RuntimeException(res);
            }
            //System.out.printf("==================== out:\n%s\n");
            while ((read = content.read(buff)) != -1) {
                baos.write(buff, 0, read);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return baos.toByteArray();
    }

    private static PutItemRequestMarshaller putItemRequestMarshaller() {
        return PUT_ITEM_REQUEST_MARSHALLER;
    }

    private static AwsJsonProtocolFactory getJsonProtocolFactory() {
        return JSON_PROTOCOL_FACTORY;
    }

    private static <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder
            .clientConfiguration(SdkClientConfiguration
                                     .builder()
                                     .option(SdkClientOption.ENDPOINT, URI.create("https://localhost"))
                                     .option(SdkClientJsonProtocolAdvancedOption.ENABLE_FAST_UNMARSHALLER, true)
                                     .build())
            .defaultServiceExceptionSupplier(DynamoDbException::builder)
            .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR)
            .protocolVersion("1.0")
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("RequestLimitExceeded")
                                 .exceptionBuilderSupplier(RequestLimitExceededException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("GlobalTableAlreadyExistsException")
                                 .exceptionBuilderSupplier(GlobalTableAlreadyExistsException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ImportConflictException")
                                 .exceptionBuilderSupplier(ImportConflictException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ConditionalCheckFailedException")
                                 .exceptionBuilderSupplier(ConditionalCheckFailedException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("PolicyNotFoundException")
                                 .exceptionBuilderSupplier(PolicyNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("LimitExceededException")
                                 .exceptionBuilderSupplier(LimitExceededException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("GlobalTableNotFoundException")
                                 .exceptionBuilderSupplier(GlobalTableNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ItemCollectionSizeLimitExceededException")
                                 .exceptionBuilderSupplier(ItemCollectionSizeLimitExceededException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ReplicaNotFoundException")
                                 .exceptionBuilderSupplier(ReplicaNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("BackupInUseException")
                                 .exceptionBuilderSupplier(BackupInUseException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ResourceNotFoundException")
                                 .exceptionBuilderSupplier(ResourceNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ContinuousBackupsUnavailableException")
                                 .exceptionBuilderSupplier(ContinuousBackupsUnavailableException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("IdempotentParameterMismatchException")
                                 .exceptionBuilderSupplier(IdempotentParameterMismatchException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ExportNotFoundException")
                                 .exceptionBuilderSupplier(ExportNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("TransactionInProgressException")
                                 .exceptionBuilderSupplier(TransactionInProgressException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("TableInUseException")
                                 .exceptionBuilderSupplier(TableInUseException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ProvisionedThroughputExceededException")
                                 .exceptionBuilderSupplier(ProvisionedThroughputExceededException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("PointInTimeRecoveryUnavailableException")
                                 .exceptionBuilderSupplier(PointInTimeRecoveryUnavailableException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ResourceInUseException")
                                 .exceptionBuilderSupplier(ResourceInUseException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("TableAlreadyExistsException")
                                 .exceptionBuilderSupplier(TableAlreadyExistsException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ExportConflictException")
                                 .exceptionBuilderSupplier(ExportConflictException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("TransactionConflictException")
                                 .exceptionBuilderSupplier(TransactionConflictException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InvalidRestoreTimeException")
                                 .exceptionBuilderSupplier(InvalidRestoreTimeException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ReplicaAlreadyExistsException")
                                 .exceptionBuilderSupplier(ReplicaAlreadyExistsException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("BackupNotFoundException")
                                 .exceptionBuilderSupplier(BackupNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("IndexNotFoundException")
                                 .exceptionBuilderSupplier(IndexNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("TableNotFoundException")
                                 .exceptionBuilderSupplier(TableNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("DuplicateItemException")
                                 .exceptionBuilderSupplier(DuplicateItemException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("ImportNotFoundException")
                                 .exceptionBuilderSupplier(ImportNotFoundException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("TransactionCanceledException")
                                 .exceptionBuilderSupplier(TransactionCanceledException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InvalidExportTimeException")
                                 .exceptionBuilderSupplier(InvalidExportTimeException::builder).build())
            .registerModeledException(
                ExceptionMetadata.builder().errorCode("InternalServerError")
                                 .exceptionBuilderSupplier(InternalServerErrorException::builder).build());
    }

}
