package com.supplydrop.loot;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds a full ItemStack from a YAML configuration map.
 * Supports: material, amount, name, lore, enchants, unbreakable, flags, potion, model-data.
 */
public class LootItemBuilder {

    private LootItemBuilder() {}

    /**
     * Build an ItemStack from a map (as loaded from YAML).
     * Returns null if the map is invalid.
     */
    public static ItemStack build(Map<?, ?> map) {
        if (map == null) return null;

        // Material
        String materialName = getString(map, "material");
        if (materialName == null) return null;
        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) return null;

        int amount = getInt(map, "amount", 1);
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Display name
        String name = getString(map, "name");
        if (name != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }

        // Lore
        List<?> loreList = getList(map, "lore");
        if (loreList != null) {
            List<String> lore = new ArrayList<>();
            for (Object line : loreList) {
                lore.add(ChatColor.translateAlternateColorCodes('&', String.valueOf(line)));
            }
            meta.setLore(lore);
        }

        // Enchantments
        Map<?, ?> enchants = getMap(map, "enchants");
        if (enchants != null) {
            for (Object key : enchants.keySet()) {
                String enchantName = String.valueOf(key).toUpperCase();
                int level = getInt(enchants, String.valueOf(key), 1);
                Enchantment enchant = Enchantment.getByName(enchantName);
                if (enchant != null) {
                    meta.addEnchant(enchant, level, true);
                }
            }
        }

        // Unbreakable
        if (getBoolean(map, "unbreakable")) {
            meta.setUnbreakable(true);
        }

        // Custom model data
        int modelData = getInt(map, "model-data", 0);
        if (modelData > 0) {
            meta.setCustomModelData(modelData);
        }

        // Item flags
        List<?> flagsList = getList(map, "flags");
        if (flagsList != null) {
            for (Object flagName : flagsList) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(String.valueOf(flagName).toUpperCase());
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Potion effects
        Map<?, ?> potion = getMap(map, "potion");
        if (potion != null && meta instanceof PotionMeta potionMeta) {
            applyPotionEffects(potionMeta, potion);
        }

        item.setItemMeta(meta);
        return item;
    }

    private static void applyPotionEffects(PotionMeta meta, Map<?, ?> potion) {
        // Base potion type
        String typeName = getString(potion, "type");
        if (typeName != null) {
            try {
                PotionType potionType = PotionType.valueOf(typeName.toUpperCase());
                int level = getInt(potion, "level", 1);
                boolean extended = getBoolean(potion, "extended");
                meta.setBasePotionData(new PotionData(potionType, extended, level > 1));
            } catch (IllegalArgumentException ignored) {}
        }

        // Custom potion effects
        List<?> effects = getList(potion, "effects");
        if (effects != null) {
            for (Object obj : effects) {
                if (obj instanceof Map<?, ?> effectMap) {
                    String effectName = getString(effectMap, "type");
                    if (effectName == null) continue;
                    PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
                    if (type == null) continue;
                    int duration = getInt(effectMap, "duration", 200);
                    int amplifier = getInt(effectMap, "amplifier", 0);
                    boolean ambient = getBoolean(effectMap, "ambient");
                    boolean particles = !getBoolean(effectMap, "hide-particles");
                    meta.addCustomEffect(new PotionEffect(type, duration, amplifier, ambient, particles), true);
                }
            }
        }
    }

    // --- Helpers ---

    private static String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private static int getInt(Map<?, ?> map, String key, int def) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
        }
        return def;
    }

    private static boolean getBoolean(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }

    private static List<?> getList(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?> l) return l;
        return null;
    }

    private static Map<?, ?> getMap(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof Map<?, ?> m) return m;
        return null;
    }
}
