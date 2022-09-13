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
package no.digipost.monitoring.thirdparty;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import no.digipost.monitoring.micrometer.AppStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.BiFunction;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimedThirdPartyCallTest {

    private PrometheusMeterRegistry prometheusRegistry;

    @BeforeEach
    void setUp() {
        prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Test
    void should_return_OK_and_record_timing() {
        final TimedThirdPartyCall<String> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .exceptionAsFailure();

        final String result = getStuff.call(() -> "OK");
        
        assertThat(result, is(equalTo("OK")));

        assertSendOK();
    }

    @Test
    void should_return_OK_and_record_timing_false() {
        final TimedThirdPartyCall<Boolean> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .exceptionAsFailure();

        getStuff.call(() -> false);

        assertSendOK();
    }

    @Test
    void should_return_FAILED_and_record_timing_false() {
        final TimedThirdPartyCall<Boolean> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .exceptionAndFalseAsFailure();

        getStuff.call(() -> false);

        assertSendFailed();
    }

    @Test
    void should_return_OK_and_record_timing_return_null() {
        final TimedThirdPartyCall<String> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .exceptionAsFailure();

        final String result = getStuff.call(() -> null);

        assertNull(result);
        assertSendOK();
    }

    @Test
    void should_return_FAILED_and_record_timing_return_null() {
        final TimedThirdPartyCall<String> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .exceptionAndNullAsFailure();

        final String result = getStuff.call(() -> null);

        assertNull(result);
        assertSendFailed();
    }

    @Test
    void should_return_failed_and_record_timing() {
        final TimedThirdPartyCall<String> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .exceptionAsFailure();

        try {
            getStuff.call(() -> {
                throw new RuntimeException("Whoohaa");
            });
        } catch (RuntimeException e) {
            //swallow
        }

        assertSendFailed();
    }

    @Test
    void should_return_FAILED_with_custom_predicate() {
        final BiFunction<MyResponse, Optional<RuntimeException>, AppStatus> warnOnSituation = (response, possibleException) -> possibleException.isPresent() || "ERROR_SITUATION".equals(response.data) ? AppStatus.WARN : AppStatus.OK;

        final TimedThirdPartyCall<MyResponse> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .callResponseStatus(warnOnSituation);

        getStuff.call(() -> new MyResponse("ERROR_SITUATION"));

        assertSendWarn();
    }

    @Test
    void should_throw_exception_and_record_timing_OK() {
        final BiFunction<String, Optional<RuntimeException>, AppStatus> OKOnSituation = (response, possibleException) -> {
            if (possibleException.isPresent()) {
                return possibleException.get() instanceof RuntimeException ? AppStatus.OK : AppStatus.FAILED;
            } else {
                return AppStatus.OK;
            }
        };

        TimedThirdPartyCall<String> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .callResponseStatus(OKOnSituation);

        assertThrows(RuntimeException.class, () ->
                getStuff.call(() -> {
                    throw new RuntimeException("This should be fine (appstatus.OK), but throw exception anyway so we don't swallow exception that may be handled further out.");
                }));

        assertSendOK();
    }

    @Test
    void define_percentiles() {
        final TimedThirdPartyCall<MyResponse> getStuff = TimedThirdPartyCallDescriptor
                .create("ExternalService", "getStuff", prometheusRegistry, 0.17)
                .exceptionAsFailure();

        getStuff.call(() -> new MyResponse("OK"));
        
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_seconds{name=\"ExternalService_getStuff\",quantile=\"0.17\",}"));
    }

    @Test
    void no_result_should_return_nothing_and_record_request() {
        final NoResultTimedThirdPartyCall getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .noResult().exceptionAsFailure();

        getStuff.call(() -> {});

        assertSendOK();
    }

    @Test
    void exception_should_propagate_and_record_failed_request() {
        final NoResultTimedThirdPartyCall getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
                .noResult().exceptionAsFailure();

        assertThrows(RuntimeException.class, () -> getStuff.call(() -> {
            throw new RuntimeException("Whoohaa");
        }));

        assertSendFailed();
    }

    void assertSendOK() {
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_total{name=\"ExternalService_getStuff\",status=\"OK\",} 1.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_total{name=\"ExternalService_getStuff\",status=\"FAILED\",} 0.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_seconds_count{name=\"ExternalService_getStuff\",} 1.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_seconds{name=\"ExternalService_getStuff\",quantile=\"0.95\",}"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_seconds{name=\"ExternalService_getStuff\",quantile=\"0.99\",}"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_seconds{name=\"ExternalService_getStuff\",quantile=\"0.5\",}"));
    }

    void assertSendFailed() {
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_total{name=\"ExternalService_getStuff\",status=\"OK\",} 0.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_total{name=\"ExternalService_getStuff\",status=\"FAILED\",} 1.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_seconds_count{name=\"ExternalService_getStuff\",} 1.0"));
    }

    void assertSendWarn() {
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_total{name=\"ExternalService_getStuff\",status=\"OK\",} 0.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_total{name=\"ExternalService_getStuff\",status=\"FAILED\",} 0.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_total{name=\"ExternalService_getStuff\",status=\"WARN\",} 1.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_third_party_call_seconds_count{name=\"ExternalService_getStuff\",} 1.0"));
    }

    static class MyResponse {
        final String data;

        MyResponse(String data) {
            this.data = data;
        }
    }
}
