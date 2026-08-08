package dev.dcbridge.dcbridge.whitelist;

import dev.dcbridge.dcbridge.config.BotConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class WhitelistStore implements AutoCloseable {
    private final JavaPlugin plugin;
    private final BotConfig config;
    private Connection connection;

    public WhitelistStore(JavaPlugin plugin, BotConfig config) {
        this.plugin = plugin;
        this.config = config;
        init();
    }

    private void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/" + config.getSqliteFileName();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA journal_mode=WAL;");
                statement.executeUpdate("PRAGMA busy_timeout=5000;");
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS whitelist_requests (
                      id TEXT PRIMARY KEY,
                      userId TEXT NOT NULL,
                      guildId TEXT NOT NULL,
                      username TEXT NOT NULL,
                      platform TEXT NOT NULL,
                      status TEXT NOT NULL DEFAULT 'pending',
                      createdAt INTEGER NOT NULL,
                      handledBy TEXT,
                      handledAt INTEGER,
                      logMessageId TEXT,
                      queueMessageId TEXT
                    )
                    """);
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS whitelisted_players (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      userId TEXT NOT NULL,
                      guildId TEXT NOT NULL,
                      username TEXT NOT NULL,
                      platform TEXT NOT NULL,
                      status TEXT NOT NULL DEFAULT 'active',
                      addedAt INTEGER NOT NULL,
                      removedAt INTEGER,
                      requestId TEXT
                    )
                    """);
            }
            plugin.getLogger().info("SQLite whitelist store initialized at " + dbPath);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to initialize whitelist SQLite store: " + ex.getMessage());
        }
    }

    public boolean isActiveUsername(String username) {
        if (connection == null) {
            return false;
        }
        String sql = "SELECT 1 FROM whitelisted_players WHERE status='active' AND LOWER(username)=LOWER(?) LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet set = statement.executeQuery()) {
                return set.next();
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to check whitelist username: " + ex.getMessage());
            return false;
        }
    }

    public void putPendingRequest(String id, String userId, String guildId, String username, String platform) {
        if (connection == null) {
            return;
        }
        String sql = "INSERT INTO whitelist_requests(id,userId,guildId,username,platform,status,createdAt) VALUES (?,?,?,?,?,'pending',?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, userId);
            statement.setString(3, guildId);
            statement.setString(4, username);
            statement.setString(5, platform);
            statement.setLong(6, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to insert pending request: " + ex.getMessage());
        }
    }

    public void updateRequestStatus(String id, String status, String handledBy) {
        if (connection == null) {
            return;
        }
        String sql = "UPDATE whitelist_requests SET status=?, handledBy=?, handledAt=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, handledBy);
            statement.setLong(3, System.currentTimeMillis());
            statement.setString(4, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to update request status: " + ex.getMessage());
        }
    }

    public RequestRow getRequestById(String id) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT userId, guildId, username, platform, status, handledBy FROM whitelist_requests WHERE id=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return new RequestRow(
                            set.getString("userId"),
                            set.getString("guildId"),
                            set.getString("username"),
                            set.getString("platform"),
                            set.getString("status"),
                            set.getString("handledBy")
                    );
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch request row: " + ex.getMessage());
        }
        return null;
    }

    public record RequestRow(String userId, String guildId, String username, String platform, String status, String handledBy) {}

    public WhitelistedPlayerRow getActiveWhitelistedByUsername(String username) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT id, userId, guildId, username, platform, requestId FROM whitelisted_players WHERE LOWER(username)=LOWER(?) AND status='active' LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return new WhitelistedPlayerRow(
                            set.getInt("id"),
                            set.getString("userId"),
                            set.getString("guildId"),
                            set.getString("username"),
                            set.getString("platform"),
                            set.getString("requestId")
                    );
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch active whitelist entry: " + ex.getMessage());
        }
        return null;
    }

    public WhitelistedPlayerRow getActiveWhitelistedByUserId(String userId) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT id, userId, guildId, username, platform, requestId FROM whitelisted_players WHERE userId=? AND status='active' LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return new WhitelistedPlayerRow(
                            set.getInt("id"),
                            set.getString("userId"),
                            set.getString("guildId"),
                            set.getString("username"),
                            set.getString("platform"),
                            set.getString("requestId")
                    );
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch active whitelist entry by userId: " + ex.getMessage());
        }
        return null;
    }

    public void revokeWhitelistedPlayer(int id) {
        if (connection == null) {
            return;
        }
        String sql = "UPDATE whitelisted_players SET status='removed', removedAt=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to revoke whitelisted player: " + ex.getMessage());
        }
    }

    public record WhitelistedPlayerRow(int id, String userId, String guildId, String username, String platform, String requestId) {}

    public void addWhitelistedPlayer(String userId, String guildId, String username, String platform, String requestId) {
        if (connection == null) {
            return;
        }
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement activeCheck = connection.prepareStatement("SELECT id, username FROM whitelisted_players WHERE userId=? AND status='active' LIMIT 1")) {
                activeCheck.setString(1, userId);
                try (ResultSet rs = activeCheck.executeQuery()) {
                    if (rs.next()) {
                        String existingUsername = rs.getString("username");
                        if (!existingUsername.equalsIgnoreCase(username)) {
                            try (PreparedStatement update = connection.prepareStatement("UPDATE whitelisted_players SET status='removed', removedAt=? WHERE id=?")) {
                                update.setLong(1, System.currentTimeMillis());
                                update.setInt(2, rs.getInt("id"));
                                update.executeUpdate();
                            }
                        } else {
                            connection.commit();
                            return;
                        }
                    }
                }
            }
            try (PreparedStatement existingUsername = connection.prepareStatement("SELECT id FROM whitelisted_players WHERE LOWER(username)=LOWER(?) AND status='active' LIMIT 1")) {
                existingUsername.setString(1, username);
                try (ResultSet rs = existingUsername.executeQuery()) {
                    if (rs.next()) {
                        connection.commit();
                        return;
                    }
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO whitelisted_players(userId,guildId,username,platform,status,addedAt,requestId) VALUES (?,?,?,?, 'active', ?, ?)")) {
                insert.setString(1, userId);
                insert.setString(2, guildId);
                insert.setString(3, username);
                insert.setString(4, platform);
                insert.setLong(5, System.currentTimeMillis());
                insert.setString(6, requestId);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to add whitelisted player: " + ex.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public void setLogMessageId(String requestId, String messageId) {
        if (connection == null) {
            return;
        }
        String sql = "UPDATE whitelist_requests SET logMessageId=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, messageId);
            statement.setString(2, requestId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to update log message id: " + ex.getMessage());
        }
    }

    public void setQueueMessageId(String requestId, String messageId) {
        if (connection == null) {
            return;
        }
        String sql = "UPDATE whitelist_requests SET queueMessageId=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, messageId);
            statement.setString(2, requestId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to update queue message id: " + ex.getMessage());
        }
    }

    public String getLogMessageId(String requestId) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT logMessageId FROM whitelist_requests WHERE id=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return set.getString("logMessageId");
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch log message id: " + ex.getMessage());
        }
        return null;
    }

    public String getQueueMessageId(String requestId) {
        if (connection == null) {
            return null;
        }
        String sql = "SELECT queueMessageId FROM whitelist_requests WHERE id=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    return set.getString("queueMessageId");
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to fetch queue message id: " + ex.getMessage());
        }
        return null;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}