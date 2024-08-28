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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.protocols.core.AbstractMarshallingRegistry;

/**
 * Registry of {@link JsonUnmarshaller} implementations by location and type.
 */
@SdkInternalApi
public final class JsonProtocolUnmarshallerRegistry {
    private final JsonUnmarshallerRegistry registry;
    private final JsonUnmarshallerRegistry instantRegistry;

    private JsonProtocolUnmarshallerRegistry(Builder builder) {
        this.registry = Objects.requireNonNull(builder.registry, "registry");
        this.instantRegistry = Objects.requireNonNull(builder.instantRegistry, "instantRegistry");
    }

    @SuppressWarnings("unchecked")
    public <T> JsonUnmarshaller<Object> getUnmarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType) {
        if (marshallingType == MarshallingType.INSTANT) {
            return (JsonUnmarshaller<Object>) instantRegistry.getUnmarshaller(marshallLocation, marshallingType);
        }
        return (JsonUnmarshaller<Object>) registry.getUnmarshaller(marshallLocation, marshallingType);
    }

    /**
     * @return Builder instance to construct a {@link JsonUnmarshallerRegistry}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link JsonUnmarshallerRegistry}.
     */
    public static final class Builder {
        private JsonUnmarshallerRegistry registry;
        private JsonUnmarshallerRegistry instantRegistry;

        private Builder() {
        }

        /**
         * Add the default registry.
         */
        public <T> Builder registry(JsonUnmarshallerRegistry registry) {
            this.registry = registry;
            return this;
        }

        /**
         * Add the protocol specific instant registry.
         */
        public <T> Builder instantRegistry(JsonUnmarshallerRegistry instantRegistry) {
            this.instantRegistry = instantRegistry;
            return this;
        }

        /**
         * @return An immutable {@link JsonUnmarshallerRegistry} object.
         */
        public JsonProtocolUnmarshallerRegistry build() {
            return new JsonProtocolUnmarshallerRegistry(this);
        }
    }
}
