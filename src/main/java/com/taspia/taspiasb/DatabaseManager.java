package com.taspia.taspiasb;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private String tableName;
    private boolean enabled;
    
    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enabled = false;
        this.dataSource = null;
    }
    
    /**
     * Initialize the database connection from server config
     */
    public void initialize() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.getLogger().info("No server config found, MySQL fallback disabled");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection mysqlSection = config.getConfigurationSection("mysql");
        
        if (mysqlSection == null) {
            plugin.getLogger().warning("No 'mysql' section found in server config, MySQL fallback disabled");
            return;
        }
        
        this.enabled = mysqlSection.getBoolean("enabled", false);
        
        if (!enabled) {
            plugin.getLogger().info("MySQL fallback is disabled in config");
            return;
        }
        
        try {
            String host = mysqlSection.getString("host", "localhost");
            int port = mysqlSection.getInt("port", 3306);
            String database = mysqlSection.getString("database", "alonsolevels");
            String username = mysqlSection.getString("username", "");
            String password = mysqlSection.getString("password", "");
            this.tableName = mysqlSection.getString("table_name", "alonsoskyblock_users");
            
            if (username.isEmpty() || password.isEmpty()) {
                plugin.getLogger().severe("MySQL username or password is empty! Please configure your MySQL settings.");
                this.enabled = false;
                return;
            }
            
            // HikariCP configuration
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // Pool settings from config
            ConfigurationSection poolSection = mysqlSection.getConfigurationSection("pool");
            if (poolSection != null) {
                hikariConfig.setMaximumPoolSize(poolSection.getInt("maximum_pool_size", 10));
                hikariConfig.setMinimumIdle(poolSection.getInt("minimum_idle", 2));
                hikariConfig.setConnectionTimeout(poolSection.getLong("connection_timeout", 30000));
                hikariConfig.setIdleTimeout(poolSection.getLong("idle_timeout", 600000));
                hikariConfig.setMaxLifetime(poolSection.getLong("max_lifetime", 1800000));
            }
            
            // Connection properties
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            this.dataSource = new HikariDataSource(hikariConfig);
            
            // Test connection
            try (Connection connection = dataSource.getConnection()) {
                plugin.getLogger().info("Successfully connected to MySQL database for level fallback");
                plugin.getLogger().info("Using table: " + tableName);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize MySQL connection for level fallback", e);
            this.enabled = false;
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error configuring MySQL connection", e);
            this.enabled = false;
        }
    }
    
    /**
     * Get player level from MySQL database
     * @param playerUuid Player UUID to look up
     * @return Player level from database, or -1 if not found or error
     */
    public int getPlayerLevelFromDatabase(UUID playerUuid) {
        if (!enabled || dataSource == null) {
            return -1;
        }
        
        String query = "SELECT lastlevel FROM " + tableName + " WHERE uuid = ?";
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setString(1, playerUuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int level = resultSet.getInt("lastlevel");
                    plugin.getLogger().fine("Retrieved level " + level + " for player " + playerUuid + " from database");
                    return level;
                } else {
                    plugin.getLogger().fine("No database record found for player " + playerUuid);
                    return -1;
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error querying player level from database for " + playerUuid, e);
            return -1;
        }
    }
    
    /**
     * Check if MySQL fallback is enabled and available
     * @return true if MySQL fallback can be used
     */
    public boolean isEnabled() {
        return enabled && dataSource != null;
    }
    
    /**
     * Get connection pool statistics for debugging
     * @return Connection pool info string
     */
    public String getConnectionInfo() {
        if (!enabled || dataSource == null) {
            return "MySQL disabled";
        }
        
        return String.format("MySQL Pool - Active: %d, Idle: %d, Total: %d, Waiting: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }
    
    /**
     * Shutdown the database connection pool
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("MySQL connection pool closed");
        }
    }
    
    /**
     * Reload the database configuration
     */
    public void reload() {
        shutdown();
        initialize();
        plugin.getLogger().info("MySQL configuration reloaded");
    }
} 