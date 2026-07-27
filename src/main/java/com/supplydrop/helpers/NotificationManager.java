package com.supplydrop.helpers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationManager {

    private static final String TABLE = "subscriptions";
    private static final Set<UUID> subscribedPlayers = ConcurrentHashMap.newKeySet();
    private static boolean defaultSubscribe = true;

    public static void init(Connection connection, boolean defaultSub) {
        defaultSubscribe = defaultSub;
        subscribedPlayers.clear();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "subscribed INTEGER NOT NULL DEFAULT 1"
                    + ")");
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to create subscriptions table: " + e.getMessage());
            return;
        }

        try (ResultSet rs = connection.createStatement().executeQuery(
                "SELECT uuid, subscribed FROM " + TABLE)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                boolean sub = rs.getInt("subscribed") == 1;
                if (sub) {
                    subscribedPlayers.add(uuid);
                }
            }
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to load subscriptions: " + e.getMessage());
        }
    }

    public static boolean isSubscribed(UUID uuid) {
        return subscribedPlayers.contains(uuid);
    }

    public static void setSubscribed(Connection connection, UUID uuid, boolean subscribed) {
        if (subscribed) {
            subscribedPlayers.add(uuid);
        } else {
            subscribedPlayers.remove(uuid);
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO " + TABLE + " (uuid, subscribed) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, subscribed ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            AirdropLogger.warning("Failed to save subscription: " + e.getMessage());
        }
    }

    /**
     * Notify players: subscribed across all worlds + all players in the same world.
     */
    public static void notify(String worldName, String message) {
        String formatted = format(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(worldName) || subscribedPlayers.contains(player.getUniqueId())) {
                player.sendMessage(formatted);
            }
        }
    }

    /**
     * Notify a single player only (for subscribe/unsubscribe confirmations).
     */
    public static void notifyPlayer(Player player, String message) {
        player.sendMessage(format(message));
    }

    private static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
