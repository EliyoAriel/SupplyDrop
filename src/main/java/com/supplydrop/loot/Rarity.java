package com.supplydrop.loot;

import org.bukkit.ChatColor;

/**
 * Represents a rarity tier for loot items.
 * Loaded dynamically from config — not a fixed enum.
 */
public record Rarity(String key, int weight, ChatColor chatColor, String prefix) {

    /**
     * Get the display name with rarity color applied.
     */
    public String formatName(String name) {
        return prefix + name;
    }

    @Override
    public String toString() {
        return prefix + key.toUpperCase();
    }
}
