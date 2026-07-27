package com.supplydrop.helpers;

import com.supplydrop.SupplyDrop;
import com.supplydrop.config.ConfigKeys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Logger;

public class ChatHandler {

    private ChatHandler() {}

    private static SupplyDrop plugin;

    public static void init(SupplyDrop pluginInstance) {
        plugin = pluginInstance;
    }

    private static Logger getLogger() {
        return plugin != null ? plugin.getLogger() : Logger.getLogger("SupplyDrop");
    }

    private static String getPrefix() {
        return ChatTheme.primary() + "[" + ChatTheme.text() + "SupplyDrop" + ChatTheme.primary() + "]";
    }

    private static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) return message;
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static String replaceThemePlaceholders(String message) {
        return message
                .replace("{primary}", ChatTheme.primary().toString())
                .replace("{text}", ChatTheme.text().toString())
                .replace("{accent}", ChatTheme.accent().toString())
                .replace("{success}", ChatTheme.success().toString())
                .replace("{warning}", ChatTheme.warning().toString())
                .replace("{error}", ChatTheme.error().toString())
                .replace("{error-detail}", ChatTheme.errorDetail().toString());
    }

    public static String formatMessage(String message, Map<String, String> placeholders) {
        String result = replacePlaceholders(message, placeholders);
        result = replaceThemePlaceholders(result);
        return format(result);
    }

    public static void send(CommandSender sender, String message) {
        String formatted = format(getPrefix() + ChatTheme.primary() + " " + replaceThemePlaceholders(message));
        if (sender instanceof Player) {
            sender.sendMessage(formatted);
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage(formatted);
        }
    }

    public static void send(CommandSender sender, String message, Map<String, String> placeholders) {
        String formatted = format(getPrefix() + ChatTheme.primary() + " " + formatMessage(message, placeholders));
        if (sender instanceof Player) {
            sender.sendMessage(formatted);
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage(formatted);
        }
    }

    public static void sendError(CommandSender sender, String message) {
        String formatted = format(getPrefix() + ChatTheme.error() + " " + replaceThemePlaceholders(message));
        if (sender instanceof Player) {
            sender.sendMessage(formatted);
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage(formatted);
        }
    }

    public static void sendError(CommandSender sender, String message, Map<String, String> placeholders) {
        String formatted = format(getPrefix() + ChatTheme.error() + " " + formatMessage(message, placeholders));
        if (sender instanceof Player) {
            sender.sendMessage(formatted);
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage(formatted);
        }
    }

    public static void broadcast(String message) {
        String formatted = format(getPrefix() + ChatTheme.primary() + " " + replaceThemePlaceholders(message));
        Bukkit.getServer().broadcastMessage(formatted);
    }

    public static void broadcast(String message, Map<String, String> placeholders) {
        String formatted = format(getPrefix() + ChatTheme.primary() + " " + formatMessage(message, placeholders));
        Bukkit.getServer().broadcastMessage(formatted);
    }

    public static void sendWithoutPrefix(CommandSender sender, String message) {
        String formatted = format(replaceThemePlaceholders(message));
        if (sender instanceof Player) {
            sender.sendMessage(formatted);
        } else {
            Bukkit.getServer().getConsoleSender().sendMessage(formatted);
        }
    }

    public static void logMessage(String message) {
        getLogger().info(message);
    }

    public static void debug(String message) {
        if (ConfigKeys.isDebugLoggingEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
