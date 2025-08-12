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

package software.amazon.awssdk.services.tick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.reactivex.rxjava3.core.Flowable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.tick.model.FizzBuzzRequest;
import software.amazon.awssdk.services.tick.model.FizzBuzzResponseHandler;
import software.amazon.awssdk.services.tick.model.ValueStream;

public class IntegrationTest {
    private static final Integer TICK_SERVICE_PORT = 8443; // 8000;

    public TickAsyncClient client() {
        return TickAsyncClient.builder()
                              .endpointOverride(URI.create("http://localhost:" + TICK_SERVICE_PORT))
                              .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("1REF0RPQBH0B9Q5T83R2", "ba1VyImxA8GbypNix6E1g1ar1ziwVsg2RxMo9sj8"
                                                                                                                                       + "")))
                              .region(Region.US_WEST_2)
                              .build();
    }

    @Test
    public void fixBuzzIntegrationTest() throws Exception {
        TickAsyncClient client = client();
        ExecutorService e = Executors.newCachedThreadPool();
        List<Future<?>> l = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            Future<?> f = e.submit(() -> {
                long range = 125_000;

                Flowable<ValueStream> inputStream = Flowable.rangeLong(1, range)
                    .map(n -> ValueStream.valueBuilder().value(n).build());
                Set<Long> unbuzzed = new HashSet<>();
                AtomicLong receivedEvents = new AtomicLong();

                FizzBuzzResponseHandler.Visitor.Builder visitorBuilder = FizzBuzzResponseHandler.Visitor.builder();
                visitorBuilder.onFizz(fizz -> {
                                  receivedEvents.incrementAndGet();
                                  long value = fizz.value();
                                  assertEquals(0, value % 3);
                                  if (value % 5 == 0) {
                                      assertTrue(unbuzzed.add(value), "Fizz already received for " + value);
                                  }
                              })
                              .onBuzz(buzz -> {
                                  receivedEvents.incrementAndGet();
                                  long value = buzz.value();
                                  assertEquals(0, value % 5);
                                  if (value % 3 == 0) {
                                      assertTrue(unbuzzed.remove(value), "No fizz for " + value);
                                  }
                              });
                FizzBuzzRequest request = FizzBuzzRequest.builder().build();
                CompletableFuture<?> res =
                    client.fizzBuzz(request,
                                    inputStream,
                                    FizzBuzzResponseHandler.builder()
                                                           .subscriber(visitorBuilder.build())
                                                           .build());
                res.join();
                assertTrue(unbuzzed.isEmpty(), unbuzzed.size() + " unbuzzed fizzes");
                assertEquals((range / 3) + (range / 5), receivedEvents.get());
            });
            l.add(f);
        }

        for (Future<?> f : l) {
            f.get();
        }
    }
}
