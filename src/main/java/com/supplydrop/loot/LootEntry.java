package com.supplydrop.loot;

import org.bukkit.inventory.ItemStack;

/**
 * A single entry in a loot table: an item with a rarity and weight.
 */
public class LootEntry {

    private final ItemStack item;
    private final Rarity rarity;
    private int weight;

    public LootEntry(ItemStack item, Rarity rarity, int weight) {
        this.item = item.clone();
        this.rarity = rarity;
        this.weight = Math.max(1, weight);
    }

    public ItemStack getItem() { return item.clone(); }
    public Rarity getRarity() { return rarity; }
    public int getWeight() { return weight; }

    public void setWeight(int weight) {
        this.weight = Math.max(1, weight);
    }

    /**
     * Effective weight = item weight × rarity tier weight.
     * Used in the weighted pool roll system.
     */
    public int getEffectiveWeight() {
        return weight * rarity.weight();
    }

    @Override
    public String toString() {
        return rarity.prefix() + item.getType().name() + " x" + item.getAmount();
    }
}
