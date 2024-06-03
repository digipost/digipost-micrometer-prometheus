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
package no.digipost.monitoring;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import no.digipost.monitoring.logging.LogbackLoggerMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogbackLoggerMetricsTest {

    private PrometheusMeterRegistry prometheusRegistry;

    @BeforeEach
    void setUp() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }


    @Test
    void test_named_logger() {
        LogbackLoggerMetrics.forLogger("test").bindTo(prometheusRegistry);

        LoggerFactory.getLogger("test").warn("warn");
        LoggerFactory.getLogger("test.sublevel").warn("warn");
        LoggerFactory.getLogger("test").error("error");
        LoggerFactory.getLogger("another.logger").error("another error");

        assertThat(prometheusRegistry.scrape(), containsString("logback_logger_events_total{level=\"warn\",logger=\"test\",} 2.0"));
        assertThat(prometheusRegistry.scrape(), containsString("logback_logger_events_total{level=\"error\",logger=\"test\",} 1.0"));
    }

    @Test
    void test_root_logger() {
        LogbackLoggerMetrics.forRootLogger().bindTo(prometheusRegistry);

        LoggerFactory.getLogger("test").error("error");
        LoggerFactory.getLogger("test").warn("warn");

        assertThat(prometheusRegistry.scrape(), containsString("logback_logger_events_total{level=\"warn\",logger=\"ROOT\",} 1.0"));
        assertThat(prometheusRegistry.scrape(), containsString("logback_logger_events_total{level=\"error\",logger=\"ROOT\",} 1.0"));
    }

    @Test
    void test_both_root_and_named_logger() {
        LogbackLoggerMetrics.forRootLogger().bindTo(prometheusRegistry);
        LogbackLoggerMetrics.forLogger("test").bindTo(prometheusRegistry);

        LoggerFactory.getLogger("test").error("error"); // counted by both
        LoggerFactory.getLogger("another.logger").error("error"); // counted by root

        assertThat(prometheusRegistry.scrape(), containsString("logback_logger_events_total{level=\"error\",logger=\"ROOT\",} 2.0"));
        assertThat(prometheusRegistry.scrape(), containsString("logback_logger_events_total{level=\"error\",logger=\"test\",} 1.0"));
    }

    @Test
    void test_excluded_loggers_should_not_be_included_in_count() {
        LogbackLoggerMetrics.forRootLogger()
                .excludeLogger("ignored")
                .excludeLogger("also.ignored")
                .bindTo(prometheusRegistry);

        LoggerFactory.getLogger("ignored").error("error");
        LoggerFactory.getLogger("also.ignored").error("error");
        LoggerFactory.getLogger("another.logger").error("error"); // counted by root

        assertThat(prometheusRegistry.scrape(), containsString("logback_logger_events_total{level=\"error\",logger=\"ROOT\",} 1.0"));
    }

}
