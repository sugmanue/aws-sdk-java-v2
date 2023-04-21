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

package software.amazon.awssdk.retries;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;

/**
 * A utility for retrying actions based on a {@link RetryStrategy}.
 *
 * <p>Standard usage includes:
 * <ol>
 *     <li>Creation via {@link #create(RetryStrategy)}, {@link #createStandard}
 *     or {@link #createAdaptive}
 *     <li>
 *         <ul>
 *             <li>Calling {@link #execute} to synchronously run a retryable task
 *             <li>Calling {@link #executeAsync} to asynchronously run a retryable task
 *             <li>Calling {@link #wrap} to convert a runnable or callable into a retryable version
 *         </ul>
 *     </li>
 * </ol>
 *
 * <p><b>Examples</b>
 *
 * <p><i>Creation without an existing {@link RetryStrategy}</i>
 * <pre>
 * RetryExecutor retryExecutor =
 *     RetryExecutor.createStandard(c -> c.retryOnExceptionInstanceOf(IllegalArgumentException.class)
 *                                        .retryOnExceptionInstanceOf(IllegalStateException.class)
 *                                        .maxAttempts(3));
 * </pre>
 *
 * <p><i>Synchronously retrying a method call</i>
 * <pre>
 * retryExecutor.execute(SomeClass::methodToTryUntilSuccess);
 * </pre>
 *
 * <p><i>Asynchronously retrying a method call</i>
 * <pre>
 * retryExecutor.executeAsync(ForkJoinPool.commonPool(), SomeClass::methodToTryUntilSuccess)
 *              .join();
 * </pre>
 *
 * <p><i>Wrapping tasks that will be submitted to an executor</i>
 * <pre>
 * ExecutorService executorService = ...;
 * Runnable runnable = retryExecutor.wrap(SomeClass::methodToTryUntilSuccess);
 * executorService.execute(runnable);
 * </pre>
 */
@SdkPublicApi
@ThreadSafe
public interface RetryExecutor extends Executor {
    /**
     * Create a RetryExecutor based on the {@link RetryStrategy}.
     *
     * <p>Example Usage
     * <pre>
     * RetryExecutor retryExecutor =
     *     RetryExecutor.createStandard(c -> c.retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                                        .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                                        .maxAttempts(3));
     * </pre>
     */
    static RetryExecutor createStandard(Consumer<RetryStrategy.Builder> configurer) {
        return null;
    }

    /**
     * Create a RetryExecutor based on the {@link RetryStrategy}.
     *
     * <p>Example Usage
     * <pre>
     * RetryExecutor retryExecutor =
     *     RetryExecutor.createAdaptive(c -> c.retryOnExceptionInstanceOf(IllegalArgumentException.class)
     *                                        .retryOnExceptionInstanceOf(IllegalStateException.class)
     *                                        .maxAttempts(3));
     * </pre>
     */
    static RetryExecutor createAdaptive(Consumer<RetryStrategy.Builder> configurer) {
        return null;
    }

    /**
     * Create a {@code RetryExecutor} from a {@link RetryStrategy}.
     */
    static RetryExecutor create(RetryStrategy retryStrategy) {
        return null;
    }

    /**
     * Execute the provided callable, retrying on failure.
     *
     * <p>If the callable succeeds, the result is returned. If the callable fails with a
     * non-retryable error or retries are exhausted, the failure exception is thrown as-is. Checked exceptions thrown by the
     * callable are wrapped with a {@link TokenAcquisitionFailedException}.
     *
     * @throws TokenAcquisitionFailedException If the provided callable threw a checked exception. The checked exception is
     *                                         preserved as the cause.
     */
    <T> T execute(Callable<T> callable);

    /**
     * Execute the provided runnable, retrying on failure.
     *
     * <p>If the callable succeeds, the result is returned. If the callable fails with a
     * non-retryable error or retries are exhausted, the failure exception is thrown as-is.
     */
    @Override
    void execute(Runnable runnable);

    /**
     * Execute the provided callable asynchronously using the provided executor service, retrying on failure.
     *
     * <p>If the callable succeeds, the future is completed with the result. If the callable or
     * executor fails with a non-retryable exception or if retries are exhausted, the future is completed with the failure
     * exception.
     */
    <T> CompletableFuture<T> executeAsync(Executor executorService,
                                          Callable<T> callable);

    /**
     * Execute the provided runnable asynchronously using the provided executor, retrying on failure.
     *
     * <p>If the runnable succeeds, the future is completed with the result. If the runnable or
     * executor fails with a non-retryable exception or if retries are exhausted, the future is completed with the failure
     * exception.
     */
    CompletableFuture<Void> executeAsync(Executor executor,
                                         Runnable runnable);

    /**
     * Decorate the provided callable with the retry behavior configured on this executor.
     *
     * <p>Failures that occur within the callable that are not retried will be thrown as-is
     * without any wrapping.
     */
    <T> Callable<T> wrap(Callable<T> callable);

    /**
     * Decorate the provided runnable with the retry behavior configured on this executor.
     *
     * <p>Failures that occur within the runnable that are not retried will be thrown as-is
     * without any wrapping.
     */
    Runnable wrap(Runnable callable);
}