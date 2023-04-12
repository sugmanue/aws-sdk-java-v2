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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.retries.backoff.DefaultBackoffStrategies.exponentialDelay;
import static software.amazon.awssdk.retries.internal.StandardRetryStrategyTest.TestCase;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.circuitbreaker.DefaultTokenBucketStore;

public class StandardRetryStrategyMiscTest {
    static final int TEST_EXCEPTION_COST = 5;
    static final int TEST_MAX = 50;
    static final IllegalArgumentException IAE = new IllegalArgumentException();
    static final RuntimeException RTE = new RuntimeException();

    @Test
    public void circuitBreakerRemembersState() {
        BackoffStrategy backoff = exponentialDelay(Duration.ofMillis(10), Duration.ofSeconds(25));
        TestCase testCase = new TestCase("foobar")
            .configure(b -> b.maxAttempts(3))
            .configure(b -> b.retryOnException(IllegalArgumentException.class))
            .configure(b -> b.backoffStrategy(backoff))
            .fineTune(b -> b.tokenBucketExceptionCost(TEST_EXCEPTION_COST))
            .fineTune(b -> b.tokenBucketStore(DefaultTokenBucketStore
                                                   .builder()
                                                   .tokenBucketMaxCapacity(TEST_MAX)
                                                   .build()))
            .givenExceptions(IAE, IAE);

        // The test case will throw twice and then succeed, so each run will withdraw 2 * TEST_EXCEPTION_COST and deposit back
        // TEST_EXCEPTION_COST.
        StandardRetryStrategy strategy = testCase.builder.build();
        int total = TEST_MAX;
        for (int idx = 0; idx < 9; idx++) {
            String name = testCase.name + " round " + idx;
            TestCase.runTestCase(testCase, strategy);
            assertThat(name, testCase.thrown, is(nullValue()));
            assertThat(name, testCase.succeeded, equalTo(true));
            assertThat(name, testCase.token.capacityRemaining(), equalTo(total - TEST_EXCEPTION_COST));
            assertThat(name, testCase.token.state(), equalTo(DefaultRetryToken.TokenState.SUCCEEDED));
            total -= TEST_EXCEPTION_COST;
        }
        // The tokens have been exhausted, assert that the next call will fail.
        String name = testCase.name + " no more tokens available";
        TestCase.runTestCase(testCase, strategy);
        assertThat(name, testCase.thrown, is(notNullValue()));
        assertThat(name, testCase.succeeded, equalTo(false));
        assertThat(name, testCase.token.capacityRemaining(), equalTo(0));
        assertThat(name, testCase.token.state(), equalTo(DefaultRetryToken.TokenState.TOKEN_ACQUISITION_FAILED));
    }
}
