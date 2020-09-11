package de.garrus.cloudnet.database.postgres.provider;

import de.dytanic.cloudnet.database.AbstractDatabaseProvider;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.module.NodeCloudNetModule;

public class CloudNetPostgresDatabaseModule extends NodeCloudNetModule {
    private static CloudNetPostgresDatabaseModule instance;

    public static CloudNetPostgresDatabaseModule getInstance() {
        return instance;
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.LOADED)
    public void init() {
        instance = this;
    }

    @ModuleTask(order = 126, event = ModuleLifeCycle.LOADED)
    public void initConfig() {
        this.getConfig().getString("addresse", "jdbc:postgresql://127.0.0.1:5432/database");
        this.getConfig().getString("username", "root");
        this.getConfig().getString("password", "root");
        this.getConfig().getInt("connectionPoolSize", 15);
        this.getConfig().getInt("connectionTimeout", 5000);
        this.getConfig().getInt("validationTimeout", 5000);
        this.saveConfig();
    }

    @ModuleTask(order = 125, event = ModuleLifeCycle.LOADED)
    public void registerDatabaseProvider() {
        this.getRegistry().registerService(AbstractDatabaseProvider.class, "postgres", new PostgresSQLDatabaseProvider(getConfig(),null));
    }

    @ModuleTask(order = 127, event = ModuleLifeCycle.STOPPED)
    public void unregisterDatabaseProvider() {
        this.getRegistry().unregisterService(AbstractDatabaseProvider.class, "postgres");
    }
}
