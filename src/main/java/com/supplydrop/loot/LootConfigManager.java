package com.supplydrop.loot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages loot table templates loaded from packages.yml.
 *
 * YAML structure:
 *   template-name:
 *     rarity-key:
 *       - material: DIAMOND_SWORD
 *         weight: 5
 *         name: "&9Crystal Edge"
 *         lore:
 *           - "&7Forged in the depths"
 *         enchants:
 *           SHARPNESS: 3
 *         unbreakable: true
 */
public class LootConfigManager {

    private final Map<String, LootTable> lootTables = new HashMap<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private final Map<String, Integer> fallDurations = new HashMap<>();
    private final Map<String, Integer> lockDurations = new HashMap<>();
    private FileConfiguration config;

    public void load(FileConfiguration config) {
        this.config = config;
        lootTables.clear();
        displayNames.clear();
        fallDurations.clear();
        lockDurations.clear();

        ConfigurationSection root = config.getConfigurationSection("packages");
        if (root == null) root = config; // fallback: treat root as templates

        for (String templateName : root.getKeys(false)) {
            ConfigurationSection templateSection = root.getConfigurationSection(templateName);
            if (templateSection == null) continue;

            // Read optional display-name
            String displayName = templateSection.getString("display-name");
            if (displayName != null && !displayName.isEmpty()) {
                displayNames.put(templateName, displayName);
            }

            // Read optional fall-duration
            int fallDuration = templateSection.getInt("fall-duration", 0);
            if (fallDuration > 0) {
                fallDurations.put(templateName, fallDuration);
            }

            // Read optional lock-duration
            int lockDuration = templateSection.getInt("lock-duration", 0);
            if (lockDuration > 0) {
                lockDurations.put(templateName, lockDuration);
            }

            LootTable table = new LootTable(templateName);

            // Each key under the template is a rarity tier
            for (String rarityKey : templateSection.getKeys(false)) {
                Rarity rarity = RarityRegistry.get(rarityKey);
                if (rarity == null) {
                    rarity = new Rarity(rarityKey, 10, org.bukkit.ChatColor.WHITE, "&f");
                }

                List<?> itemList = templateSection.getList(rarityKey);
                if (itemList == null) continue;

                for (Object obj : itemList) {
                    if (obj instanceof Map<?, ?> map) {
                        // New format: map with material, weight, name, lore, etc.
                        int itemWeight = getIntFromMap(map, "weight", 10);
                        ItemStack item = LootItemBuilder.build(map);
                        if (item != null) {
                            table.addEntry(new LootEntry(item, rarity, itemWeight));
                        }
                    } else if (obj instanceof ItemStack item) {
                        // Legacy format: raw ItemStack
                        table.addEntry(new LootEntry(item, rarity, rarity.weight()));
                    }
                }
            }

            lootTables.put(templateName, table);
        }
    }

