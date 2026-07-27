package com.supplydrop;

import com.supplydrop.helpers.ChatHandler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PackagesConfig {

    private final SupplyDrop plugin;
    private FileConfiguration config;
    private File configFile;

    public PackagesConfig(SupplyDrop plugin) {
        this.plugin = plugin;
        configFile = new File(plugin.getDataFolder(), "packages.yml");
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        reloadConfig();
    }

    private void createDefaultConfig() {
        plugin.getDataFolder().mkdirs();
        config = new YamlConfiguration();
        writeDefaults();
        saveConfig();
        ChatHandler.logMessage("Packages configuration created with default loot tables.");
    }

    private void writeDefaults() {
        // Weapons template
        List<Map<String, Object>> weaponsCommon = new ArrayList<>();
        weaponsCommon.add(item("WOODEN_SWORD", 1, 20));
        weaponsCommon.add(item("WOODEN_AXE", 1, 15));
        weaponsCommon.add(item("BOW", 1, 10));
        weaponsCommon.add(item("ARROW", 16, 25));
        config.set("packages.weapons.common", weaponsCommon);

        List<Map<String, Object>> weaponsUncommon = new ArrayList<>();
        weaponsUncommon.add(item("STONE_SWORD", 1, 15));
        weaponsUncommon.add(item("STONE_AXE", 1, 12));
        weaponsUncommon.add(item("CROSSBOW", 1, 10));
        weaponsUncommon.add(item("TRIDENT", 1, 5));
        config.set("packages.weapons.uncommon", weaponsUncommon);

        List<Map<String, Object>> weaponsRare = new ArrayList<>();
        weaponsRare.add(item("IRON_SWORD", 1, 10, "&7Rusty Blade", List.of("&7A worn but functional sword")));
        weaponsRare.add(item("IRON_AXE", 1, 8));
        weaponsRare.add(item("DIAMOND_SWORD", 1, 5, "&9Crystal Edge", List.of("&9Forged in the depths", "&9Damage: &f+10"), Map.of("SHARPNESS", 3, "FIRE_ASPECT", 1)));
        weaponsRare.add(item("END_ROD", 4, 12));
        config.set("packages.weapons.rare", weaponsRare);

        List<Map<String, Object>> weaponsLegendary = new ArrayList<>();
        weaponsLegendary.add(item("DIAMOND_AXE", 1, 3, "&6Warhammer", List.of("&6Crushes all in its path"), Map.of("SHARPNESS", 5, "UNBREAKING", 3)));
        weaponsLegendary.add(item("NETHERITE_SWORD", 1, 2, "&6Excalibur", List.of("&6The legendary blade", "", "&d&l⚠ LEGENDARY"), Map.of("SHARPNESS", 5, "FIRE_ASPECT", 2, "SWEEPING_EDGE", 3, "UNBREAKING", 3, "MENDING", 1), true));
        weaponsLegendary.add(item("NETHERITE_AXE", 1, 2, "&6Executioner", List.of("&6One swing is all it takes"), Map.of("SHARPNESS", 5, "UNBREAKING", 3, "MENDING", 1)));
        weaponsLegendary.add(item("TRIDENT", 1, 3, "&6Poseidon's Wrath", List.of("&6Commands the seas"), Map.of("LOYALTY", 3, "IMPALING", 5)));
        config.set("packages.weapons.legendary", weaponsLegendary);

        // Armor template
        List<Map<String, Object>> armorCommon = new ArrayList<>();
        armorCommon.add(item("LEATHER_HELMET", 1, 15));
        armorCommon.add(item("LEATHER_CHESTPLATE", 1, 15));
        armorCommon.add(item("LEATHER_LEGGINGS", 1, 15));
        armorCommon.add(item("LEATHER_BOOTS", 1, 15));
        config.set("packages.armor.common", armorCommon);

        List<Map<String, Object>> armorUncommon = new ArrayList<>();
        armorUncommon.add(item("CHAINMAIL_HELMET", 1, 12));
        armorUncommon.add(item("CHAINMAIL_CHESTPLATE", 1, 12));
        armorUncommon.add(item("CHAINMAIL_LEGGINGS", 1, 12));
        armorUncommon.add(item("CHAINMAIL_BOOTS", 1, 12));
        config.set("packages.armor.uncommon", armorUncommon);

        List<Map<String, Object>> armorRare = new ArrayList<>();
        armorRare.add(item("IRON_HELMET", 1, 10));
        armorRare.add(item("IRON_CHESTPLATE", 1, 10));
        armorRare.add(item("IRON_LEGGINGS", 1, 10));
        armorRare.add(item("IRON_BOOTS", 1, 10));
        config.set("packages.armor.rare", armorRare);

        List<Map<String, Object>> armorLegendary = new ArrayList<>();
        armorLegendary.add(item("DIAMOND_HELMET", 1, 5, "&6Diamond Crown", List.of("&6Shines with power"), Map.of("PROTECTION", 4, "UNBREAKING", 3)));
        armorLegendary.add(item("DIAMOND_CHESTPLATE", 1, 5, "&6Dragon Chestplate", List.of("&6Forged from dragon scales"), Map.of("PROTECTION", 4, "UNBREAKING", 3, "MENDING", 1)));
        armorLegendary.add(item("DIAMOND_LEGGINGS", 1, 5));
        armorLegendary.add(item("DIAMOND_BOOTS", 1, 5));
        armorLegendary.add(item("NETHERITE_HELMET", 1, 2, "&6Wither's Visage", List.of("&6Grants wither resistance"), Map.of("PROTECTION", 4, "UNBREAKING", 3, "MENDING", 1)));
        armorLegendary.add(item("NETHERITE_CHESTPLATE", 1, 2, "&6Infernal Plate", List.of("&6Burns those who strike"), Map.of("PROTECTION", 4, "FIRE_PROTECTION", 4, "UNBREAKING", 3, "MENDING", 1), true));
        config.set("packages.armor.legendary", armorLegendary);

        // Supplies template
        List<Map<String, Object>> suppliesCommon = new ArrayList<>();
        suppliesCommon.add(item("BREAD", 16, 25));
        suppliesCommon.add(item("COOKED_BEEF", 8, 20));
        suppliesCommon.add(item("GOLDEN_APPLE", 1, 5));
        suppliesCommon.add(item("ARROW", 32, 30));
        config.set("packages.supplies.common", suppliesCommon);

        List<Map<String, Object>> suppliesUncommon = new ArrayList<>();
        suppliesUncommon.add(item("ENDER_PEARL", 4, 12));
        suppliesUncommon.add(item("EXPERIENCE_BOTTLE", 16, 15));
        suppliesUncommon.add(item("EMERALD", 8, 18));
        suppliesUncommon.add(item("DIAMOND", 2, 5));
        config.set("packages.supplies.uncommon", suppliesUncommon);

        List<Map<String, Object>> suppliesRare = new ArrayList<>();
        suppliesRare.add(item("ELYTRA", 1, 3));
        suppliesRare.add(item("SHULKER_BOX", 1, 5));
        suppliesRare.add(item("TOTEM_OF_UNDYING", 1, 4));
        suppliesRare.add(item("BEACON", 1, 5));
        config.set("packages.supplies.rare", suppliesRare);

        List<Map<String, Object>> suppliesLegendary = new ArrayList<>();
        suppliesLegendary.add(item("ENCHANTED_GOLDEN_APPLE", 4, 3, "&6Notch's Apple", List.of("&6The rarest food in existence")));
        suppliesLegendary.add(item("ELYTRA", 1, 2, "&6Dragon Wings", List.of("&6Soar through the skies"), Map.of("UNBREAKING", 3, "MENDING", 1)));
        suppliesLegendary.add(item("TOTEM_OF_UNDYING", 2, 2));
        suppliesLegendary.add(item("NETHER_STAR", 1, 1, "&6Celestial Core", List.of("&6Pulse with otherworldly energy")));
        config.set("packages.supplies.legendary", suppliesLegendary);
    }

    private Map<String, Object> item(String material, int amount, int weight) {
        Map<String, Object> map = new HashMap<>();
        map.put("material", material);
        map.put("amount", amount);
        map.put("weight", weight);
        return map;
    }

    private Map<String, Object> item(String material, int amount, int weight, String name, List<String> lore) {
        Map<String, Object> map = item(material, amount, weight);
        map.put("name", name);
        map.put("lore", lore);
        return map;
    }

    private Map<String, Object> item(String material, int amount, int weight, String name, List<String> lore, Map<String, Integer> enchants) {
        Map<String, Object> map = item(material, amount, weight, name, lore);
        map.put("enchants", enchants);
        return map;
    }

    private Map<String, Object> item(String material, int amount, int weight, String name, List<String> lore, Map<String, Integer> enchants, boolean unbreakable) {
        Map<String, Object> map = item(material, amount, weight, name, lore, enchants);
        map.put("unbreakable", true);
        return map;
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save packages config", e);
        }
    }
}
