package com.supplydrop.loot;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A loot table that holds items grouped by rarity.
 * Uses weighted pool rolling: each item's effective chance = itemWeight × rarityWeight.
 */
public class LootTable {

    private final String name;
    private final List<LootEntry> entries;
    private final Random random;

    public LootTable(String name) {
        this.name = name;
        this.entries = new ArrayList<>();
        this.random = new Random();
    }

    public void addEntry(LootEntry entry) {
        entries.add(entry);
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
        }
    }

    public List<LootEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public String getName() { return name; }
    public int size() { return entries.size(); }

    /**
     * Roll a single random item from this loot table.
     * Uses effective weight (item weight × rarity weight).
     */
    public LootEntry rollSingle() {
        if (entries.isEmpty()) return null;

        int totalWeight = entries.stream().mapToInt(LootEntry::getEffectiveWeight).sum();
        if (totalWeight <= 0) return entries.get(random.nextInt(entries.size()));

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (LootEntry entry : entries) {
            cumulative += entry.getEffectiveWeight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    /**
     * Roll multiple random items, respecting min/max roll count.
     * The same entry CAN appear multiple times.
     */
    public List<ItemStack> rollMultiple(int minRolls, int maxRolls) {
        int rollCount = minRolls + random.nextInt(Math.max(1, maxRolls - minRolls + 1));
        List<ItemStack> results = new ArrayList<>();

        for (int i = 0; i < rollCount; i++) {
            LootEntry entry = rollSingle();
            if (entry != null) {
                results.add(entry.getItem());
            }
        }
        return results;
    }

    /**
     * Get entries filtered by rarity tier.
     */
    public List<LootEntry> getEntriesByRarity(Rarity rarity) {
        List<LootEntry> filtered = new ArrayList<>();
        for (LootEntry entry : entries) {
            if (entry.getRarity().equals(rarity)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    /**
     * Get a formatted info string for this loot table.
     */
    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        for (Rarity rarity : RarityRegistry.getAll()) {
            List<LootEntry> rarityEntries = getEntriesByRarity(rarity);
            if (rarityEntries.isEmpty()) continue;
            sb.append(rarity.prefix()).append(rarity.key().toUpperCase()).append(": ");
            List<String> names = new ArrayList<>();
            for (LootEntry entry : rarityEntries) {
                names.add(entry.getItem().getType().name() + " x" + entry.getItem().getAmount());
            }
            sb.append(String.join(", ", names)).append("\n");
        }
        return sb.toString();
    }
}