    public LootTable getTable(String name) {
        return lootTables.get(name);
    }

    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(lootTables.keySet());
    }

    public String getDisplayName(String templateName) {
        return displayNames.get(templateName);
    }

    public int getFallDuration(String templateName) {
        return fallDurations.getOrDefault(templateName, 0);
    }

    public void setDisplayName(String templateName, String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            displayNames.remove(templateName);
        } else {
            displayNames.put(templateName, displayName);
        }
    }

    public void setFallDuration(String templateName, int fallDuration) {
        if (fallDuration <= 0) {
            fallDurations.remove(templateName);
        } else {
            fallDurations.put(templateName, fallDuration);
        }
    }

    public int getLockDuration(String templateName) {
        return lockDurations.getOrDefault(templateName, 0);
    }

    public void setLockDuration(String templateName, int lockDuration) {
        if (lockDuration <= 0) {
            lockDurations.remove(templateName);
        } else {
            lockDurations.put(templateName, lockDuration);
        }
    }

    public void createTable(String name) {
        if (lootTables.containsKey(name)) return;
        lootTables.put(name, new LootTable(name));
    }

    public void deleteTable(String name) {
        lootTables.remove(name);
        displayNames.remove(name);
    }

    /**
     * Save all loot tables back to config in the new nested format.
     */
    public void save() {
        if (config == null) return;

        // Determine root — if packages section exists, write there; otherwise create it
        ConfigurationSection root = config.getConfigurationSection("packages");
        if (root == null) {
            root = config.createSection("packages");
        }

        // Remove old templates
        for (String key : new ArrayList<>(root.getKeys(false))) {
            root.set(key, null);
        }

        // Write current templates
        for (Map.Entry<String, LootTable> entry : lootTables.entrySet()) {
            LootTable table = entry.getValue();
            ConfigurationSection templateSection = root.createSection(entry.getKey());

            // Write display-name if set
            String displayName = displayNames.get(entry.getKey());
            if (displayName != null && !displayName.isEmpty()) {
                templateSection.set("display-name", displayName);
            }

            // Write fall-duration if set (>0)
            int fallDur = fallDurations.getOrDefault(entry.getKey(), 0);
            if (fallDur > 0) {
                templateSection.set("fall-duration", fallDur);
            }

            // Write lock-duration if set (>0)
            int lockDur = lockDurations.getOrDefault(entry.getKey(), 0);
            if (lockDur > 0) {
                templateSection.set("lock-duration", lockDur);
            }

            for (Rarity rarity : RarityRegistry.getAll()) {
                List<LootEntry> rarityEntries = table.getEntriesByRarity(rarity);
                if (rarityEntries.isEmpty()) continue;

                List<Map<String, Object>> itemList = new ArrayList<>();
                for (LootEntry lootEntry : rarityEntries) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("material", lootEntry.getItem().getType().name());
                    itemMap.put("weight", lootEntry.getWeight());
                    itemMap.put("amount", lootEntry.getItem().getAmount());

                    ItemMetaHelper meta = new ItemMetaHelper(lootEntry.getItem());
                    if (meta.hasDisplayName()) itemMap.put("name", meta.getDisplayNameRaw());
                    if (meta.hasLore()) itemMap.put("lore", meta.getLoreRaw());
                    if (meta.hasEnchants()) itemMap.put("enchants", meta.getEnchantsMap());
                    if (meta.isUnbreakable()) itemMap.put("unbreakable", true);

                    itemList.add(itemMap);
                }
                templateSection.set(rarity.key(), itemList);
            }
        }
    }

    /**
     * Helper to extract display name, lore, enchants from an existing ItemStack for saving.
     */
    private static class ItemMetaHelper {
        private final org.bukkit.inventory.meta.ItemMeta meta;

        ItemMetaHelper(ItemStack item) {
            this.meta = item.getItemMeta();
        }

        boolean hasDisplayName() { return meta != null && meta.hasDisplayName(); }
        boolean hasLore() { return meta != null && meta.hasLore(); }
        boolean hasEnchants() { return meta != null && !meta.getEnchants().isEmpty(); }
        boolean isUnbreakable() { return meta != null && meta.isUnbreakable(); }

        String getDisplayNameRaw() {
            if (meta == null || !meta.hasDisplayName()) return null;
            // Strip color codes for storage
            return meta.getDisplayName().replace(org.bukkit.ChatColor.COLOR_CHAR, '&');
        }

        List<String> getLoreRaw() {
            if (meta == null || !meta.hasLore()) return null;
            List<String> raw = new ArrayList<>();
            for (String line : meta.getLore()) {
                raw.add(line.replace(org.bukkit.ChatColor.COLOR_CHAR, '&'));
            }
            return raw;
        }

        Map<String, Integer> getEnchantsMap() {
            if (meta == null) return null;
            Map<String, Integer> map = new HashMap<>();
            for (var entry : meta.getEnchants().entrySet()) {
                map.put(entry.getKey().getName(), entry.getValue());
            }
            return map;
        }
    }

    private static int getIntFromMap(Map<?, ?> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        return def;
    }
}
