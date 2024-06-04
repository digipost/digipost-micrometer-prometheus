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

import java.io.IOException;
import java.util.Collections;
import java.util.jar.Manifest;

class JarManifest extends Manifest {

    JarManifest() throws ClassNotFoundException {
        this(
                Class.forName(((String) System.getProperties().get("sun.java.command")).split(" ")[0], true, Thread.currentThread().getContextClassLoader())
        );
    }

    JarManifest(Class<?> classFromJar) {
        final String jarLocation = classFromJar.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toString()
                .replaceAll("!/BOOT-INF/classes!/", ""); // If you have an executable jar, your main jar is exploded into a BOOT-INF-folder structure

        try {
            Collections.list(Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF"))
                    .stream()
                    .filter(s -> s.toString().contains(jarLocation))
                    .findAny()
                    .ifPresent(mf -> {
                        try {
                            this.read(mf.openStream());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Det blir ikke noe informasjon fra META-INF/MANIFEST.MF");
            e.printStackTrace();
        }
    }
}
