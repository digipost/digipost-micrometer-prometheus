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

import io.micrometer.core.instrument.binder.MeterBinder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

class JarManifestTest {

    @Test
    void should_read_manifest_file_from_a_jar_a_class_resides_in() {
        final JarManifest jarManifest = new JarManifest(MeterBinder.class);

        assertThat(jarManifest.getMainAttributes().getValue("Manifest-Version"), is(equalTo("1.0")));
        assertThat(jarManifest.getMainAttributes().getValue("Implementation-Title"), containsString("io.micrometer"));
    }
}
