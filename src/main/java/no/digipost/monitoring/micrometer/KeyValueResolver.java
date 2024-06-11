/*
 * Copyright (C) Posten Norge AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.digipost.monitoring.micrometer;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.Manifest;

@FunctionalInterface
interface KeyValueResolver<V> {

    KeyValueResolver<String> FROM_ENVIRONMENT_VARIABLES = ofNullable(System::getenv);

    KeyValueResolver<String> FROM_SYSTEM_PROPERTIES = ofNullable(System::getProperty);

    /**
     * A resolver which will never resolve any value.
     *
     * Prefer using {@link #noValue()}, which will provide a properly typed
     * reference to this instance.
     */
    KeyValueResolver<?> NO_VALUE = __ -> Optional.empty();


    /**
     * @return A resolver which will never resolve any value.
     */
    static <V> KeyValueResolver<V> noValue() {
        @SuppressWarnings("unchecked")
        KeyValueResolver<V> typedResolver = (KeyValueResolver<V>) NO_VALUE;
        return typedResolver;
    }

    static KeyValueResolver<String> fromManifestMainAttributes(Manifest manifest) {
        return ofNullable(manifest.getMainAttributes()::getValue);
    }


    @SafeVarargs
    static <V> KeyValueResolver<V> inOrderOfPrecedence(KeyValueResolver<V> ... resolvers) {
        return inOrderOfPrecedence(List.of(resolvers));
    }

    static <V> KeyValueResolver<V> inOrderOfPrecedence(List<? extends KeyValueResolver<V>> resolvers) {
        return key -> {
            for (var resolver : resolvers) {
                var value = resolver.tryResolveValue(key);
                if (value.isPresent()) {
                    return value;
                }
            }
            return Optional.empty();
        };
    }



    static <V> KeyValueResolver<V> ofNullable(Function<String, V> nullableValueResolver) {
        return of(nullableValueResolver.andThen(Optional::ofNullable));
    }

    static <V> KeyValueResolver<V> of(Function<? super String, Optional<V>> valueResolver) {
        return key -> valueResolver.apply(key);
    }


    Optional<V> tryResolveValue(String key);

    default <W> KeyValueResolver<W> andThen(BiFunction<? super String, ? super V, W> keyValueMapper) {
        return key -> tryResolveValue(key).map(value -> keyValueMapper.apply(key, value));
    }

}
