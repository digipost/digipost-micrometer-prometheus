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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static no.digipost.monitoring.micrometer.KeyValueResolver.FROM_ENVIRONMENT_VARIABLES;
import static no.digipost.monitoring.micrometer.KeyValueResolver.FROM_SYSTEM_PROPERTIES;
import static no.digipost.monitoring.micrometer.KeyValueResolver.noValue;

/**
 * Adds `app.info` gauge that has several tags suitable for showing information about
 * build version, java version and such. This will also
 * bind a commonTag in registry `application` with a
 * walue typically from `artifactId` in your pom.
 * <p>
 * Values fetched system properties, process environment and from <code>manifest.mf</code>,
 * in that order. You can use system properties or environment variables to
 * override manifest values, or supply configuration when an application is
 * not run from a jar.
 */
public class ApplicationInfoMetrics implements MeterBinder {

    private final KeyValueResolver<String> fromRuntimeEnvironment;

    /**
     * This method will find your running mainclass from System.properties("sun.java.command")
     * and base the Manifest file from the jar containing that class.
     * Usually that is ok. But if that is not the case, use the other constructor
     * and specify the class yourself.
     *
     * @throws ClassNotFoundException - Throws if class from System.properties("sun.java.command") is not found.
     * It could be that the property is missing?
     */
    public ApplicationInfoMetrics() throws ClassNotFoundException {
        this(null);
    }

    /**
     * Base metrics tags of MANIFEST.MF from jar witch holds your class.
     *
     * @param classInJar - Class contained in jar you want metrics from
     */
    public ApplicationInfoMetrics(Class<?> classInJar) {
        this.fromRuntimeEnvironment = KeyValueResolver
                .inOrderOfPrecedence(
                    FROM_SYSTEM_PROPERTIES,
                    FROM_ENVIRONMENT_VARIABLES,
                    Optional.ofNullable(classInJar)
                        .flatMap(JarManifest::tryResolveFromClassInJar).or(JarManifest::tryResolveAutomatically)
                        .map(KeyValueResolver::fromManifestMainAttributes)
                        .orElse(noValue()));
    }


    @Override
    public void bindTo(MeterRegistry registry) {
        List<Tag> tags = Stream.of(
                fromRuntimeEnvironment.tryResolveValue("Git-Build-Time").map(buildTime -> Tag.of("buildTime", buildTime)),
                fromRuntimeEnvironment.tryResolveValue("Git-Build-Version").map(buildVersion -> Tag.of("buildVersion", buildVersion)),
                fromRuntimeEnvironment.tryResolveValue("Git-Commit").map(buildNumber -> Tag.of("buildNumber", buildNumber)),
                fromRuntimeEnvironment.tryResolveValue("Build-Jdk-Spec").map(javaBuildVersion -> Tag.of("javaBuildVersion", javaBuildVersion)),
                FROM_SYSTEM_PROPERTIES.tryResolveValue("java.version").map(javaVersion -> Tag.of("javaVersion", javaVersion)))
            .flatMap(Optional::stream)
            .collect(toList());

        Gauge.builder("app.info", () -> 1.0d)
                .description("General build and runtime information about the application. This is a static value")
                .tags(tags)
                .register(registry);
    }

}
