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

package software.amazon.awssdk.v2migration.internal.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

@SdkInternalApi
public final class NamingConversionUtils {

    private static final String V1_PACKAGE_PREFIX = "com.amazonaws.services";
    private static final String V2_PACKAGE_PREFIX = "software.amazon.awssdk.services";
    private static final Map<String, String> PACKAGE_MAPPING = new HashMap<>();
    private static final Map<String, String> CLIENT_MAPPING = new HashMap<>();
    private static final Map<String, String> S3_POJO_MAPPING = new HashMap<>();
    private static final Set<String> S3_MODEL_SUBMODULES = new HashSet<>();

    static {
        PACKAGE_MAPPING.put("appregistry", "servicecatalogappregistry");
        PACKAGE_MAPPING.put("augmentedairuntime", "sagemakera2iruntime");
        PACKAGE_MAPPING.put("certificatemanager", "acm");
        PACKAGE_MAPPING.put("cloudcontrolapi", "cloudcontrol");
        PACKAGE_MAPPING.put("cloudsearchv2", "cloudsearch");
        PACKAGE_MAPPING.put("cloudwatchevidently", "evidently");
        PACKAGE_MAPPING.put("logs", "cloudwatchlogs");
        PACKAGE_MAPPING.put("cloudwatchrum", "rum");
        PACKAGE_MAPPING.put("cognitoidp", "cognitoidentityprovider");
        PACKAGE_MAPPING.put("connectcampaign", "connectcampaigns");
        PACKAGE_MAPPING.put("connectwisdom", "wisdom");
        PACKAGE_MAPPING.put("databasemigrationservice", "databasemigration");
        PACKAGE_MAPPING.put("dynamodbv2", "dynamodb");
        PACKAGE_MAPPING.put("elasticfilesystem", "efs");
        PACKAGE_MAPPING.put("elasticmapreduce", "emr");
        PACKAGE_MAPPING.put("gluedatabrew", "databrew");
        PACKAGE_MAPPING.put("iamrolesanywhere", "rolesanywhere");
        PACKAGE_MAPPING.put("identitymanagement", "iam");
        PACKAGE_MAPPING.put("iotdata", "iotdataplane");
        PACKAGE_MAPPING.put("kinesisfirehose", "firehose");
        PACKAGE_MAPPING.put("kinesisvideosignalingchannels", "kinesisvideosignaling");
        PACKAGE_MAPPING.put("lookoutforvision", "lookoutvision");
        PACKAGE_MAPPING.put("mainframemodernization", "m2");
        PACKAGE_MAPPING.put("managedgrafana", "grafana");
        PACKAGE_MAPPING.put("migrationhubstrategyrecommendations", "migrationhubstrategy");
        PACKAGE_MAPPING.put("nimblestudio", "nimble");
        PACKAGE_MAPPING.put("private5g", "privatenetworks");
        PACKAGE_MAPPING.put("prometheus", "amp");
        PACKAGE_MAPPING.put("recyclebin", "rbin");
        PACKAGE_MAPPING.put("redshiftdataapi", "redshiftdata");
        PACKAGE_MAPPING.put("sagemakeredgemanager", "sagemakeredge");
        PACKAGE_MAPPING.put("securitytoken", "sts");
        PACKAGE_MAPPING.put("servermigration", "sms");
        PACKAGE_MAPPING.put("simpleemail", "ses");
        PACKAGE_MAPPING.put("simpleemailv2", "sesv2");
        PACKAGE_MAPPING.put("simplesystemsmanagement", "ssm");
        PACKAGE_MAPPING.put("simpleworkflow", "swf");
        PACKAGE_MAPPING.put("stepfunctions", "sfn");
    }

