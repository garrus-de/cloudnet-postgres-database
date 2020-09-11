package de.garrus.cloudnet.database.postgres.provider;

import de.dytanic.cloudnet.database.sql.SQLDatabase;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;

import java.util.concurrent.ExecutorService;

public class PostgresDatabase extends SQLDatabase {
    public PostgresDatabase(SQLDatabaseProvider databaseProvider, String name, ExecutorService executorService) {
        super(databaseProvider, name, executorService);
    }
}
