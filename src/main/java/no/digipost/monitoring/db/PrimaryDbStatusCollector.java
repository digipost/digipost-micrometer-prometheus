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
package no.digipost.monitoring.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PrimaryDbStatusCollector implements DbStatusCollector {

    private static final String VALIDATION_QUERY = "SELECT 1";
    private static final String TYPE = "primary";
    private DataSource ds;

    public PrimaryDbStatusCollector(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public DbStatus check() {
        try (final Connection conn = ds.getConnection();
             final PreparedStatement ps = conn.prepareStatement(VALIDATION_QUERY)) {
            ps.execute();
            return DbStatus.OK;
        } catch (SQLException e) {
            return DbStatus.ERROR;
        }
    }

    @Override
    public String type() {
        return TYPE;
    }
}
