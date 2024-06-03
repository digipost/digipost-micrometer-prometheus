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

/**
 * Values to make generic alerts for specific known error-states. This is 
 * useful when you have batch-processes that fails.
 * 
 * Usage:
 * 
 * AtomicInteger status = new AtomicInteger(OK.code());
 * Metrics.gauge("app_status", status);
 * 
 * [...]
 * // Error-situation occur
 * status.set(FAILED.code());
 * 
 */
public enum AppStatus {
    FAILED(-1), WARN(0), OK(1);

    private int i;

    AppStatus(int i) {
        this.i = i;
    }

    public int code() {
        return i;
    }
}
