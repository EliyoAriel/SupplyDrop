package com.supplydrop.packages;

import com.supplydrop.loot.LootTable;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * A package is a loot table template.
 * It wraps a LootTable and provides display/info methods.
 */
public class Package {

    private final String name;
    private final LootTable lootTable;
    private String displayName;
    private int fallDuration;
    private int lockDuration;

    public Package(String name, LootTable lootTable) {
        this(name, lootTable, null, 0, 0);
    }

    public Package(String name, LootTable lootTable, String displayName) {
        this(name, lootTable, displayName, 0, 0);
    }

    public Package(String name, LootTable lootTable, String displayName, int fallDuration) {
        this(name, lootTable, displayName, fallDuration, 0);
    }

    public Package(String name, LootTable lootTable, String displayName, int fallDuration, int lockDuration) {
        this.name = name;
        this.lootTable = lootTable;
        this.displayName = displayName;
        this.fallDuration = fallDuration;
        this.lockDuration = lockDuration;
    }

    public String getName() { return name; }
    public LootTable getLootTable() { return lootTable; }

    public String getDisplayName() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getFallDuration() {
        return fallDuration;
    }

    public void setFallDuration(int fallDuration) {
        this.fallDuration = fallDuration;
    }

    public int getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(int lockDuration) {
        this.lockDuration = lockDuration;
    }

    /**
     * Roll random items from this package's loot table.
     */
    public List<ItemStack> rollLoot(int minRolls, int maxRolls) {
        return lootTable.rollMultiple(minRolls, maxRolls);
    }

    /**
     * Get info about this package's loot table.
     */
    public String getInfo() {
        return lootTable.getInfo();
    }
}