    static {
        CLIENT_MAPPING.put("ACMPCA", "AcmPca");
        CLIENT_MAPPING.put("AppRegistry", "ServiceCatalogAppRegistry");
        CLIENT_MAPPING.put("AugmentedAIRuntime", "SageMakerA2IRuntime");
        CLIENT_MAPPING.put("BillingConductor", "Billingconductor");
        CLIENT_MAPPING.put("CertificateManager", "Acm");
        CLIENT_MAPPING.put("CloudControlApi", "CloudControl");
        CLIENT_MAPPING.put("CloudHSMV2", "CloudHsmV2");
        CLIENT_MAPPING.put("CloudWatchEvidently", "Evidently");
        CLIENT_MAPPING.put("CloudWatchRUM", "Rum");
        CLIENT_MAPPING.put("CodeArtifact", "Codeartifact");
        CLIENT_MAPPING.put("CodeStarNotifications", "CodestarNotifications");
        CLIENT_MAPPING.put("CodeStarconnections", "CodeStarConnections");
        CLIENT_MAPPING.put("ConnectCampaign", "ConnectCampaigns");
        CLIENT_MAPPING.put("ConnectWisdom", "Wisdom");
        CLIENT_MAPPING.put("DatabaseMigrationService", "DatabaseMigration");
        CLIENT_MAPPING.put("DirectoryService", "Directory");
        CLIENT_MAPPING.put("DynamoDB", "DynamoDb");
        CLIENT_MAPPING.put("DynamoDBStreams", "DynamoDbStreams");
        CLIENT_MAPPING.put("ElasticFileSystem", "Efs");
        CLIENT_MAPPING.put("ElasticMapReduce", "Emr");
        CLIENT_MAPPING.put("FinSpaceData", "FinspaceData");
        CLIENT_MAPPING.put("ForecastQuery", "Forecastquery");
        CLIENT_MAPPING.put("GlueDataBrew", "DataBrew");
        CLIENT_MAPPING.put("IAMRolesAnywhere", "RolesAnywhere");
        CLIENT_MAPPING.put("IdentityManagement", "Iam");
        CLIENT_MAPPING.put("IdentityStore", "Identitystore");
        CLIENT_MAPPING.put("IoTDeviceAdvisor", "IotDeviceAdvisor");
        CLIENT_MAPPING.put("IoTEvents", "IotEvents");
        CLIENT_MAPPING.put("IoTEventsData", "IotEventsData");
        CLIENT_MAPPING.put("IoTJobsDataPlane", "IotJobsDataPlane");
        CLIENT_MAPPING.put("IoTWireless", "IotWireless");
        CLIENT_MAPPING.put("IotData", "IotDataPlane");
        CLIENT_MAPPING.put("KinesisFirehose", "Firehose");
        CLIENT_MAPPING.put("KinesisVideoSignalingChannels", "KinesisVideoSignaling");
        CLIENT_MAPPING.put("Logs", "CloudWatchLogs");
        CLIENT_MAPPING.put("LookoutforVision", "LookoutVision");
        CLIENT_MAPPING.put("MainframeModernization", "M2");
        CLIENT_MAPPING.put("ManagedGrafana", "Grafana");
        CLIENT_MAPPING.put("MigrationHubStrategyRecommendations", "MigrationHubStrategy");
        CLIENT_MAPPING.put("NimbleStudio", "Nimble");
        CLIENT_MAPPING.put("Private5G", "PrivateNetworks");
        CLIENT_MAPPING.put("RecycleBin", "Rbin");
        CLIENT_MAPPING.put("RedshiftDataAPI", "RedshiftData");
        CLIENT_MAPPING.put("ResilienceHub", "Resiliencehub");
        CLIENT_MAPPING.put("SSOOIDC", "SsoOidc");
        CLIENT_MAPPING.put("SagemakerEdgeManager", "SagemakerEdge");
        CLIENT_MAPPING.put("SavingsPlans", "Savingsplans");
        CLIENT_MAPPING.put("SecurityTokenService", "Sts");
        CLIENT_MAPPING.put("ServerMigration", "Sms");
        CLIENT_MAPPING.put("SimpleEmailService", "Ses");
        CLIENT_MAPPING.put("SimpleEmailServiceV2", "SesV2");
        CLIENT_MAPPING.put("SimpleSystemsManagement", "Ssm");
        CLIENT_MAPPING.put("SimpleWorkflow", "Swf");
        CLIENT_MAPPING.put("StepFunctions", "Sfn");
        CLIENT_MAPPING.put("WAF", "Waf");
        CLIENT_MAPPING.put("WAFRegional", "WafRegional");
        CLIENT_MAPPING.put("Workspaces", "WorkSpaces");
    }

