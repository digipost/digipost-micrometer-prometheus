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

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;


class ApplicationInfoMetricsTest {

    private PrometheusMeterRegistry prometheusRegistry;

    @BeforeEach
    void setUp() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Test
    public void shouldNotThrowErrorIfManifestValuesDontExists(){
        try {
            ApplicationInfoMetrics applicationInfoMetrics = new ApplicationInfoMetrics(ApplicationInfoMetricsTest.class);
            applicationInfoMetrics.bindTo(prometheusRegistry);
            System.out.println(prometheusRegistry.scrape());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void shouldOverrideManifestValueWithSystemProperty() {
        ApplicationInfoMetrics applicationInfoMetrics = new ApplicationInfoMetrics(Logger.class);
        String app = "NotMyApp";
        try {
            System.setProperty("Implementation-Title", app);
            applicationInfoMetrics.bindTo(prometheusRegistry);
        } finally {
            System.getProperties().remove("Implementation-Title");
        }
        String scrapeResult = prometheusRegistry.scrape();
        assertThat(scrapeResult, containsString(String.format("application=\"%s\"", app)));
    }

}
