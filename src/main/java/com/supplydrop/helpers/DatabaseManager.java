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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Async SQLite persistence for crate data.
 * All write operations are queued and processed on a background thread.
 * Read operations run synchronously (only called on startup).
 */
public class DatabaseManager {

    private static final String DB_FILE = "crates.db";
    private static final String TABLE = "crates";

    private static File dbFile;
    private static Connection connection;
    private static final BlockingQueue<Runnable> writeQueue = new LinkedBlockingQueue<>();
    private static Thread asyncThread;
    private static volatile boolean running = false;

    private DatabaseManager() {}

    /**
     * Initialize the database connection and schema.
     * Must be called on main thread during plugin enable.
     */
    public static void init(File dataFolder) {
        dbFile = new File(dataFolder, DB_FILE);
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            connection.setAutoCommit(true);
            ensureSchema();
        } catch (SQLException e) {
            AirdropLogger.severe("Failed to initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
        }

        // Start async writer thread
        running = true;
        asyncThread = new Thread(DatabaseManager::processQueue, "SupplyDrop-DB-Writer");
        asyncThread.setDaemon(true);
        asyncThread.start();

        AirdropLogger.info("Database initialized: " + dbFile.getAbsolutePath());
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
        // Flush remaining queue items
        drainQueue();
        closeConnection();
    }

    // ─── ASYNC WRITE OPERATIONS ─────────────────────────────────────

    /**
     * Save a crate to the database (async).
     */
    public static void save(Location location, String displayName, boolean isTrap,
                            boolean isTeamCrate, int requiredPlayers, String uuid) {
        writeQueue.offer(() -> doSave(location, displayName, isTrap, isTeamCrate, requiredPlayers, uuid));
    }

    /**
     * Remove a crate from the database (async).
     */
    public static void remove(Location location) {
        writeQueue.offer(() -> doRemove(location));
    }

    /**
     * Remove a crate by UUID (async).
     */
    public static void removeById(String uuid) {
        writeQueue.offer(() -> doRemoveById(uuid));
    }

    /**
     * Remove all crates from the database (async).
     */
    public static void removeAll() {
        writeQueue.offer(() -> doRemoveAll());
    }

    // ─── SYNC READ OPERATIONS (startup only) ────────────────────────

