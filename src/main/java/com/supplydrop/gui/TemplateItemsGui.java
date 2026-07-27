package com.supplydrop.gui;

import com.supplydrop.SupplyDrop;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.loot.LootEntry;
import com.supplydrop.loot.LootTable;
import com.supplydrop.loot.RarityRegistry;
import com.supplydrop.loot.Rarity;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Level 3: Items in a specific rarity tier for a template.
 *
 * Layout (54 slots):
 *   Row 0-3 (0-35):  Loot items for current page (36 per page)
 *   Row 4  (36-44):  Glass panes — "Hold item → click to add"
 *   Row 5  (45-53):  ◀ Prev (45) | Back (49) | Next ▶ (53)
 *
 * Actions:
 *   Left-click loot item  → remove from table, give to player
 *   Click glass pane while holding item → add to table
 */
public class TemplateItemsGui implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 36;
    private static final int BACK_SLOT = 49;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final Inventory inv;
    private final Package pkg;
    private final Rarity rarity;
    private int currentPage = 0;
    private boolean listenerRegistered = false;

    public TemplateItemsGui(Package pkg, Rarity rarity) {
        this.pkg = pkg;
        this.rarity = rarity;
        String title = ChatColor.translateAlternateColorCodes('&', rarity.prefix() + rarity.key().toUpperCase())
                + ChatColor.GRAY + " - " + ChatColor.AQUA + pkg.getName();
        this.inv = Bukkit.createInventory(null, INVENTORY_SIZE, title);
    }

    private List<LootEntry> getRarityEntries() {
        return pkg.getLootTable().getEntriesByRarity(rarity);
    }

    private int getTotalPages() {
        int total = getRarityEntries().size();
        return Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
    }

    private void clampPage() {
        int maxPage = getTotalPages() - 1;
        if (currentPage > maxPage) currentPage = maxPage;
        if (currentPage < 0) currentPage = 0;
    }

    private ItemStack createAddPane() {
        ItemStack addPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta addMeta = addPane.getItemMeta();
        if (addMeta != null) {
            addMeta.setDisplayName(ChatColor.GREEN + "Click to add item");
            addMeta.setLore(List.of(ChatColor.GRAY + "Hold an item in your cursor", ChatColor.GRAY + "then click here"));
            addPane.setItemMeta(addMeta);
        }
        return addPane;
    }

    private void initializeItems() {
        inv.clear();
        clampPage();

        List<LootEntry> entries = getRarityEntries();
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());

        if (entries.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(ChatColor.GRAY + "No items yet");
                emptyMeta.setLore(List.of(ChatColor.DARK_GRAY + "Hold an item and click below to add"));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(18, empty);
        } else {
            for (int i = start; i < end; i++) {
                LootEntry entry = entries.get(i);
                int guiSlot = i - start;
                ItemStack display = entry.getItem().clone();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add("");
                    lore.add(ChatColor.GRAY + "Weight: " + ChatColor.WHITE + entry.getWeight());
                    lore.add(ChatColor.RED + "Left-click: remove");
                    lore.add(ChatColor.GOLD + "Right-click: edit weight");
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
                inv.setItem(guiSlot, display);
            }
        }

        // Row 4: glass "add" panes
        ItemStack addPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta addMeta = addPane.getItemMeta();
        if (addMeta != null) {
            addMeta.setDisplayName(ChatColor.GREEN + "Click to add item");
            addMeta.setLore(List.of(ChatColor.GRAY + "Hold an item in your cursor", ChatColor.GRAY + "then click here"));
            addPane.setItemMeta(addMeta);
        }
        for (int i = 36; i <= 44; i++) {
            inv.setItem(i, addPane);
        }

        // Row 5: navigation
        ItemStack backBtn = new ItemStack(Material.BLUE_WOOL);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.DARK_BLUE + "← Back");
            backBtn.setItemMeta(backMeta);
        }
        inv.setItem(BACK_SLOT, backBtn);

        if (currentPage > 0) {
            ItemStack prevBtn = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevBtn.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(ChatColor.YELLOW + "◀ Previous Page");
                prevBtn.setItemMeta(prevMeta);
            }
            inv.setItem(PREV_SLOT, prevBtn);
        }

        if (currentPage < getTotalPages() - 1) {
            ItemStack nextBtn = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextBtn.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page ▶");
                nextBtn.setItemMeta(nextMeta);
            }
            inv.setItem(NEXT_SLOT, nextBtn);
        }

        // Page info glass panes
        ItemStack infoPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta infoMeta = infoPane.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.GRAY + "Page " + (currentPage + 1) + "/" + getTotalPages()
                    + " | " + entries.size() + " items");
            infoPane.setItemMeta(infoMeta);
        }
        for (int i = 46; i <= 48; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, infoPane);
        }
        for (int i = 50; i <= 52; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, infoPane);
        }
    }

    /**
     * Maps a GUI slot (0-35) to the actual LootEntry index in the rarity-filtered list.
     */
    private int getFilteredIndex(int guiSlot) {
        return currentPage * ITEMS_PER_PAGE + guiSlot;
    }

    /**
     * Maps a filtered index to the real index in the full loot table.
     */
    private int getRealIndex(int filteredIndex) {
        LootTable table = pkg.getLootTable();
        int count = 0;
        for (int i = 0; i < table.size(); i++) {
            LootEntry entry = table.getEntries().get(i);
            if (entry.getRarity().equals(rarity)) {
                if (count == filteredIndex) return i;
                count++;
            }
        }
        return -1;
    }

    public void openInventory(HumanEntity ent) {
        ensureListenerRegistered();
        initializeItems();
        ent.openInventory(inv);
    }

    public void setPage(int page) {
        this.currentPage = page;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int slot = e.getRawSlot();

        // Player inventory area (bottom row) — let them pick up items freely
        if (slot >= INVENTORY_SIZE) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        // Back button
        if (slot == BACK_SLOT) {
            TemplateRarityGui rarityGui = new TemplateRarityGui(pkg);
            Bukkit.getPluginManager().registerEvents(rarityGui, SupplyDrop.getPluginInstance());
            Bukkit.getScheduler().runTask(SupplyDrop.getPluginInstance(), () -> rarityGui.openInventory(p));
            return;
        }

        // Prev page
        if (slot == PREV_SLOT && currentPage > 0) {
            currentPage--;
            initializeItems();
            return;
        }

        // Next page
        if (slot == NEXT_SLOT && currentPage < getTotalPages() - 1) {
            currentPage++;
            initializeItems();
            return;
        }

        // Loot item area (rows 0-3, slots 0-35)
        if (slot >= 0 && slot < ITEMS_PER_PAGE) {
            if (clicked == null || clicked.getType().isAir()) return;
            // Skip if it's the "no items" placeholder
            if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            // Right-click → edit weight via chat
            if (e.isRightClick()) {
                int filteredIndex = getFilteredIndex(slot);
                List<LootEntry> entries = getRarityEntries();
                if (filteredIndex < 0 || filteredIndex >= entries.size()) return;

                SupplyDrop plugin = SupplyDrop.getPluginInstance();
                if (plugin == null) return;

                LootEntry entry = entries.get(filteredIndex);
                String itemName = entry.getItem().getType().name();
                int currentWeight = entry.getWeight();

                plugin.setPendingWeightEdit(p.getUniqueId(), pkg.getName(), rarity, filteredIndex, currentPage);
                p.closeInventory();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    ChatHandler.send(p, "Editing weight for &f" + itemName
                            + " &7(current: &f" + currentWeight + "&7). Type new weight in chat, or &c/cancel&7.");
                });
                return;
            }

            // Left-click → remove item
            if (e.isLeftClick()) {
                int filteredIndex = getFilteredIndex(slot);
                int realIndex = getRealIndex(filteredIndex);
                if (realIndex < 0) return;

                List<LootEntry> entries = getRarityEntries();
                if (filteredIndex < 0 || filteredIndex >= entries.size()) return;

                LootEntry entry = entries.get(filteredIndex);
                ItemStack removed = entry.getItem().clone();

                pkg.getLootTable().removeEntry(realIndex);
                PackageManager.saveConfig();

                // Give item back, drop if inventory full
                var leftover = p.getInventory().addItem(removed);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                }

                // Restore cursor to what it was before the swap
                ItemStack cursorItem = p.getItemOnCursor();
                if (cursorItem != null && !cursorItem.getType().isAir()
                        && cursorItem.getType() == clicked.getType()) {
                    // Swap happened — put cursor item back on cursor, clear the slot
                    p.setItemOnCursor(new ItemStack(Material.AIR));
                    inv.setItem(slot, clicked);
                }

                ChatHandler.send(p, "Removed &f" + removed.getType().name()
                        + " &7from " + rarity.prefix() + rarity.key().toUpperCase());
                initializeItems();
                p.updateInventory();
                return;
            }
        }

        // Glass pane area (row 4, slots 36-44) → add item from cursor
        if (slot >= 36 && slot <= 44) {
            ItemStack cursorItem = p.getItemOnCursor();
            if (cursorItem == null || cursorItem.getType().isAir()) {
                ChatHandler.send(p, "&7Hold an item in your cursor, then click here to add it.");
                // Restore glass pane (cancel already fired, but ensure slot is clean)
                inv.setItem(slot, createAddPane());
                return;
            }

            ItemStack toAdd = cursorItem.clone();
            LootEntry entry = new LootEntry(toAdd, rarity, rarity.weight());
            pkg.getLootTable().addEntry(entry);
            PackageManager.saveConfig();

            // Clear cursor, restore glass pane, force sync, refresh
            p.setItemOnCursor(new ItemStack(Material.AIR));
            inv.setItem(slot, createAddPane());
            p.updateInventory();
            ChatHandler.send(p, "Added &f" + toAdd.getType().name() + " x" + toAdd.getAmount()
                    + " &7to " + rarity.prefix() + rarity.key().toUpperCase());
            initializeItems();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inv)) return;
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin == null || !plugin.isEnabled()) {
            unregister();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, this::unregisterIfIdle);
    }

    private void ensureListenerRegistered() {
        if (listenerRegistered) return;
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
        }
    }

    private void unregister() {
        HandlerList.unregisterAll(this);
        listenerRegistered = false;
    }

    private void unregisterIfIdle() {
        if (!inv.getViewers().isEmpty()) return;
        unregister();
    }
}
