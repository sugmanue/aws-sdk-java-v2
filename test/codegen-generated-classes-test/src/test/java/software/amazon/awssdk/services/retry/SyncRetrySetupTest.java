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

package software.amazon.awssdk.services.retry;

import java.util.List;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;

public class SyncRetrySetupTest extends BaseRetrySetupTest<ProtocolRestJsonClient, ProtocolRestJsonClientBuilder> {
    @Override
    protected ProtocolRestJsonClientBuilder newClientBuilder() {
        return ProtocolRestJsonClient.builder();
    }

    @Override
    protected AllTypesResponse callAllTypes(ProtocolRestJsonClient client, List<SdkPlugin> requestPlugins) {
        return client.allTypes(r -> r.overrideConfiguration(c -> {
            for (SdkPlugin plugin : requestPlugins) {
                c.addPlugin(plugin);
            }
        }));
    }
}