    static {
        S3_POJO_MAPPING.put("GetBucketCrossOriginConfigurationRequest", "GetBucketCorsRequest");
        S3_POJO_MAPPING.put("DeleteBucketCrossOriginConfigurationRequest", "DeleteBucketCorsRequest");
        S3_POJO_MAPPING.put("GetBucketVersioningConfigurationRequest", "GetBucketVersioningRequest");
        S3_POJO_MAPPING.put("DeleteBucketLifecycleConfigurationRequest", "DeleteBucketLifecycleRequest");
        S3_POJO_MAPPING.put("DeleteBucketReplicationConfigurationRequest", "DeleteBucketReplicationRequest");
        S3_POJO_MAPPING.put("DeleteBucketTaggingConfigurationRequest", "DeleteBucketTaggingRequest");
        S3_POJO_MAPPING.put("DeleteBucketWebsiteConfigurationRequest", "DeleteBucketWebsiteRequest");
        S3_POJO_MAPPING.put("GetBucketLoggingConfigurationRequest", "GetBucketLoggingRequest");
        S3_POJO_MAPPING.put("GetBucketReplicationConfigurationRequest", "GetBucketReplicationRequest");
        S3_POJO_MAPPING.put("GetBucketTaggingConfigurationRequest", "GetBucketTaggingRequest");
        S3_POJO_MAPPING.put("GetBucketWebsiteConfigurationRequest", "GetBucketWebsiteRequest");
        S3_POJO_MAPPING.put("GetRequestPaymentConfigurationRequest", "GetBucketRequestPaymentRequest");
        S3_POJO_MAPPING.put("GetObjectMetadataRequest", "HeadObjectRequest");
        S3_POJO_MAPPING.put("InitiateMultipartUploadRequest", "CreateMultipartUploadRequest");
        S3_POJO_MAPPING.put("InitiateMultipartUploadResponse", "CreateMultipartUploadResponse");
        S3_POJO_MAPPING.put("ListVersionsRequest", "ListObjectVersionsRequest");
        S3_POJO_MAPPING.put("DeleteVersionRequest", "DeleteObjectRequest");
        S3_POJO_MAPPING.put("CopyPartRequest", "UploadPartCopyRequest");
        S3_POJO_MAPPING.put("CopyPartResponse", "UploadPartCopyResponse");

        S3_POJO_MAPPING.put("SetBucketCrossOriginConfigurationRequest", "PutBucketCorsRequest");
        S3_POJO_MAPPING.put("SetBucketReplicationConfigurationRequest", "PutBucketReplicationRequest");
        S3_POJO_MAPPING.put("SetBucketTaggingConfigurationRequest", "PutBucketTaggingRequest");
        S3_POJO_MAPPING.put("SetBucketWebsiteConfigurationRequest", "PutBucketWebsiteRequest");
        S3_POJO_MAPPING.put("SetRequestPaymentConfigurationRequest", "PutBucketRequestPaymentRequest");
        S3_POJO_MAPPING.put("SetBucketLoggingConfigurationRequest", "PutBucketLoggingRequest");
        S3_POJO_MAPPING.put("SetBucketVersioningConfigurationRequest", "PutBucketVersioningRequest");

        S3_POJO_MAPPING.put("CorsRule", "CORSRule");
        S3_POJO_MAPPING.put("LifecycleFilter", "LifecycleRuleFilter");
        S3_POJO_MAPPING.put("LifecycleAndOperator", "LifecycleRuleAndOperator");
        S3_POJO_MAPPING.put("ReplicationFilter", "ReplicationRuleFilter");
        S3_POJO_MAPPING.put("ReplicationAndOperator", "ReplicationRuleAndOperator");
        S3_POJO_MAPPING.put("PartETag", "CompletedPart");
        S3_POJO_MAPPING.put("ObjectTagging", "Tagging");
        S3_POJO_MAPPING.put("BucketPolicy", "GetBucketPolicyResponse");
        S3_POJO_MAPPING.put("CanonicalGrantee", "Grantee");
        S3_POJO_MAPPING.put("EmailAddressGrantee", "Grantee");
        S3_POJO_MAPPING.put("GetS3AccountOwnerRequest", "ListBucketsRequest");
        S3_POJO_MAPPING.put("MultipartUploadListing", "ListMultipartUploadsResponse");
        S3_POJO_MAPPING.put("PartListing", "ListPartsResponse");
        S3_POJO_MAPPING.put("PartSummary", "Part");
        S3_POJO_MAPPING.put("RedirectRule", "RedirectAllRequestsTo");
        S3_POJO_MAPPING.put("ReplicationDestinationConfig", "Destination");
        S3_POJO_MAPPING.put("RoutingRuleCondition", "Condition");
        S3_POJO_MAPPING.put("S3ObjectSummary", "S3Object");
        S3_POJO_MAPPING.put("ObjectListing", "ListObjectsResponse");
        S3_POJO_MAPPING.put("VersionListing", "ListObjectVersionsResponse");
        S3_POJO_MAPPING.put("S3VersionSummary", "ObjectVersion");
        S3_POJO_MAPPING.put("AmazonS3Exception", "S3Exception");
        S3_POJO_MAPPING.put("CloudFunctionConfiguration", "LambdaFunctionConfiguration");
        S3_POJO_MAPPING.put("LambdaConfiguration", "LambdaFunctionConfiguration");

        //Special Enum mapping
        S3_POJO_MAPPING.put("S3Event", "Event");

        // Maps 1 to many, e.g., used as input to Request POJOs and returned as Response POJOs
        // Transform to one type here, convert others in S3TypesToV2 with ChangeMethodInvocationReturnType
        S3_POJO_MAPPING.put("ObjectMetadata", "HeadObjectResponse");
        S3_POJO_MAPPING.put("AccessControlList", "AccessControlPolicy");
        S3_POJO_MAPPING.put("BucketAccelerateConfiguration", "AccelerateConfiguration");
        S3_POJO_MAPPING.put("BucketCrossOriginConfiguration", "CORSConfiguration");
        S3_POJO_MAPPING.put("BucketNotificationConfiguration", "NotificationConfiguration");
        S3_POJO_MAPPING.put("BucketReplicationConfiguration", "ReplicationConfiguration");
        S3_POJO_MAPPING.put("BucketTaggingConfiguration", "Tagging");
        S3_POJO_MAPPING.put("BucketWebsiteConfiguration", "WebsiteConfiguration");
        S3_POJO_MAPPING.put("BucketVersioningConfiguration", "VersioningConfiguration");
        S3_POJO_MAPPING.put("BucketLoggingConfiguration", "BucketLoggingStatus");
    }

