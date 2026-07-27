package com.supplydrop.packages;

import com.supplydrop.PackagesConfig;
import com.supplydrop.SupplyDrop;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.loot.LootConfigManager;
import com.supplydrop.loot.LootTable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages package loot table templates.
 */
public class PackageManager {

    private static final LootConfigManager lootManager = new LootConfigManager();
    private static final ConcurrentHashMap<String, Package> packages = new ConcurrentHashMap<>();

    private PackageManager() {}

    public static boolean reload() {
        PackagesConfig pc = SupplyDrop.getPackagesConfiguration();
        if (pc == null) {
            AirdropLogger.warning("Skipping package reload: packages configuration unavailable");
            packages.clear();
            return false;
        }

        AirdropLogger.debug("Reloading loot tables from packages.yml");
        pc.reloadConfig();
        lootManager.load(pc.getConfig());

        packages.clear();
        for (String name : lootManager.getTableNames()) {
            LootTable table = lootManager.getTable(name);
            if (table != null) {
                String displayName = lootManager.getDisplayName(name);
                packages.put(name, new Package(name, table, displayName));
            }
        }

        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin != null && plugin.isEnabled()) {
            plugin.setupPackageGuis();
        }

        AirdropLogger.debug("Loaded " + packages.size() + " loot table template(s)");
        return true;
    }

    public static Set<String> getPackageNames() {
        return Collections.unmodifiableSet(packages.keySet());
    }

    public static Package get(String name) {
        return packages.get(name);
    }

    public static boolean has(String name) {
        return packages.containsKey(name);
    }

    public static void createPackage(String name) {
        lootManager.createTable(name);
        LootTable table = lootManager.getTable(name);
        if (table != null) {
            packages.put(name, new Package(name, table));
        }
        saveConfig();
    }

    public static void setDisplayName(String name, String displayName) {
        lootManager.setDisplayName(name, displayName);
        Package pkg = packages.get(name);
        if (pkg != null) {
            pkg.setDisplayName(displayName);
        }
        saveConfig();
    }

    public static void deletePackage(String name) {
        lootManager.deleteTable(name);
        packages.remove(name);
        saveConfig();
    }

    public static LootConfigManager getLootManager() {
        return lootManager;
    }

    public static void saveConfig() {
        lootManager.save();
        PackagesConfig pc = SupplyDrop.getPackagesConfiguration();
        if (pc != null) {
            pc.saveConfig();
        }
    }

    public static void clear() {
        packages.clear();
    }
}
