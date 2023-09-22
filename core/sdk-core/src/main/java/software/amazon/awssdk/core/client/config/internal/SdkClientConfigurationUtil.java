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

package software.amazon.awssdk.core.client.config.internal;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;


@SdkInternalApi
public final class SdkClientConfigurationUtil {
    private SdkClientConfigurationUtil() {
    }


    /**
     * Invokes all the plugins to the given configuration and returns the updated configuration.
     */
    public static SdkClientConfiguration invokePlugins(
        SdkClientConfiguration clientConfiguration,
        List<SdkPlugin> plugins,
        ConfigurationUpdater<SdkServiceClientConfiguration.Builder> handler
    ) {
        if (plugins.isEmpty()) {
            return clientConfiguration;
        }
        SdkClientConfiguration.Builder configBuilder = clientConfiguration.toBuilder();
        return handler.update(builder -> {
            for (SdkPlugin plugin : plugins) {
                plugin.configureClient(builder);
            }
        }, configBuilder);
    }
}
