/**
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
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.jar.Manifest;

/**
 * Adds `app.info` gauge that has several tags suitable for showing information about
 * build version, java version and such. This will also
 * bind a commonTag in registry `application` with a
 * walue typically from `artifactId` in your pom.
 * <p>
 * Values fetched from `manifest.mf` and `System.getProperties()`.
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
    public ApplicationInfoMetrics(Class classFromJar) {
        manifest = new JarManifest(classFromJar);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        if (manifest.getMainAttributes().isEmpty()) {
            // Guard for not packaged run
            return;
        }

        final String artifactId = manifest.getMainAttributes().getValue("Implementation-Title");
        registry.config().commonTags("application", artifactId);

        Tags tags = Tags.of(
            "buildTime", manifest.getMainAttributes().getValue("Git-Build-Time")
            , "buildVersion", manifest.getMainAttributes().getValue("Git-Build-Version")
            , "buildNumber", manifest.getMainAttributes().getValue("Git-Commit")
            , "javaBuildVersion", manifest.getMainAttributes().getValue("Build-Jdk-Spec")
            , "javaVersion", (String) System.getProperties().get("java.version")
        );

        Gauge.builder("app.info", () -> 1.0d)
            .description("General build and runtime information about the application. This is a static value")
            .tags(tags)
            .register(registry);
    }
}
