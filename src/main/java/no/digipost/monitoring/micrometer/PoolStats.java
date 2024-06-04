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
 * This interface need to be implemented for your specific
 * ConnectionPool (eg. C3P0, Hikari...)
 */
public interface PoolStats {

    /**
     * @return A string with implementation name of connection pool in use. (eg. 'C3P0')
     */
    String implementation();

    /**
     * @return A string with type of connection pool in use
     */
    String type();

    /**
     * @return size of connection pool
     */
    int getMaxPoolSize();

    /**
     * @return number of connections in use from the pool
     */
    int getNumberOfUsedConnections();
}
