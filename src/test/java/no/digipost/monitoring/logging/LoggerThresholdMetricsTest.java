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
package no.digipost.monitoring.logging;

import ch.qos.logback.classic.Logger;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class LoggerThresholdMetricsTest {

    private PrometheusMeterRegistry prometheusRegistry;

    @BeforeEach
    void setUp() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Test
    public void should_be_present(){
        LoggerThresholdMetrics.forLogger(Logger.ROOT_LOGGER_NAME,1, 1).bindTo(prometheusRegistry);
        String scrape = prometheusRegistry.scrape();
        System.out.println(scrape);
        assertThat(scrape, containsString("log_events_1min_threshold{level=\"warn\",logger=\"ROOT\",} 1.0"));
        assertThat(scrape, containsString("log_events_1min_threshold{level=\"error\",logger=\"ROOT\",} 1.0"));
    }

}