    static {
        S3_MODEL_SUBMODULES.add("analytics");
        S3_MODEL_SUBMODULES.add("intelligenttiering");
        S3_MODEL_SUBMODULES.add("inventory");
        S3_MODEL_SUBMODULES.add("lifecycle");
        S3_MODEL_SUBMODULES.add("metrics");
        S3_MODEL_SUBMODULES.add("ownership");
        S3_MODEL_SUBMODULES.add("replication");
    }

    private NamingConversionUtils() {
    }

    public static String getV2Equivalent(String currentFqcn) {
        int lastIndexOfDot = currentFqcn.lastIndexOf('.');
        String v1ClassName = currentFqcn.substring(lastIndexOfDot + 1);
        String packagePrefix = currentFqcn.substring(0, lastIndexOfDot);

        if (isS3ModelClass(packagePrefix)) {
            packagePrefix = removeS3ModelSubmoduleName(packagePrefix);
        }
        String v2PackagePrefix = packagePrefix.replace(V1_PACKAGE_PREFIX, V2_PACKAGE_PREFIX);
        v2PackagePrefix = checkPackageServiceNameForV2Suffix(v2PackagePrefix);

        String v2ClassName = pascalCaseIfApplicable(v1ClassName);
        if (Stream.of("Abstract", "Amazon", "AWS").anyMatch(v1ClassName::startsWith)) {
            v2ClassName = getV2ClientOrExceptionEquivalent(v1ClassName);
        } else if (v1ClassName.endsWith("Result")) {
            int lastIndex = v1ClassName.lastIndexOf("Result");
            v2ClassName = v1ClassName.substring(0, lastIndex) + "Response";
        }

        if (isS3ModelClass(v2PackagePrefix)) {
            if (S3_POJO_MAPPING.containsKey(v2ClassName)) {
                v2ClassName = S3_POJO_MAPPING.get(v2ClassName);
            }
            if (v2ClassName.startsWith("Set")) {
                v2ClassName = v2ClassName.replace("Set", "Put");
            }
        }

        return v2PackagePrefix + "." + v2ClassName;
    }

