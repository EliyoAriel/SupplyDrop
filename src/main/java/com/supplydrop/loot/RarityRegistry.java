package com.supplydrop.loot;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for dynamically loaded rarity tiers from config.
 */
public class RarityRegistry {

    private static final Map<String, Rarity> tiers = new LinkedHashMap<>();
    private static final List<Rarity> orderedTiers = new ArrayList<>();

    private RarityRegistry() {}

    /**
     * Load rarity tiers from the "rarities" config section.
     */
    public static void load(ConfigurationSection section) {
        tiers.clear();
        orderedTiers.clear();

        if (section == null) {
            loadDefaults();
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection tier = section.getConfigurationSection(key);
            if (tier == null) continue;

            int weight = Math.max(1, tier.getInt("weight", 10));
            String colorName = tier.getString("color", "WHITE");
            String prefix = tier.getString("prefix", "&f");

            ChatColor color;
            try {
                color = ChatColor.valueOf(colorName.toUpperCase());
            } catch (IllegalArgumentException e) {
                color = ChatColor.WHITE;
            }

            Rarity rarity = new Rarity(key, weight, color, prefix);
            tiers.put(key.toLowerCase(), rarity);
            orderedTiers.add(rarity);
        }

        if (tiers.isEmpty()) {
            loadDefaults();
        }
    }

    private static void loadDefaults() {
        tiers.clear();
        orderedTiers.clear();
        register("common", 60, ChatColor.GRAY, "&7");
        register("uncommon", 25, ChatColor.GREEN, "&a");
        register("rare", 10, ChatColor.BLUE, "&9");
        register("legendary", 5, ChatColor.GOLD, "&6");
    }

    private static void register(String key, int weight, ChatColor color, String prefix) {
        Rarity rarity = new Rarity(key, weight, color, prefix);
        tiers.put(key.toLowerCase(), rarity);
        orderedTiers.add(rarity);
    }

    /**
     * Get a rarity by key (case-insensitive). Returns null if not found.
     */
    public static Rarity get(String key) {
        if (key == null) return null;
        return tiers.get(key.toLowerCase());
    }

    /**
     * Get all registered rarity tiers in order.
     */
    public static List<Rarity> getAll() {
        return Collections.unmodifiableList(orderedTiers);
    }

    public static void clear() {
        tiers.clear();
        orderedTiers.clear();
    }
}
