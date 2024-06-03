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
package no.digipost.monitoring.servlet;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import no.digipost.monitoring.servlet.ActiveRequestsFilter.RequestMetaInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static no.digipost.monitoring.servlet.ActiveRequestsFilter.Config.forMaxThreads;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

class ActiveRequestsFilterTest {

    public static final Duration LONG_RUNNING_THRESHOLD = Duration.ofMinutes(1);
    private PrometheusMeterRegistry prometheusRegistry;
    private ActiveRequestsFilter unit;

    @BeforeEach
    void setUp() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Test
    void test_active() {
        unit = new ActiveRequestsFilter(prometheusRegistry, forMaxThreads(10));

        simulateRequest("/hello", Instant.now());
        simulateRequest("/hello", Instant.now());

        assertActive("2.0");
        assertMax("10.0");
        assertLongRunning("0.0");
    }

    @Test
    void test_longrunning() {
        unit = new ActiveRequestsFilter(prometheusRegistry, forMaxThreads(10).longRunningThreshold(LONG_RUNNING_THRESHOLD));

        simulateRequest("/hello", Instant.now().minus(LONG_RUNNING_THRESHOLD).minus(ofSeconds(1)));

        assertActive("1.0");
        assertLongRunning("1.0");
    }

    @Test
    void test_longrunning_excluded() {
        unit = new ActiveRequestsFilter(prometheusRegistry, forMaxThreads(10)
                        .longRunningThreshold(LONG_RUNNING_THRESHOLD)
                        .longRunningExclusions(singletonList(r -> r.path.equals("/hello"))));

        simulateRequest( "/hello", Instant.now().minus(LONG_RUNNING_THRESHOLD).minus(ofSeconds(1)));
        simulateRequest( "/another", Instant.now().minus(LONG_RUNNING_THRESHOLD).minus(ofSeconds(1)));

        assertActive("2.0");
        assertLongRunning("1.0");
    }

    private void simulateRequest(String path, Instant requestReceived) {
        unit.activeRequests.put(UUID.randomUUID(), new RequestMetaInfo(path, "GET", requestReceived));
    }

    private void assertActive(String value) {
        assertThat(prometheusRegistry.scrape(), containsString("app_http_requests_active " + value));
    }

    private void assertMax(String value) {
        assertThat(prometheusRegistry.scrape(), containsString("app_http_requests_max " + value));
    }

    private void assertLongRunning(String value) {
        assertThat(prometheusRegistry.scrape(), containsString("app_http_requests_longrunning " + value));
    }

}
