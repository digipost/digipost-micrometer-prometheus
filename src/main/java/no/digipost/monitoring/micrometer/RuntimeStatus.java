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

/**
 * Values for runtime status.
 * <p>
 * You can use the states as metrics values, as well as
 * the name of the state string to create liveness/readyness
 * in kubernets, loadbalancer-switching. Or what ever you choose.
 */
public class RuntimeStatus {

    private State state = State.STARTING;

    public void set(State newState) {
        state = newState;
    }

    public State get() {
        return state;
    }

    public enum State {
        OFFLINE(-2), SHUTTING_DOWN(-1), STARTING(0), ONLINE(1);

        private final int value;

        State(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
