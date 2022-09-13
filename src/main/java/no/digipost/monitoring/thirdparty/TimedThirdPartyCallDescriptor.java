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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import no.digipost.monitoring.micrometer.AppStatus;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Defaults and factory methods for TimedThirdPartyCall
 */
public class TimedThirdPartyCallDescriptor {

    private static final BiFunction<Object, Optional<RuntimeException>, AppStatus> failOnException = (response, possibleException) -> possibleException.isPresent() ? AppStatus.FAILED : AppStatus.OK;
    private static final BiFunction<Object, Optional<RuntimeException>, AppStatus> failOnExceptionOrNull = (response, possibleException) -> (possibleException.isPresent() || response == null) ? AppStatus.FAILED : AppStatus.OK;
    private static final BiFunction<Boolean, Optional<RuntimeException>, AppStatus> failOnExceptionOrFalse = (response, possibleException) -> (possibleException.isPresent() || (response != null && !response)) ? AppStatus.FAILED : AppStatus.OK;
    final Counter successCounter;
    final Counter warnCounter;
    final Counter failedCounter;
    final Timer timer;
    private final String group;
    private final String endpoint;

    private TimedThirdPartyCallDescriptor(String group, String endpoint, MeterRegistry prometheusRegistry, double... percentiles) {
        this.group = group;
        this.endpoint = endpoint;
        this.successCounter = prometheusRegistry.counter("app_third_party_call_total", Tags.of("name", getName(), "status", AppStatus.OK.name()));
        this.warnCounter = prometheusRegistry.counter("app_third_party_call_total", Tags.of("name", getName(), "status", AppStatus.WARN.name()));
        this.failedCounter = prometheusRegistry.counter("app_third_party_call_total", Tags.of("name", getName(), "status", AppStatus.FAILED.name()));

        if (percentiles.length == 0) {
            this.timer = Timer.builder("app_third_party_call")
                    .tags(Tags.of("name", getName()))
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(prometheusRegistry);
        } else {
            this.timer = Timer.builder("app_third_party_call")
                    .tags(Tags.of("name", getName()))
                    .publishPercentiles(percentiles)
                    .register(prometheusRegistry);
        }
    }

    public static TimedThirdPartyCallDescriptor create(String group, String endpoint, MeterRegistry prometheusRegistry, double... percentiles) {
        return new TimedThirdPartyCallDescriptor(group, endpoint, prometheusRegistry, percentiles);
    }

    public class NoResultTimedThirdPartyCallDescriptor {

        private NoResultTimedThirdPartyCallDescriptor() {}

        public NoResultTimedThirdPartyCall exceptionAsFailure() {
            return new NoResultTimedThirdPartyCall(TimedThirdPartyCallDescriptor.this, failOnException);
        }

    }

    public NoResultTimedThirdPartyCallDescriptor noResult() {
        return new NoResultTimedThirdPartyCallDescriptor();
    }

    public <RESULT> TimedThirdPartyCall<RESULT> exceptionAsFailure() {
        return build(failOnException);
    }


    public <RESULT> TimedThirdPartyCall<RESULT> exceptionAndNullAsFailure() {
        return build(failOnExceptionOrNull);
    }

    public TimedThirdPartyCall<Boolean> exceptionAndFalseAsFailure() {
        return new TimedThirdPartyCall<>(this, failOnExceptionOrFalse);
    }

    public <RESULT> TimedThirdPartyCall<RESULT> callResponseStatus(BiFunction<? super RESULT, Optional<RuntimeException>, AppStatus> reportWarnPredicate) {
        return build(reportWarnPredicate);
    }

    private <RESULT> TimedThirdPartyCall<RESULT> build(BiFunction<? super RESULT, Optional<RuntimeException>, AppStatus> reportWarnPredicate) {
        return new TimedThirdPartyCall<>(this, reportWarnPredicate);
    }

    String getName() {
        return group + "_" + endpoint;
    }
}
