package de.garrus.cloudnet.database.postgres.provider;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import de.dytanic.cloudnet.common.collection.NetorHashMap;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.IThrowableCallback;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.IDatabase;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class PostgresSQLDatabaseProvider extends SQLDatabaseProvider {
    private static final long NEW_CREATION_DELAY = 600000L;
    protected final NetorHashMap<String, Long, IDatabase> cachedDatabaseInstances = new NetorHashMap<>();
    protected final HikariDataSource hikariDataSource = new HikariDataSource();
    private final JsonDocument config;
    private String address;

    public PostgresSQLDatabaseProvider(JsonDocument config,ExecutorService executorService) {
        super(executorService);
        this.config = config;
    }

    public boolean init() {
        this.address = this.config.getString("addresse");
        this.hikariDataSource.setJdbcUrl(address);
        this.hikariDataSource.setUsername(this.config.getString("username"));
        this.hikariDataSource.setPassword(this.config.getString("password"));
        this.hikariDataSource.setDriverClassName("org.postgresql.Driver");
        this.hikariDataSource.setMaximumPoolSize(this.config.getInt("connectionPoolSize"));
        this.hikariDataSource.setConnectionTimeout(this.config.getInt("connectionTimeout"));
        this.hikariDataSource.setValidationTimeout(this.config.getInt("validationTimeout"));
        this.hikariDataSource.validate();
        return true;
    }

    public IDatabase getDatabase(String name) {
        Preconditions.checkNotNull(name);
        this.removedOutdatedEntries();
        if (!this.cachedDatabaseInstances.contains(name)) {
            this.cachedDatabaseInstances.add(name, System.currentTimeMillis() + NEW_CREATION_DELAY, new PostgresDatabase(this, name,super.executorService) {
            });
        }

        return this.cachedDatabaseInstances.getSecond(name);
    }

    public boolean containsDatabase(String name) {
        Preconditions.checkNotNull(name);
        this.removedOutdatedEntries();

        return getDatabaseNames().contains(name);
    }

    public boolean deleteDatabase(String name) {
        Preconditions.checkNotNull(name);
        this.cachedDatabaseInstances.remove(name);
        if (this.containsDatabase(name)) {
            return false;
        }

        try {
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("DROP TABLE " + name);
            boolean deleted = preparedStatement.executeUpdate() != -1;

            if (!preparedStatement.isClosed()) {
                preparedStatement.close();
            }

            if (!connection.isClosed()) {
                connection.close();
            }

            return deleted;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return false;
    }

    public List<String> getDatabaseNames() {
        return this.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='PUBLIC'", resultSet -> {
            List<String> collection = new ArrayList<>();

            while (resultSet.next()) {
                collection.add(resultSet.getString("table_name"));
            }

            return collection;
        });
    }

    public String getName() {
        return this.config.getString("database");
    }

    public void close() {
        this.hikariDataSource.close();
    }

    private void removedOutdatedEntries() {
        Iterator<Map.Entry<String, Pair<Long, IDatabase>>> var1 = this.cachedDatabaseInstances.entrySet().iterator();

        while (var1.hasNext()) {
            Map.Entry<String, Pair<Long, IDatabase>> entry = var1.next();
            if ((Long) ((Pair) entry.getValue()).getFirst() < System.currentTimeMillis()) {
                this.cachedDatabaseInstances.remove(entry.getKey());
            }
        }

    }

    public Connection getConnection() throws SQLException {
        return this.hikariDataSource.getConnection();
    }

    public NetorHashMap<String, Long, IDatabase> getCachedDatabaseInstances() {
        return this.cachedDatabaseInstances;
    }

    public HikariDataSource getHikariDataSource() {
        return this.hikariDataSource;
    }

    public JsonDocument getConfig() {
        return this.config;
    }

    public String getAddress() {
        return this.address;
    }

    public int executeUpdate(String query, Object... objects) {
        Preconditions.checkNotNull(query);
        Preconditions.checkNotNull(objects);

        try {
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query);

            for (int i = 0; i < objects.length; i++) {
                preparedStatement.setString(i + 1, objects[i].toString());
            }
            int i = preparedStatement.executeUpdate();

            if (!preparedStatement.isClosed()) {
                preparedStatement.close();
            }
            if (!connection.isClosed()) {
                connection.close();
            }

            return i;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return -1;
        }

    }

    public <T> T executeQuery(String query, IThrowableCallback<ResultSet, T> callback, Object... objects) {
        Preconditions.checkNotNull(query);
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(objects);

        try {
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query);

            for (int i = 0; i < objects.length; i++) {
                preparedStatement.setString(i + 1, objects[i].toString());
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            try {
                T call = callback.call(resultSet);

                if (!resultSet.isClosed()) {
                    resultSet.close();
                }
                if (!preparedStatement.isClosed()) {
                    preparedStatement.close();
                }
                if (!connection.isClosed()) {
                    connection.close();
                }
                return call;

            } catch (Throwable throwable) {
                if (!resultSet.isClosed()) {
                    resultSet.close();
                }
                if (!preparedStatement.isClosed()) {
                    preparedStatement.close();
                }
                if (!connection.isClosed()) {
                    connection.close();
                }
                throwable.printStackTrace();
                return null;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }

    }
}
