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

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.co.probablyfine.matchers.Java8Matchers.where;


class ApplicationInfoMetricsTest {

    private PrometheusMeterRegistry prometheusRegistry;

    @BeforeEach
    void setUp() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Test
    public void shouldNotThrowErrorIfManifestValuesDontExists() {
        assertDoesNotThrow(() -> {
            var metrics = new ApplicationInfoMetrics(ApplicationInfoMetricsTest.class);
            metrics.bindTo(prometheusRegistry);
            System.out.println(prometheusRegistry.scrape());
        });
    }

    @Test
    void shouldOverrideManifestValueWithSystemProperty() {
        var meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        var metrics = new ApplicationInfoMetrics(Logger.class);

        metrics.bindTo(meterRegistry);
        assertThat(meterRegistry, where(PrometheusMeterRegistry::scrape, matchesRegex("(?s).+javaBuildVersion=\".+\".+")));


        String overriddenVersion = "Microsoft J# Version 0.001 Alpha";
        try {
            System.setProperty("Build-Jdk-Spec", overriddenVersion);
            metrics.bindTo(meterRegistry);
        } finally {
            System.clearProperty("Build-Jdk-Spec");
        }
        assertThat(meterRegistry, where(PrometheusMeterRegistry::scrape, containsString("javaBuildVersion=\"" + overriddenVersion)));
    }

}
