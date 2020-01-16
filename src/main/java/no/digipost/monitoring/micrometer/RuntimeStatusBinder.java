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
package no.digipost.monitoring.micrometer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.NonNull;

/**
 * @see RuntimeStatus for possible values of the metric
 */
public class RuntimeStatusBinder implements MeterBinder {

    private RuntimeStatus status;

    public RuntimeStatusBinder(RuntimeStatus status) {
        this.status = status;
    }

    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        Gauge.builder("app_runtime_status", () -> status.get().getValue())
                .description("State of application with regards the runtime status")
                .register(registry);
    }
}
