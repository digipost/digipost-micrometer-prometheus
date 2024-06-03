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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.Manifest;

import static java.util.Optional.ofNullable;

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

    final Manifest manifest;

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
        manifest = new JarManifest();
    }

    /**
     * Base metrics tags of MANIFEST.MF from jar witch holds your class.
     *
     * @param classFromJar - Class contained in jar you want metrics from
     */
    public ApplicationInfoMetrics(Class<?> classFromJar) {
        manifest = new JarManifest(classFromJar);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        fromManifestOrEnv("Implementation-Title")
                .ifPresent(artifactId -> registry.config().commonTags("application", artifactId));

        List<Tag> tags = new ArrayList<>();

        addTagIfValuePresent(tags,"buildTime","Git-Build-Time");
        addTagIfValuePresent(tags,"buildVersion","Git-Build-Version");
        addTagIfValuePresent(tags,"buildNumber","Git-Commit");
        addTagIfValuePresent(tags,"javaBuildVersion","Build-Jdk-Spec");

        tags.add(Tag.of("javaVersion", (String) System.getProperties().get("java.version")));

        Gauge.builder("app.info", () -> 1.0d)
                .description("General build and runtime information about the application. This is a static value")
                .tags(tags)
                .register(registry);
    }

    private void addTagIfValuePresent(List<Tag> tags, String tagKey, String valueName) {
        fromManifestOrEnv(valueName).ifPresent(value -> tags.add(Tag.of(tagKey, value)));
    }

    private Optional<String> fromManifestOrEnv(String name) {
        String value = environmentVariableOrSystemProperty(name);
        if (value == null) {
            value = manifest.getMainAttributes().getValue(name);
        }
        return ofNullable(value);
    }

    private static String environmentVariableOrSystemProperty(String name) {
        String value = System.getProperty(name);
        if (value == null) {
            value = System.getenv(name);
        }
        return value;
    }
}
