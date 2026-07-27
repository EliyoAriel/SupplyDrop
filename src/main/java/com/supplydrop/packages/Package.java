package com.supplydrop.packages;

import com.supplydrop.loot.LootEntry;
import com.supplydrop.loot.LootTable;
import com.supplydrop.loot.Rarity;
import com.supplydrop.loot.RarityRegistry;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A package is a loot table template.
 * It wraps a LootTable and provides display/info methods.
 */
public class Package {

    private final String name;
    private final LootTable lootTable;
    private String displayName;

    public Package(String name, LootTable lootTable) {
        this(name, lootTable, null);
    }

    public Package(String name, LootTable lootTable, String displayName) {
        this.name = name;
        this.lootTable = lootTable;
        this.displayName = displayName;
    }

    public String getName() { return name; }
    public LootTable getLootTable() { return lootTable; }

    public String getDisplayName() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean hasCustomDisplayName() {
        return displayName != null && !displayName.isEmpty();
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
        StringBuilder sb = new StringBuilder();
        for (Rarity rarity : RarityRegistry.getAll()) {
            List<LootEntry> entries = lootTable.getEntriesByRarity(rarity);
            if (entries.isEmpty()) continue;
            sb.append(rarity.prefix()).append(rarity.key().toUpperCase()).append(": ");
            List<String> names = new ArrayList<>();
            for (LootEntry entry : entries) {
                String itemName = entry.getItem().getType().name();
                int amount = entry.getItem().getAmount();
                names.add(itemName + " x" + amount);
            }
            sb.append(String.join(", ", names)).append("\n");
        }
        return sb.toString();
    }
}