    private static boolean isS3ModelClass(String packagePrefix) {
        return packagePrefix.contains("services.s3.model");
    }

    private static String removeS3ModelSubmoduleName(String packagePrefix) {
        for (String submoduleName : S3_MODEL_SUBMODULES) {
            if (packagePrefix.endsWith(submoduleName)) {
                return packagePrefix.substring(0, packagePrefix.length() - submoduleName.length() - 1);
            }
        }
        return packagePrefix;
    }

    /**
     * Edge cases in v1 package names
     */
    private static String checkPackageServiceNameForV2Suffix(String v2PackagePrefix) {
        for (Map.Entry<String, String> entry : PACKAGE_MAPPING.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (v2PackagePrefix.contains(key)) {
                return v2PackagePrefix.replace(key, value);
            }
        }
        return v2PackagePrefix;
    }

    public static String getV2ModelPackageWildCardEquivalent(String currentFqcn) {
        int lastIndexOfDot = currentFqcn.lastIndexOf('.');
        String packagePrefix = currentFqcn.substring(0, lastIndexOfDot);
        String v2PackagePrefix = packagePrefix.replace(V1_PACKAGE_PREFIX, V2_PACKAGE_PREFIX);
        v2PackagePrefix = checkPackageServiceNameForV2Suffix(v2PackagePrefix);
        return v2PackagePrefix + ".*";
    }

    private static String getV2ClientOrExceptionEquivalent(String className) {

        if (className.startsWith("Abstract")) {
            className = className.substring(8);
        }
        if (className.startsWith("Amazon")) {
            className = className.substring(6);
        } else if (className.startsWith("AWS")) {
            className = className.substring(3);
        }

        for (Map.Entry<String, String> entry : CLIENT_MAPPING.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (className.contains(key)) {
                className = className.replace(key, value);
                break;
            }
        }

        String v2Style = pascalCaseIfApplicable(className);

        if (className.endsWith("Exception")) {
            return v2Style;
        }

        if (!className.endsWith("Client") && !className.endsWith("Builder")) {
            v2Style += "Client";
        }

        return v2Style;
    }

    private static String pascalCaseIfApplicable(String className) {
        if (className.startsWith("CSV") || className.startsWith("JSON")) {
            return className;
        }

        return CodegenNamingUtils.pascalCase(className);
    }
}
