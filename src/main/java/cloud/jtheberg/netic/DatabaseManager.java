package cloud.jtheberg.netic;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de base de données pour l'historique
 * Support SQLite et MariaDB avec pool de connexions HikariCP
 */
public class DatabaseManager {

    private HikariDataSource dataSource;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String databaseType;

    /**
     * Initialise la connexion à la base de données
     */
    public void initialize() throws SQLException {
        databaseType = NeticPlugin.getInstance().getConfig().getString("database.type", "sqlite");

        HikariConfig config = new HikariConfig();

        if (databaseType.equalsIgnoreCase("sqlite")) {
            initializeSQLite(config);
        } else if (databaseType.equalsIgnoreCase("mariadb")) {
            initializeMariaDB(config);
        } else {
            throw new IllegalArgumentException("Type de BDD invalide: " + databaseType);
        }

        // Configuration du pool
        ConfigurationSection poolConfig = NeticPlugin.getInstance().getConfig().getConfigurationSection("database.pool");
        if (poolConfig != null) {
            config.setMaximumPoolSize(poolConfig.getInt("maximum-pool-size", 10));
            config.setMinimumIdle(poolConfig.getInt("minimum-idle", 2));
            config.setConnectionTimeout(poolConfig.getLong("connection-timeout", 30000));
            config.setIdleTimeout(poolConfig.getLong("idle-timeout", 600000));
            config.setMaxLifetime(poolConfig.getLong("max-lifetime", 1800000));
        }

        dataSource = new HikariDataSource(config);
        createTables();

        NeticPlugin.getInstance().getLogger().info("✅ Pool de connexions HikariCP initialisé");
    }

    private void initializeSQLite(HikariConfig config) {
        String fileName = NeticPlugin.getInstance().getConfig().getString("database.sqlite.file", "netic_history.db");
        File dbFile = new File(NeticPlugin.getInstance().getDataFolder(), fileName);

        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("NeticAI-SQLite-Pool");

        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("cache_size", "10000");
        config.addDataSourceProperty("temp_store", "MEMORY");

        NeticPlugin.getInstance().getLogger().info("📁 SQLite: " + dbFile.getAbsolutePath());
    }

    private void initializeMariaDB(HikariConfig config) {
        ConfigurationSection mariaConfig = NeticPlugin.getInstance().getConfig().getConfigurationSection("database.mariadb");

        if (mariaConfig == null) {
            throw new IllegalArgumentException("Configuration MariaDB manquante");
        }

        String host = mariaConfig.getString("host", "localhost");
        int port = mariaConfig.getInt("port", 3306);
        String database = mariaConfig.getString("database", "netic");
        String username = mariaConfig.getString("username", "root");
        String password = mariaConfig.getString("password", "");

        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setPoolName("NeticAI-MariaDB-Pool");

        ConfigurationSection props = mariaConfig.getConfigurationSection("properties");
        if (props != null) {
            for (String key : props.getKeys(false)) {
                config.addDataSourceProperty(key, props.get(key).toString());
            }
        }

        NeticPlugin.getInstance().getLogger().info("🔗 MariaDB: " + host + ":" + port + "/" + database);
    }

    private void createTables() throws SQLException {
        String createTableSQL;

        if (databaseType.equalsIgnoreCase("sqlite")) {
            createTableSQL = """
                CREATE TABLE IF NOT EXISTS netic_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    role TEXT NOT NULL,
                    player_name TEXT,
                    message TEXT NOT NULL
                )
                """;

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON netic_history(timestamp)");
            }

        } else {
            createTableSQL = """
                CREATE TABLE IF NOT EXISTS netic_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    timestamp DATETIME NOT NULL,
                    role VARCHAR(50) NOT NULL,
                    player_name VARCHAR(36),
                    message TEXT NOT NULL,
                    INDEX idx_timestamp (timestamp)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
            }
        }

        NeticPlugin.getInstance().getLogger().info("✅ Table 'netic_history' créée/vérifiée");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource non initialisée");
        }
        return dataSource.getConnection();
    }

    public void addMessage(String role, String playerName, String message) {
        String sql = "INSERT INTO netic_history (timestamp, role, player_name, message) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, LocalDateTime.now().format(formatter));
            stmt.setString(2, role);
            stmt.setString(3, playerName);
            stmt.setString(4, message);

            stmt.executeUpdate();

        } catch (SQLException e) {
            NeticPlugin.getInstance().getLogger().severe("❌ Erreur ajout message BDD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<HistoryEntry> getRecentMessages(int limit) {
        List<HistoryEntry> messages = new ArrayList<>();
        String sql = "SELECT timestamp, role, player_name, message FROM netic_history ORDER BY id DESC LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(new HistoryEntry(
                            rs.getString("timestamp"),
                            rs.getString("role"),
                            rs.getString("player_name"),
                            rs.getString("message")
                    ));
                }
            }

        } catch (SQLException e) {
            NeticPlugin.getInstance().getLogger().severe("❌ Erreur récupération messages: " + e.getMessage());
            e.printStackTrace();
        }

        java.util.Collections.reverse(messages);
        return messages;
    }

    public int getMessageCount() {
        String sql = "SELECT COUNT(*) as count FROM netic_history";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            NeticPlugin.getInstance().getLogger().severe("❌ Erreur comptage messages: " + e.getMessage());
        }

        return 0;
    }

    public void clearHistory() {
        String sql = "DELETE FROM netic_history";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            int deleted = stmt.executeUpdate(sql);
            NeticPlugin.getInstance().getLogger().info("🗑️ " + deleted + " messages supprimés");

        } catch (SQLException e) {
            NeticPlugin.getInstance().getLogger().severe("❌ Erreur suppression historique: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            NeticPlugin.getInstance().getLogger().info("✅ Pool de connexions fermé");
        }
    }

    public static class HistoryEntry {
        public final String timestamp;
        public final String role;
        public final String playerName;
        public final String message;

        public HistoryEntry(String timestamp, String role, String playerName, String message) {
            this.timestamp = timestamp;
            this.role = role;
            this.playerName = playerName;
            this.message = message;
        }

        @Override
        public String toString() {
            if (playerName != null && !playerName.isEmpty()) {
                return role + " " + playerName + ": " + message;
            }
            return role + ": " + message;
        }
    }
}