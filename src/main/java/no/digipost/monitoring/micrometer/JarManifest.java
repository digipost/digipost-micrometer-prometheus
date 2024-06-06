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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Manifest;

import static java.lang.Thread.currentThread;
import static no.digipost.monitoring.micrometer.KeyValueResolver.noValue;

class JarManifest {

    private static final Logger LOG = LoggerFactory.getLogger(JarManifest.class);

    static KeyValueResolver<String> tryResolveFromMainAttributes() {
        return tryResolveFromMainAttributes(null);
    }

    static KeyValueResolver<String> tryResolveFromMainAttributes(Class<?> classInJar) {
        return Optional.ofNullable(classInJar)
            .flatMap(JarManifest::tryResolveFromClassInJar).or(JarManifest::tryResolveAutomatically)
            .map(KeyValueResolver::fromManifestMainAttributes)
            .orElse(noValue());
    }

    static Optional<Manifest> tryResolveAutomatically() {
        return Optional.ofNullable(System.getProperty("sun.java.command"))
            .map(sunJavaCommand -> sunJavaCommand.split(" ")[0])
            .flatMap(className -> {
                    try {
                        return Optional.of(Class.forName(className, true, currentThread().getContextClassLoader()));
                    } catch (Exception e) {
                        LOG.info(
                                "Giving up resolving Manifest automatically from class name {}, because {}: {}",
                                className, e.getClass().getSimpleName(), e.getMessage(), e);
                        return Optional.empty();
                    }
                })
            .flatMap(JarManifest::tryResolveFromClassInJar);
    }

    static Optional<Manifest> tryResolveFromClassInJar(Class<?> classInJar) {
        try {
            return Optional.of(resolveFromClassInJar(classInJar));
        } catch (Exception e) {
            LOG.info(
                    "Giving up resolving Manifest from class {}, because {}: {}",
                    classInJar.getName(), e.getClass().getSimpleName(), e.getMessage(), e);
            return Optional.empty();
        }
    }


    private static final String MANIFEST_RESOURCE_NAME = "META-INF/MANIFEST.MF";

    private static final ConcurrentMap<String, Manifest> CACHED_MANIFESTS_BY_JAR_LOCATION = new ConcurrentHashMap<>();

    static Manifest resolveFromClassInJar(Class<?> classInJar) {

        String jarLocationForClass = classInJar.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toString()
                .replaceAll("!/BOOT-INF/classes!/", ""); // If you have an executable jar, your main jar is exploded into a BOOT-INF-folder structure

        return CACHED_MANIFESTS_BY_JAR_LOCATION.computeIfAbsent(jarLocationForClass, jarLocation -> {
            LOG.debug("Trying to resolving {} for {}", MANIFEST_RESOURCE_NAME, jarLocation);
            List<URL> manifestCandidates;
            try {
                manifestCandidates = Collections.list(currentThread().getContextClassLoader().getResources(MANIFEST_RESOURCE_NAME));
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Unable to resolve any resources with name " + MANIFEST_RESOURCE_NAME +
                        " because " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            }

            URL manifestUrl = manifestCandidates.stream()
                    .filter(s -> s.toString().contains(jarLocation))
                    .findAny()
                    .orElseThrow(() -> new NoSuchElementException(
                            MANIFEST_RESOURCE_NAME + " expected located in " + jarLocation + ", resolved from class " + classInJar.getName()));

            try (InputStream manifestStream = manifestUrl.openStream()) {
                return new Manifest(manifestStream);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read MANIFEST.MF from " + manifestUrl + ", resolved from class " + classInJar.getName(), e);
            }
        });

    }

    private JarManifest() {
    }

}