    /**
     * Load all saved crates from the database.
     * Returns list of CrateRecord objects.
     */
    public static synchronized List<CrateRecord> loadAll() {
        List<CrateRecord> records = new ArrayList<>();
        if (connection == null) return records;

        String sql = "SELECT * FROM " + TABLE;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String uuidStr = rs.getString("uuid");
                java.util.UUID uuid = (uuidStr != null && !uuidStr.isEmpty())
                        ? java.util.UUID.fromString(uuidStr) : java.util.UUID.randomUUID();
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                String displayName = rs.getString("display_name");
                boolean isTrap = rs.getInt("is_trap") == 1;
                boolean isTeamCrate = rs.getInt("is_team_crate") == 1;
                int requiredPlayers = rs.getInt("required_players");

                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                Location loc = new Location(world, x, y, z);
                records.add(new CrateRecord(uuid, loc, displayName, isTrap, isTeamCrate, requiredPlayers));
            }
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to load crates from database: " + e.getMessage());
        }
        return records;
    }

    /**
     * Get the number of active crates in the database.
     */
    public static synchronized int getCount() {
        if (connection == null) return 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + TABLE)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    // ─── MIGRATION ──────────────────────────────────────────────────

    /**
     * Migrate data from old crates.yml to SQLite.
     * Renames crates.yml to crates.yml.bak after migration.
     */
    public static void migrateFromYaml(File dataFolder) {
        File yamlFile = new File(dataFolder, "crates.yml");
        if (!yamlFile.exists()) return;

        AirdropLogger.info("Migrating crate data from crates.yml to SQLite...");

        org.bukkit.configuration.file.YamlConfiguration yaml =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(yamlFile);

        if (!yaml.contains("crates")) {
            yamlFile.renameTo(new File(dataFolder, "crates.yml.bak"));
            AirdropLogger.info("crates.yml was empty, renamed to .bak");
            return;
        }

        var keys = yaml.getConfigurationSection("crates").getKeys(false);
        int migrated = 0;

        for (String key : keys) {
            String path = "crates." + key;
            String worldName = yaml.getString(path + ".world");
            int x = yaml.getInt(path + ".x");
            int y = yaml.getInt(path + ".y");
            int z = yaml.getInt(path + ".z");
            String displayName = yaml.getString(path + ".display-name");
            boolean isTrap = yaml.getBoolean(path + ".trap");
            boolean isTeamCrate = yaml.getBoolean(path + ".team-crate");
            int teamPlayers = yaml.getInt(path + ".team-players", 2);

            if (worldName == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Location loc = new Location(world, x, y, z);
            doSave(loc, displayName, isTrap, isTeamCrate, teamPlayers, java.util.UUID.randomUUID().toString());
            migrated++;
        }

        yamlFile.renameTo(new File(dataFolder, "crates.yml.bak"));
        AirdropLogger.info("Migrated " + migrated + " crate(s) from crates.yml to SQLite.");
    }

    // ─── INTERNAL METHODS ───────────────────────────────────────────

    private static void ensureSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "uuid TEXT NOT NULL, "
                + "world TEXT NOT NULL, "
                + "x INTEGER NOT NULL, "
                + "y INTEGER NOT NULL, "
                + "z INTEGER NOT NULL, "
                + "display_name TEXT, "
                + "is_trap INTEGER DEFAULT 0, "
                + "is_team_crate INTEGER DEFAULT 0, "
                + "required_players INTEGER DEFAULT 2, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "UNIQUE(world, x, y, z)"
                + ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        // Migration: add uuid column if missing from existing DB
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + TABLE + ")")) {
            boolean hasUuid = false;
            while (rs.next()) {
                if ("uuid".equals(rs.getString("name"))) {
                    hasUuid = true;
                    break;
                }
            }
            if (!hasUuid) {
                stmt.execute("ALTER TABLE " + TABLE + " ADD COLUMN uuid TEXT NOT NULL DEFAULT ''");
                AirdropLogger.info("Added uuid column to " + TABLE + " table.");
            }
        }
    }

    private static void doSave(Location location, String displayName, boolean isTrap,
                               boolean isTeamCrate, int requiredPlayers, String uuid) {
        if (connection == null) return;
        String sql = "INSERT OR REPLACE INTO " + TABLE
                + " (uuid, world, x, y, z, display_name, is_trap, is_team_crate, required_players) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, location.getWorld().getName());
            ps.setInt(3, location.getBlockX());
            ps.setInt(4, location.getBlockY());
            ps.setInt(5, location.getBlockZ());
            ps.setString(6, displayName);
            ps.setInt(7, isTrap ? 1 : 0);
            ps.setInt(8, isTeamCrate ? 1 : 0);
            ps.setInt(9, requiredPlayers);
            ps.executeUpdate();
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to save crate to database: " + e.getMessage());
        }
    }

    private static void doRemove(Location location) {
        if (connection == null) return;
        String sql = "DELETE FROM " + TABLE
                + " WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, location.getWorld().getName());
            ps.setInt(2, location.getBlockX());
            ps.setInt(3, location.getBlockY());
            ps.setInt(4, location.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to remove crate from database: " + e.getMessage());
        }
    }

    private static void doRemoveById(String uuid) {
        if (connection == null) return;
        String sql = "DELETE FROM " + TABLE + " WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to remove crate by ID from database: " + e.getMessage());
        }
    }

    private static void doRemoveAll() {
        if (connection == null) return;
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM " + TABLE);
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to remove all crates from database: " + e.getMessage());
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
                AirdropLogger.warning("Failed to close database connection: " + e.getMessage());
            }
        }
    }

    /**
     * Record holding persisted crate data.
     */
    public record CrateRecord(java.util.UUID uuid, Location location, String displayName, boolean isTrap,
                              boolean isTeamCrate, int requiredPlayers) {}
}
