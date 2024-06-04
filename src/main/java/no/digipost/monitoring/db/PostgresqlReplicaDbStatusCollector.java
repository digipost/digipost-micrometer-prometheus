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
package no.digipost.monitoring.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This collector is able to say if the database is in recovery and thus queries primary
 * 
 * This will only work with a PostgreSQL database. Hence the name.
 */
public class PostgresqlReplicaDbStatusCollector implements DbStatusCollector {

    private static final String VALIDATION_QUERY = "SELECT pg_is_in_recovery()";
    private static final String TYPE = "replica";
    private DataSource ds;

    public PostgresqlReplicaDbStatusCollector(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public DbStatus check() {
        try (final Connection conn = ds.getConnection();
             final PreparedStatement ps = conn.prepareStatement(VALIDATION_QUERY);
             final ResultSet rs = ps.executeQuery()) {

            rs.next();
            if (rs.getBoolean(1)) {
                return DbStatus.OK;
            } else {
                return DbStatus.CONNECTED_TO_PRIMARY;
            }

        } catch (SQLException e) {
            return DbStatus.ERROR;
        }
    }

    @Override
    public String type() {
        return TYPE;
    }
}
