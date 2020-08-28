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

class LoggerThresholdMetricTest {

    private PrometheusMeterRegistry prometheusRegistry;

    @BeforeEach
    void setUp() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Test
    public void should_create_metrics_for_warn_and_error_threshold(){
        LogbackLoggerMetrics.forLogger(Logger.ROOT_LOGGER_NAME)
                .warnThreshold5min(10)
                .errorThreshold5min(5)
                .bindTo(prometheusRegistry);
        String scrape = prometheusRegistry.scrape();
        
        assertThat(scrape, containsString("log_events_5min_threshold{level=\"warn\",logger=\"ROOT\",} 10.0"));
        assertThat(scrape, containsString("log_events_5min_threshold{level=\"error\",logger=\"ROOT\",} 5.0"));
    }

    @Test
    public void should_only_create_metric_for_warn_threshold(){
        LogbackLoggerMetrics.forLogger(Logger.ROOT_LOGGER_NAME)
                .warnThreshold5min(10)
                .bindTo(prometheusRegistry);
        String scrape = prometheusRegistry.scrape();
        
        assertThat(scrape, containsString("log_events_5min_threshold{level=\"warn\",logger=\"ROOT\",} 10.0"));
        assertThat(scrape, not(containsString("log_events_5min_threshold{level=\"error\"")));
    }

    @Test
    public void should_not_create_metrics_for_threshold(){
        LogbackLoggerMetrics.forLogger(Logger.ROOT_LOGGER_NAME)
                .bindTo(prometheusRegistry);
        String scrape = prometheusRegistry.scrape();
        
        assertThat(scrape, not(containsString("log_events_5min_threshold")));
    }

}