package com.supplydrop.gui;

import com.supplydrop.SupplyDrop;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.loot.LootEntry;
import com.supplydrop.loot.LootTable;
import com.supplydrop.loot.Rarity;
import com.supplydrop.loot.RarityRegistry;
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

import java.util.List;

/**
 * Level 2: Shows rarity tiers for a template.
 * Click a rarity → opens TemplateItemsGui for that rarity.
 * Click back → returns to TemplateListGui.
 * Click delete → deletes the template.
 */
public class TemplateRarityGui implements Listener {

    private static final int INVENTORY_SIZE = 45;
    private static final int BACK_SLOT = 39;
    private static final int DELETE_SLOT = 41;

    private static final Material[] TIER_MATERIALS = {
        Material.COAL_BLOCK,       // index 0
        Material.RAW_IRON_BLOCK,   // index 1
        Material.IRON_BLOCK,       // index 2
        Material.DIAMOND_BLOCK,    // index 3
        Material.GOLD_BLOCK,       // index 4
        Material.EMERALD_BLOCK,    // index 5
        Material.NETHERITE_BLOCK,  // index 6
        Material.AMETHYST_BLOCK,   // index 7+
    };

    private final Inventory inv;
    private final Package pkg;
    private boolean listenerRegistered = false;

    public TemplateRarityGui(Package pkg) {
        this.pkg = pkg;
        this.inv = Bukkit.createInventory(null, INVENTORY_SIZE, ChatColor.DARK_BLUE + pkg.getName() + ChatColor.GRAY + " - Rarities");
        initializeItems();
    }

    private Material getMaterialForIndex(int index) {
        if (index < TIER_MATERIALS.length) return TIER_MATERIALS[index];
        return Material.STONE;
    }

    private void initializeItems() {
        inv.clear();
        LootTable table = pkg.getLootTable();
        List<Rarity> rarities = RarityRegistry.getAll();

        for (int i = 0; i < rarities.size() && i < DELETE_SLOT; i++) {
            Rarity rarity = rarities.get(i);
            List<LootEntry> entries = table.getEntriesByRarity(rarity);
            int count = entries.size();

            Material displayMat = getMaterialForIndex(i);
            ItemStack item = new ItemStack(displayMat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(rarity.chatColor() + rarity.key().toUpperCase());
                meta.setLore(List.of(
                        "§7Items in this tier: §f" + count,
                        "§7Tier weight: §f" + rarity.weight(),
                        "",
                        "§aClick to edit items"
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        // Back button
        ItemStack backBtn = new ItemStack(Material.BLUE_WOOL);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§9← Back");
            backBtn.setItemMeta(backMeta);
        }
        inv.setItem(BACK_SLOT, backBtn);

        // Delete button
        ItemStack deleteBtn = new ItemStack(Material.RED_WOOL);
        ItemMeta deleteMeta = deleteBtn.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName("§cDelete Template");
            deleteMeta.setLore(List.of("§7Click to delete §c" + pkg.getName()));
            deleteBtn.setItemMeta(deleteMeta);
        }
        inv.setItem(DELETE_SLOT, deleteBtn);
    }

    public void openInventory(HumanEntity ent) {
        ensureListenerRegistered();
        initializeItems();
        ent.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        if (e.getRawSlot() >= INVENTORY_SIZE) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // Back button
        if (clicked.getType() == Material.BLUE_WOOL) {
            TemplateListGui listGui = new TemplateListGui();
            Bukkit.getPluginManager().registerEvents(listGui, SupplyDrop.getPluginInstance());
            Bukkit.getScheduler().runTask(SupplyDrop.getPluginInstance(), () -> {
                p.closeInventory();
                listGui.openInventory(p);
            });
            return;
        }

        // Delete button
        if (clicked.getType() == Material.RED_WOOL) {
            PackageManager.deletePackage(pkg.getName());
            ChatHandler.send(p, "Template &c" + pkg.getName() + " &adeleted.");
            HandlerList.unregisterAll(this);
            p.closeInventory();
            return;
        }

        // Rarity buttons — find rarity by slot index
        int slot = e.getRawSlot();
        List<Rarity> rarities = RarityRegistry.getAll();
        if (slot < 0 || slot >= rarities.size()) return;

        Rarity rarity = rarities.get(slot);
        TemplateItemsGui itemsGui = new TemplateItemsGui(pkg, rarity);
        Bukkit.getPluginManager().registerEvents(itemsGui, SupplyDrop.getPluginInstance());
        itemsGui.openInventory(p);
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
