package com.supplydrop.helpers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;

/**
 * Async SQLite persistence for crate event history.
 * Separate database from crates.db.
 */
public class HistoryManager {

    private static final String DB_FILE = "history.db";
    private static final String TABLE = "history";

    private static Connection connection;
    private static final BlockingQueue<Runnable> writeQueue = new LinkedBlockingQueue<>();
    private static Thread asyncThread;
    private static volatile boolean running = false;

    private HistoryManager() {}

    public static Connection getConnection() {
        return connection;
    }

    /**
     * Initialize the history database.
     */
    public static void init(File dataFolder) {
        File dbFile = new File(dataFolder, DB_FILE);
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(true);
            ensureSchema();
        } catch (SQLException e) {
            AirdropLogger.severe("Failed to initialize history database: " + e.getMessage());
            e.printStackTrace();
        }

        running = true;
        asyncThread = new Thread(HistoryManager::processQueue, "SupplyDrop-History-Writer");
        asyncThread.setDaemon(true);
        asyncThread.start();

        AirdropLogger.info("History database initialized: " + dbFile.getAbsolutePath());
    }

    /**
     * Shutdown the async writer and close the connection.
     */
    public static void shutdown() {
        running = false;
        if (asyncThread != null) {
            asyncThread.interrupt();
            try {
                asyncThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        drainQueue();
        closeConnection();
    }

    // ─── ASYNC WRITE ──────────────────────────────────────────────

    /**
     * Log a crate event (async).
     */
    public static void logEvent(String event, String template, Location location, String player, String extra) {
        writeQueue.offer(() -> doLogEvent(event, template, location, player, extra));
    }

    // ─── SYNC READ ────────────────────────────────────────────────

    /**
     * Query history with pagination and optional filters.
     * @param page    page number (1-based)
     * @param perPage items per page
     * @param filterEvent  filter by event type (null = all)
     * @param filterPlayer filter by player name (null = all)
     * @return list of HistoryRecord
     */
    public static synchronized List<HistoryRecord> query(int page, int perPage,
                                                          String filterEvent, String filterPlayer) {
        List<HistoryRecord> records = new ArrayList<>();
        if (connection == null) return records;

        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (filterEvent != null && !filterEvent.isEmpty()) {
            sql.append(" AND event = ?");
            params.add(filterEvent.toUpperCase());
        }
        if (filterPlayer != null && !filterPlayer.isEmpty()) {
            sql.append(" AND player LIKE ?");
            params.add("%" + filterPlayer + "%");
        }

        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        int offset = (Math.max(1, page) - 1) * perPage;
        params.add(perPage);
        params.add(offset);

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new HistoryRecord(
                            rs.getInt("id"),
                            rs.getString("event"),
                            rs.getString("template"),
                            rs.getString("world"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z"),
                            rs.getString("player"),
                            rs.getString("extra"),
                            rs.getString("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to query history: " + e.getMessage());
        }
        return records;
    }

    /**
     * Count history entries with optional filters.
     */
    public static synchronized int count(String filterEvent, String filterPlayer) {
        if (connection == null) return 0;

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + TABLE + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (filterEvent != null && !filterEvent.isEmpty()) {
            sql.append(" AND event = ?");
            params.add(filterEvent.toUpperCase());
        }
        if (filterPlayer != null && !filterPlayer.isEmpty()) {
            sql.append(" AND player LIKE ?");
            params.add("%" + filterPlayer + "%");
        }

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /**
     * Get total history count (no filters).
     */
    public static synchronized int getTotalCount() {
        return count(null, null);
    }

    // ─── INTERNAL ─────────────────────────────────────────────────

    private static void ensureSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "event TEXT NOT NULL, "
                + "template TEXT, "
                + "world TEXT, "
                + "x INTEGER, "
                + "y INTEGER, "
                + "z INTEGER, "
                + "player TEXT, "
                + "extra TEXT, "
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static void doLogEvent(String event, String template, Location location, String player, String extra) {
        if (connection == null) return;
        String sql = "INSERT INTO " + TABLE
                + " (event, template, world, x, y, z, player, extra) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, event);
            ps.setString(2, template);
            if (location != null && location.getWorld() != null) {
                ps.setString(3, location.getWorld().getName());
                ps.setInt(4, location.getBlockX());
                ps.setInt(5, location.getBlockY());
                ps.setInt(6, location.getBlockZ());
            } else {
                ps.setString(3, null);
                ps.setNull(4, java.sql.Types.INTEGER);
                ps.setNull(5, java.sql.Types.INTEGER);
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.setString(7, player);
            ps.setString(8, extra);
            ps.executeUpdate();
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to log event: " + e.getMessage());
        }
    }

    private static void processQueue() {
        while (running) {
            try {
                Runnable task = writeQueue.poll(1, TimeUnit.SECONDS);
                if (task != null) {
                    task.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void drainQueue() {
        Runnable task;
        while ((task = writeQueue.poll()) != null) {
            task.run();
        }
    }

    private static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                AirdropLogger.warning("Failed to close history database: " + e.getMessage());
            }
        }
    }

    /**
     * Record holding history data.
     */
    public record HistoryRecord(int id, String event, String template, String world,
                                int x, int y, int z, String player, String extra, String timestamp) {}
}
