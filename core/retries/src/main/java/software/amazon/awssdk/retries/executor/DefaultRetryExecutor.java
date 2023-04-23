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

package software.amazon.awssdk.retries.executor;


import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.retries.RetryExecutor;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.internal.Sleeper;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public class DefaultRetryExecutor implements RetryExecutor {

    private final Sleeper sleeper;
    private final RetryStrategy strategy;
    private final String scope;

    public DefaultRetryExecutor(RetryStrategy strategy, Sleeper sleeper, String scope) {
        this.strategy = Validate.paramNotNull(strategy, "strategy");
        this.sleeper = Validate.paramNotNull(sleeper, "sleeper");
        this.scope = Validate.paramNotNull(scope, "scope");
    }

    @Override
    public <T> T execute(Callable<T> callable) {
        try {
            return wrap(callable).call();
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(Runnable runnable) {
        wrap(runnable).run();
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(Executor executor, Callable<T> callable) {
        return CompletableFuture.supplyAsync(() -> executeCallable(callable), executor);
    }

    @Override
    public CompletableFuture<Void> executeAsync(Executor executor, Runnable runnable) {
        return executeAsync(executor, Executors.callable(runnable, null));
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> callable) {
        return () -> executeCallable(callable);
    }

    @Override
    public Runnable wrap(Runnable runnable) {
        return () -> executeCallable(Executors.callable(runnable, null));
    }

    private <T> T executeCallable(Callable<T> callable) {
        AcquireInitialTokenRequest acquireRequest = AcquireInitialTokenRequest.create(scope);
        AcquireInitialTokenResponse acquireResponse = strategy.acquireInitialToken(acquireRequest);
        sleeper.sleep(acquireResponse.delay());
        RetryToken retryToken = acquireResponse.token();
        while (true) {
            try {
                T response = callable.call();
                RecordSuccessRequest recordSuccessRequest = RecordSuccessRequest.create(retryToken);
                strategy.recordSuccess(recordSuccessRequest);
                return response;
            } catch (Exception e) {
                Optional<Duration> suggestedDelay = suggestedDelay(e);
                RefreshRetryTokenRequest refreshRequest = RefreshRetryTokenRequest.builder()
                                                                                  .failure(e)
                                                                                  .token(retryToken)
                                                                                  .suggestedDelay(suggestedDelay.orElse(null))
                                                                                  .build();
                RefreshRetryTokenResponse refreshResponse = strategy.refreshRetryToken(refreshRequest);
                retryToken = refreshResponse.token();
                sleeper.sleep(refreshResponse.delay());
            }
        }
    }

    static Optional<Duration> suggestedDelay(Exception e) {
        return Optional.empty();
    }
}