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

import no.digipost.monitoring.micrometer.AppStatus;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Wrapper of Micrometer time-api to add OK/WARN/FAILED-metrics to enhance alerting capabilities.
 * 
 * Tha API is quite restricted compared to Timer-api:
 * 
 * Usage:
 * <pre>
 * TimedThirdPartyCall&#60;String&#62; getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
 *                 .exceptionAsFailure();
 * String result = getStuff.call(() -&#62; "OK");
 * </pre>
 * 
 * You control what is OK and failed by specifying the function for states that give an AppStatus. This
 * can be done with helper methods on TimedThirdPartyCallDescriptor. You can also 
 * send in your BiFunction to `callResponseStatus`. By doing that, you can differentiate 
 * between situations that is failing but is ignorable. This is useful when you integrate to
 * an api, via batch, which has some kind of timeout-issue. Just check for a 
 * situation and count as WARN in stead.
 * 
 * @param <RESULT> The return of your function
 */
public class TimedThirdPartyCall<RESULT> {

    private final TimedThirdPartyCallDescriptor descriptor;
    private final BiFunction<? super RESULT, Optional<RuntimeException>, AppStatus> reportWarnPredicate;

    public TimedThirdPartyCall(TimedThirdPartyCallDescriptor descriptor, BiFunction<? super RESULT, Optional<RuntimeException>, AppStatus> reportWarnPredicate) {
        this.descriptor = descriptor;
        this.reportWarnPredicate = reportWarnPredicate;
    }

    public RESULT call(Supplier<RESULT> thirdPartyCall) {

        RESULT returnValue = null;
        Optional<RuntimeException> thrown = Optional.empty();
        try {
            returnValue = descriptor.timer.record(thirdPartyCall);
        } catch (RuntimeException t) {
            thrown = Optional.of(t);
        }

        if (AppStatus.FAILED == reportWarnPredicate.apply(returnValue, thrown)) {
            descriptor.failedCounter.increment();
            if (thrown.isPresent()) throw thrown.get();
        } else if (AppStatus.WARN == reportWarnPredicate.apply(returnValue, thrown)) {
            descriptor.warnCounter.increment();
        } else {
            descriptor.successCounter.increment();
        }

        return returnValue;
    }

}
