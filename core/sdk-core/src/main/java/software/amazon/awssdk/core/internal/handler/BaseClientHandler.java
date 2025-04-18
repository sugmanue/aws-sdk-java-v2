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

package software.amazon.awssdk.core.internal.handler;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.BiFunction;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.core.internal.io.SdkLengthAwareInputStream;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public abstract class BaseClientHandler {
    private SdkClientConfiguration clientConfiguration;

    protected BaseClientHandler(SdkClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    /**
     * Finalize {@link SdkHttpFullRequest} by running beforeMarshalling, afterMarshalling,
     * modifyHttpRequest, modifyHttpContent and modifyAsyncHttpContent interceptors
     */
    static <InputT extends SdkRequest, OutputT> InterceptorContext finalizeSdkHttpFullRequest(
        ClientExecutionParams<InputT, OutputT> executionParams,
        ExecutionContext executionContext, InputT inputT,
        SdkClientConfiguration clientConfiguration) {

        runBeforeMarshallingInterceptors(executionContext);

        Pair<SdkHttpFullRequest, Duration> measuredMarshall = MetricUtils.measureDuration(() ->
                executionParams.getMarshaller().marshall(inputT));

        executionContext.metricCollector().reportMetric(CoreMetric.MARSHALLING_DURATION, measuredMarshall.right());

        SdkHttpFullRequest request = measuredMarshall.left();

        request = modifyEndpointHostIfNeeded(request, clientConfiguration, executionParams);

        addHttpRequest(executionContext, request);
        runAfterMarshallingInterceptors(executionContext);
        return runModifyHttpRequestAndHttpContentInterceptors(executionContext);
    }

    private static void runBeforeMarshallingInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().beforeMarshalling(executionContext.interceptorContext(),
                                                              executionContext.executionAttributes());
    }

    /**
     * Modifies the given {@link SdkHttpFullRequest} with new host if host prefix is enabled and set.
     */
    private static SdkHttpFullRequest modifyEndpointHostIfNeeded(SdkHttpFullRequest originalRequest,
                                                                 SdkClientConfiguration clientConfiguration,
                                                                 ClientExecutionParams executionParams) {
        if (executionParams.discoveredEndpoint() != null) {
            URI discoveredEndpoint = executionParams.discoveredEndpoint();
            executionParams.putExecutionAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true);
            return originalRequest.toBuilder().host(discoveredEndpoint.getHost()).port(discoveredEndpoint.getPort()).build();
        }

        Boolean disableHostPrefixInjection = clientConfiguration.option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION);
        if ((disableHostPrefixInjection != null && disableHostPrefixInjection.equals(Boolean.TRUE)) ||
            StringUtils.isEmpty(executionParams.hostPrefixExpression())) {
            return originalRequest;
        }

        return originalRequest.toBuilder()
                              .host(executionParams.hostPrefixExpression() + originalRequest.host())
                              .build();
    }

    private static void addHttpRequest(ExecutionContext executionContext, SdkHttpFullRequest request) {
        InterceptorContext interceptorContext = executionContext.interceptorContext();

        Optional<ContentStreamProvider> contentStreamProvider = request.contentStreamProvider();
        if (contentStreamProvider.isPresent()) {
            interceptorContext = interceptorContext.copy(b -> b.httpRequest(request)
                                                               .requestBody(getBody(request)));
        } else {
            interceptorContext = interceptorContext.copy(b -> b.httpRequest(request));
        }

        executionContext.interceptorContext(interceptorContext);
    }

    private static RequestBody getBody(SdkHttpFullRequest request) {
        Optional<ContentStreamProvider> contentStreamProviderOptional = request.contentStreamProvider();
        if (contentStreamProviderOptional.isPresent()) {
            Optional<String> contentLengthOptional = request.firstMatchingHeader("Content-Length");
            long contentLength = Long.parseLong(contentLengthOptional.orElse("0"));
            String contentType = request.firstMatchingHeader("Content-Type").orElse("");

            // Enforce the content length specified only if it was present on the request (and not the default).
            ContentStreamProvider streamProvider = contentStreamProviderOptional.get();
            if (contentLengthOptional.isPresent()) {
                ContentStreamProvider toWrap = contentStreamProviderOptional.get();
                streamProvider = () -> new SdkLengthAwareInputStream(toWrap.newStream(), contentLength);
            }

            return new SdkInternalOnlyRequestBody(streamProvider,
                                                  contentLength,
                                                  contentType);
        }

        return null;
    }

    private static void runAfterMarshallingInterceptors(ExecutionContext executionContext) {
        executionContext.interceptorChain().afterMarshalling(executionContext.interceptorContext(),
                                                             executionContext.executionAttributes());
    }

    private static InterceptorContext runModifyHttpRequestAndHttpContentInterceptors(ExecutionContext executionContext) {
        InterceptorContext interceptorContext =
            executionContext.interceptorChain().modifyHttpRequestAndHttpContent(executionContext.interceptorContext(),
                                                                                executionContext.executionAttributes());
        executionContext.interceptorContext(interceptorContext);
        return interceptorContext;
    }

    /**
     * Run afterUnmarshalling and modifyResponse interceptors.
     */
    private static <OutputT extends SdkResponse> BiFunction<OutputT, SdkHttpFullResponse, OutputT>
        runAfterUnmarshallingInterceptors(ExecutionContext context) {

        return (input, httpFullResponse) -> {
            // Update interceptor context to include response
            InterceptorContext interceptorContext =
                context.interceptorContext().copy(b -> b.response(input));

            context.interceptorChain().afterUnmarshalling(interceptorContext, context.executionAttributes());

            interceptorContext = context.interceptorChain().modifyResponse(interceptorContext, context.executionAttributes());

            // Store updated context
            context.interceptorContext(interceptorContext);

            return (OutputT) interceptorContext.response();
        };
    }

    private static <OutputT extends SdkResponse> BiFunction<OutputT, SdkHttpFullResponse, OutputT>
        attachHttpResponseToResult() {

        return ((response, httpFullResponse) -> (OutputT) response.toBuilder().sdkHttpResponse(httpFullResponse).build());
    }

    // This method is only called from tests, since the subclasses in aws-core override it.
    protected <InputT extends SdkRequest, OutputT extends SdkResponse> ExecutionContext
        invokeInterceptorsAndCreateExecutionContext(
        ClientExecutionParams<InputT, OutputT> params) {
        SdkClientConfiguration clientConfiguration = resolveRequestConfiguration(params);
        SdkRequest originalRequest = params.getInput();

        ExecutionAttributes executionAttributes = params.executionAttributes();
        executionAttributes
            .putAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT, 1)
            .putAttribute(SdkExecutionAttribute.SERVICE_CONFIG,
                          clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION))
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, clientConfiguration.option(SdkClientOption.SERVICE_NAME))
            .putAttribute(SdkExecutionAttribute.PROFILE_FILE,
                          clientConfiguration.option(SdkClientOption.PROFILE_FILE_SUPPLIER) != null ?
                          clientConfiguration.option(SdkClientOption.PROFILE_FILE_SUPPLIER).get() : null)
            .putAttribute(SdkExecutionAttribute.PROFILE_FILE_SUPPLIER,
                          clientConfiguration.option(SdkClientOption.PROFILE_FILE_SUPPLIER))
            .putAttribute(SdkExecutionAttribute.PROFILE_NAME, clientConfiguration.option(SdkClientOption.PROFILE_NAME));

        ExecutionInterceptorChain interceptorChain =
            new ExecutionInterceptorChain(clientConfiguration.option(SdkClientOption.EXECUTION_INTERCEPTORS));

        InterceptorContext interceptorContext = InterceptorContext.builder()
                                                                  .request(originalRequest)
                                                                  .build();

        interceptorChain.beforeExecution(interceptorContext, executionAttributes);
        interceptorContext = interceptorChain.modifyRequest(interceptorContext, executionAttributes);

        MetricCollector metricCollector = resolveMetricCollector(params);

        return ExecutionContext.builder()
                               .interceptorChain(interceptorChain)
                               .interceptorContext(interceptorContext)
                               .executionAttributes(executionAttributes)
                               .signer(clientConfiguration.option(SdkAdvancedClientOption.SIGNER))
                               .metricCollector(metricCollector)
                               .build();
    }

    protected boolean isCalculateCrc32FromCompressedData() {
        return clientConfiguration.option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED);
    }

    protected void validateSigningConfiguration(SdkHttpRequest request, Signer signer) {
        if (signer == null) {
            return;
        }

        if (signer.credentialType() != CredentialType.TOKEN) {
            return;
        }

        URI endpoint = request.getUri();
        if (!"https".equals(endpoint.getScheme())) {
            throw SdkClientException.create("Cannot use bearer token signer with a plaintext HTTP endpoint: " + endpoint);
        }
    }

    protected SdkClientConfiguration resolveRequestConfiguration(ClientExecutionParams<?, ?> params) {
        SdkClientConfiguration config =  params.requestConfiguration();
        if (config != null) {
            return config;
        }
        return clientConfiguration;
    }

    /**
     * Decorate response handlers by running after unmarshalling Interceptors and adding http response metadata.
     */
    <OutputT extends SdkResponse> HttpResponseHandler<OutputT> decorateResponseHandlers(
        HttpResponseHandler<OutputT> delegate, ExecutionContext executionContext) {

        return resultTransformationResponseHandler(delegate, responseTransformations(executionContext));
    }

    <OutputT extends SdkResponse> HttpResponseHandler<Response<OutputT>> decorateSuccessResponseHandlers(
        HttpResponseHandler<Response<OutputT>> delegate, ExecutionContext executionContext) {

        return successTransformationResponseHandler(delegate, responseTransformations(executionContext));
    }

    <OutputT extends SdkResponse> HttpResponseHandler<Response<OutputT>> successTransformationResponseHandler(
        HttpResponseHandler<Response<OutputT>> responseHandler,
        BiFunction<OutputT, SdkHttpFullResponse, OutputT> successTransformer) {

        return (response, executionAttributes) -> {
            Response<OutputT> delegateResponse = responseHandler.handle(response, executionAttributes);

            if (delegateResponse.isSuccess()) {
                return delegateResponse.toBuilder()
                                       .response(successTransformer.apply(delegateResponse.response(), response))
                                       .build();
            } else {
                return delegateResponse;
            }
        };
    }

    <OutputT extends SdkResponse> HttpResponseHandler<OutputT> resultTransformationResponseHandler(
        HttpResponseHandler<OutputT> responseHandler,
        BiFunction<OutputT, SdkHttpFullResponse, OutputT> successTransformer) {

        return (response, executionAttributes) -> {
            OutputT delegateResponse = responseHandler.handle(response, executionAttributes);
            return successTransformer.apply(delegateResponse, response);
        };
    }

    static void validateCombinedResponseHandler(ClientExecutionParams<?, ?> executionParams) {
        if (executionParams.getCombinedResponseHandler() != null) {
            if (executionParams.getResponseHandler() != null) {
                throw new IllegalArgumentException("Only one of 'combinedResponseHandler' and 'responseHandler' may "
                                                   + "be specified in a ClientExecutionParams object");
            }

            if (executionParams.getErrorResponseHandler() != null) {
                throw new IllegalArgumentException("Only one of 'combinedResponseHandler' and 'errorResponseHandler' "
                                                   + "may be specified in a ClientExecutionParams object");
            }
        }
    }

    /**
     * Returns the composition of 'runAfterUnmarshallingInterceptors' and 'attachHttpResponseToResult' response
     * transformations as a single transformation that should be applied to all responses.
     */
    private static <T extends SdkResponse> BiFunction<T, SdkHttpFullResponse, T>
        responseTransformations(ExecutionContext executionContext) {

        return composeResponseFunctions(runAfterUnmarshallingInterceptors(executionContext),
                                        attachHttpResponseToResult());
    }

    /**
     * Composes two functions passing the result of the first function as the first argument of the second function
     * and the same second argument to both functions. This is used by response transformers to chain together and
     * pass through a persistent SdkHttpFullResponse object as a second arg turning them effectively into a single
     * response transformer.
     * <p>
     * So given f1(x, y) and f2(x, y) where x is typically OutputT and y is typically SdkHttpFullResponse the composed
     * function would be f12(x, y) = f2(f1(x, y), y).
     */
    private static <T, R> BiFunction<T, R, T> composeResponseFunctions(BiFunction<T, R, T> function1,
                                                                       BiFunction<T, R, T> function2) {
        return (x, y) -> function2.apply(function1.apply(x, y), y);
    }

    private MetricCollector resolveMetricCollector(ClientExecutionParams<?, ?> params) {
        MetricCollector metricCollector = params.getMetricCollector();
        if (metricCollector == null) {
            metricCollector = MetricCollector.create("ApiCall");
        }
        return metricCollector;
    }
}
