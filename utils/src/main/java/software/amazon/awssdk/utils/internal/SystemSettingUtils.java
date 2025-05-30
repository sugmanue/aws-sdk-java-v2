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

package software.amazon.awssdk.utils.internal;

import static software.amazon.awssdk.utils.OptionalUtils.firstPresent;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * A set of static utility methods for shared code in {@link SystemSetting}.
 *
 * <p>
 * Implementation notes: this class should've been outside internal package,
 * but we can't fix it due to backwards compatibility reasons.
 */
@SdkProtectedApi
public final class SystemSettingUtils {
    private static final Logger LOG = Logger.loggerFor(SystemSettingUtils.class);

    private SystemSettingUtils() {
    }

    /**
     * Resolve the value of this system setting, loading it from the System by checking:
     * <ol>
     *     <li>The system properties.</li>
     *     <li>The environment variables.</li>
     *     <li>The default value.</li>
     * </ol>
     */
    public static Optional<String> resolveSetting(SystemSetting setting) {
        return firstPresent(resolveProperty(setting), () -> resolveEnvironmentVariable(setting), () -> resolveDefault(setting))
                .map(String::trim);
    }

    /**
     * Resolve the value of this system setting, loading it from the System by checking:
     * <ol>
     *     <li>The system properties.</li>
     *     <li>The environment variables.</li>
     * </ol>
     * <p>
     * This is similar to {@link #resolveSetting(SystemSetting)} but does not fall back to the default value if neither
     * the environment variable or system property value are present.
     */
    public static Optional<String> resolveNonDefaultSetting(SystemSetting setting) {
        return firstPresent(resolveProperty(setting), () -> resolveEnvironmentVariable(setting))
                .map(String::trim);
    }

    /**
     * Attempt to load this setting from the system properties.
     */
    private static Optional<String> resolveProperty(SystemSetting setting) {
        // CHECKSTYLE:OFF - This is the only place we're allowed to use System.getProperty
        return Optional.ofNullable(setting.property()).map(System::getProperty);
        // CHECKSTYLE:ON
    }

    /**
     * Attempt to load this setting from the environment variables.
     */
    public static Optional<String> resolveEnvironmentVariable(SystemSetting setting) {
        return resolveEnvironmentVariable(setting.environmentVariable());
    }

    /**
     * Attempt to load a key from the environment variables.
     */
    public static Optional<String> resolveEnvironmentVariable(String key) {
        try {
            return Optional.ofNullable(key).map(SystemSettingUtilsTestBackdoor::getEnvironmentVariable);
        } catch (SecurityException e) {
            LOG.debug(() -> String.format("Unable to load the environment variable %s because the security manager did not "
                                          + "allow the SDK to read this system property. This setting will be assumed to be "
                                          + "null", key), e);
            return Optional.empty();
        }
    }

    /**
     * Load the default value from the setting.
     */
    private static Optional<String> resolveDefault(SystemSetting setting) {
        return Optional.ofNullable(setting.defaultValue());
    }

    /**
     * Convert a string to boolean safely (as opposed to the less strict {@link Boolean#parseBoolean(String)}). If a customer
     * specifies a boolean value it should be "true" or "false" (case insensitive) or an exception will be thrown.
     */
    public static Boolean safeStringToBoolean(SystemSetting setting, String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        }

        throw new IllegalStateException("Environment variable '" + setting.environmentVariable() + "' or system property '" +
                                        setting.property() + "' was defined as '" + value + "', but should be 'false' or 'true'");
    }


}
