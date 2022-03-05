package de.garrus.cloudnet.database.postgres.provider;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.database.sql.SQLDatabase;
import de.dytanic.cloudnet.database.sql.SQLDatabaseProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class PostgresDatabase extends SQLDatabase {
    public PostgresDatabase(SQLDatabaseProvider databaseProvider, String name, ExecutorService executorService) {
        super(databaseProvider, name, executorService);
    }

    @Override
    public boolean isSynced() {
        return false;
    }


    @Override
    public List<JsonDocument> get(JsonDocument filters) {
        Preconditions.checkNotNull(filters);

        StringBuilder stringBuilder = new StringBuilder("SELECT ").append(TABLE_COLUMN_VALUE).append(" FROM `")
                .append(this.name).append('`');

        Collection<String> collection = new ArrayList<>();

        if (filters.size() > 0) {
            stringBuilder.append(" WHERE ");

            Iterator<String> iterator = filters.iterator();
            String item;

            while (iterator.hasNext()) {
                item = iterator.next();

                stringBuilder.append(TABLE_COLUMN_VALUE).append(" LIKE ?");
                collection.add("%\"" + item + "\":" + filters.get(item).toString().replaceAll("([_%])", "\\$$1") + "%");

                if (iterator.hasNext()) {
                    stringBuilder.append(" and ");
                }
            }
        }

        return this.databaseProvider.executeQuery(
                stringBuilder.toString(),
                resultSet -> {
                    List<JsonDocument> jsonDocuments = new ArrayList<>();
                    while (resultSet.next()) {
                        jsonDocuments.add(JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));
                    }

                    return jsonDocuments;
                },
                collection.toArray()
        );
    }


    @Override
    public List<JsonDocument> get(String fieldName, Object fieldValue) {
        Preconditions.checkNotNull(fieldName);
        Preconditions.checkNotNull(fieldValue);

        return this.databaseProvider.executeQuery(
                String.format("SELECT %s FROM `%s` WHERE %s LIKE ? ", TABLE_COLUMN_VALUE, this.name,
                        TABLE_COLUMN_VALUE),
                resultSet -> {
                    List<JsonDocument> jsonDocuments = new ArrayList<>();
                    while (resultSet.next()) {
                        jsonDocuments.add(JsonDocument.newDocument(resultSet.getString(TABLE_COLUMN_VALUE)));
                    }

                    return jsonDocuments;
                },
                "%\"" + fieldName + "\":" + JsonDocument.GSON.toJson(fieldValue).replaceAll("([_%])", "\\$$1") + "%"
        );
    }
}
