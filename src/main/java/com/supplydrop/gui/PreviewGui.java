package com.supplydrop.gui;

import com.supplydrop.SupplyDrop;
import com.supplydrop.loot.LootEntry;
import com.supplydrop.loot.LootTable;
import com.supplydrop.loot.Rarity;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Virtual chest GUI showing all items in a loot table template with pagination.
 */
public class PreviewGui implements InventoryHolder, Listener {

    private final Inventory inventory;
    private final String templateName;
    private final List<LootEntry> allEntries;
    private int currentPage = 0;
    private boolean listenerRegistered = false;

    private static final int SLOTS_PER_PAGE = 45; // 5 rows for items
    private static final int TOTAL_SLOTS = 54;    // 6 rows total
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int INFO_SLOT = 49;

    public PreviewGui(String templateName, LootTable lootTable) {
        this.templateName = templateName;
        this.allEntries = lootTable.getEntries();
        this.inventory = Bukkit.createInventory(this, TOTAL_SLOTS, buildTitle(0));
        renderPage(0);
    }

    private String buildTitle(int page) {
        return ChatColor.translateAlternateColorCodes('&',
                "&b&l" + templateName + " &7- Page " + (page + 1));
    }

    private void renderPage(int page) {
        this.currentPage = page;
        inventory.clear();

        int totalPages = getTotalPages();
        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, allEntries.size());

        for (int i = start; i < end; i++) {
            int slot = i - start;
            LootEntry entry = allEntries.get(i);

            ItemStack item = entry.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                Rarity rarity = entry.getRarity();

                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        rarity.prefix() + "&l" + rarity.key().toUpperCase()));
                lore.add(ChatColor.GRAY + "Weight: " + ChatColor.WHITE + entry.getWeight());
                lore.add(ChatColor.GRAY + "Effective: " + ChatColor.WHITE + entry.getEffectiveWeight());
                lore.add(ChatColor.GRAY + "Item " + (i + 1) + "/" + allEntries.size());

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
        }

        // Fill empty item slots with glass
        for (int i = end - start; i < SLOTS_PER_PAGE; i++) {
            inventory.setItem(i, createGlassPane());
        }

        // Navigation bar (bottom row)
        // Prev button
        if (page > 0) {
            inventory.setItem(PREV_SLOT, createNavButton(Material.ARROW,
                    "&aPrevious Page", "&7Page " + page + "/" + totalPages));
        }

        // Info
        inventory.setItem(INFO_SLOT, createNavButton(Material.PAPER,
                "&b" + templateName,
                "&7Page &f" + (page + 1) + "&7/&f" + totalPages,
                "&7Items: &f" + allEntries.size()));

        // Next button
        if (page < totalPages - 1) {
            inventory.setItem(NEXT_SLOT, createNavButton(Material.ARROW,
                    "&aNext Page", "&7Page " + (page + 2) + "/" + totalPages));
        }

        // Fill nav bar empty slots with glass
        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, createGlassPane());
            }
        }
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) allEntries.size() / SLOTS_PER_PAGE));
    }

    private ItemStack createGlassPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createNavButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open(Player player) {
        if (!listenerRegistered) {
            SupplyDrop plugin = SupplyDrop.getPluginInstance();
            if (plugin != null) {
                Bukkit.getPluginManager().registerEvents(this, plugin);
                listenerRegistered = true;
            }
        }
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PreviewGui gui)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= TOTAL_SLOTS) return;

        if (slot == PREV_SLOT && gui.currentPage > 0) {
            gui.renderPage(gui.currentPage - 1);
            e.getWhoClicked().openInventory(gui.inventory);
        } else if (slot == NEXT_SLOT && gui.currentPage < gui.getTotalPages() - 1) {
            gui.renderPage(gui.currentPage + 1);
            e.getWhoClicked().openInventory(gui.inventory);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof PreviewGui)) return;
        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }
    }
}
