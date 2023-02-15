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
package no.digipost.monitoring.event;

import java.util.Optional;

/**
 * An application business event is a countable
 * event with an optional alerting threshold.
 *
 * Example:
 * <code> 
 * public enum MyEvent implements AppBusinessEvent{
 *     VIOLATION_WITH_WARN_AND_ERROR;
 *     
 *  public String getName() {
 *      return name();
 *  }
 *
 *  public Optional&lt;EventsThreshold&gt; getWarnThreshold() {
 *      return Optional.ofNullable(this.warnThreshold);
 *  }
 *
 *  public Optional&lt;EventsThreshold&gt; getErrorThreshold() {
 *      return Optional.ofNullable(this.errorThreshold);
 *  }
 * }
 * </code>
 */
public interface AppBusinessEvent {

    String getName();

    default Optional<EventsThreshold> getWarnThreshold() {
        return Optional.empty();
    }

    default Optional<EventsThreshold> getErrorThreshold() {
        return Optional.empty();
    }
}
