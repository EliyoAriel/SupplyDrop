package com.supplydrop.helpers;

import com.supplydrop.Config;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;

public final class ChatTheme {

    private static Config config;

    private ChatTheme() {}

    public static void init(Config configInstance) {
        config = configInstance;
    }

    public static ChatColor primary() {
        return getColor("primary", ChatColor.BLUE);
    }

    public static ChatColor text() {
        return getColor("text", ChatColor.WHITE);
    }

    public static ChatColor accent() {
        return getColor("accent", ChatColor.AQUA);
    }

    public static ChatColor success() {
        return getColor("success", ChatColor.GREEN);
    }

    public static ChatColor warning() {
        return getColor("warning", ChatColor.YELLOW);
    }

    public static ChatColor error() {
        return getColor("error", ChatColor.RED);
    }

    public static ChatColor errorDetail() {
        return getColor("error-detail", ChatColor.DARK_RED);
    }

    private static ChatColor getColor(String key, ChatColor fallback) {
        if (config == null) return fallback;
        FileConfiguration fc = config.getConfig();
        if (fc == null) return fallback;
        String value = fc.getString("ui.chat.colors." + key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return ChatColor.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
