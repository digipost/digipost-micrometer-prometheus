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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;

import java.util.List;
import java.util.Optional;

import static no.digipost.monitoring.micrometer.KeyValueResolver.FROM_ENVIRONMENT_VARIABLES;
import static no.digipost.monitoring.micrometer.KeyValueResolver.FROM_SYSTEM_PROPERTIES;
import static no.digipost.monitoring.micrometer.KeyValueResolver.noValue;

public final class MeterFilters {

    /**
     * Try to automatically resolve the application's name to create a {@link MeterFilter} which will
     * include it as a tag for all meter outputs.
     *
     * @return the MeterFilter, or {@link Optional#empty()} if the application name could not be determined
     */
    public static Optional<MeterFilter> tryIncludeApplicationNameCommonTag() {
        return tryIncludeApplicationNameCommonTag(null);
    }


    /**
     * Try to resolve the application's name to create a {@link MeterFilter} which will
     * include it as a tag for all meter outputs.
     *
     * @return the MeterFilter, or {@link Optional#empty()} if the application name could not be determined
     */
    public static Optional<MeterFilter> tryIncludeApplicationNameCommonTag(Class<?> classInJar) {
        return KeyValueResolver.inOrderOfPrecedence(
                    FROM_SYSTEM_PROPERTIES,
                    FROM_ENVIRONMENT_VARIABLES,
                    Optional.ofNullable(classInJar)
                        .flatMap(JarManifest::tryResolveFromClassInJar).or(JarManifest::tryResolveAutomatically)
                        .map(KeyValueResolver::fromManifestMainAttributes)
                .orElse(noValue()))
                .tryResolveValue("Implementation-Title")
                .map(MeterFilters::includeApplicationNameCommonTag);
    }

    /**
     * Create a {@link MeterFilter} which will include the given application name
     * as a tag for all meter outputs.
     *
     * @param applicationName the application name
     *
     * @return the {@link MeterFilter filter}
     */
    public static MeterFilter includeApplicationNameCommonTag(String applicationName) {
        return MeterFilter.commonTags(List.of(Tag.of("application", applicationName)));
    }

}
