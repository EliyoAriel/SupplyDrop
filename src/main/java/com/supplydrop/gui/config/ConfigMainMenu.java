package com.supplydrop.gui.config;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Config;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.gui.TemplateListGui;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.packages.PackageManager;

import org.bukkit.Bukkit;
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

public class ConfigMainMenu implements Listener {

    private static final int SIZE = 54;
    private static final int RELOAD_SLOT = 49;
    private final Inventory inv;
    private boolean listenerRegistered = false;

    public ConfigMainMenu() {
        inv = Bukkit.createInventory(null, SIZE, "§9SupplyDrop - Configuration");
        initializeItems();
    }

    public void initializeItems() {
        inv.clear();

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) { glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta); }
        for (int i = 0; i < SIZE; i++) {
            if (i == 10 || i == 11 || i == 12 || i == 13 || i == 14 || i == RELOAD_SLOT) continue;
            inv.setItem(i, glass);
        }

        inv.setItem(10, sectionItem(Material.REDSTONE, "§bAuto-Drop Settings",
                "§7Scheduler, intervals, world,",
                "§7announcements, wave drops"));
        inv.setItem(11, sectionItem(Material.CHEST, "§bCrate Settings",
                "§7Expiry, protection, max-active,",
                "§7team crate, trap chance"));
        inv.setItem(12, sectionItem(Material.CHEST, "§bTemplates",
                "§7Manage loot table templates",
                "§7and their items"));
        inv.setItem(13, sectionItem(Material.ARMOR_STAND, "§bHologram Settings",
                "§7Floating text above crates,",
                "§7lines and placeholders"));
        inv.setItem(14, sectionItem(Material.REDSTONE_LAMP, "§bQuick Toggles",
                "§7Hologram, announce,",
                "§7notification defaults"));

        inv.setItem(RELOAD_SLOT, sectionItem(Material.REPEATER, "§eReload Config",
                "§7Reload all configuration files",
                "§7and restart scheduler"));
    }

    private ItemStack sectionItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(List.of(lore)); item.setItemMeta(meta); }
        return item;
    }

    public void openInventory(HumanEntity ent) {
        ensureListenerRegistered();
        initializeItems();
        ent.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        if (e.getRawSlot() >= SIZE) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin == null) return;

        switch (e.getRawSlot()) {
            case 10 -> new ConfigAutoDropPage().openInventory(p);
            case 11 -> new ConfigCratePage().openInventory(p);
            case 12 -> new TemplateListGui().openInventory(p);
            case 13 -> new ConfigHologramPage().openInventory(p);
            case 14 -> new ConfigTogglesPage().openInventory(p);
            case RELOAD_SLOT -> {
                Config config = SupplyDrop.getConfiguration();
                if (config != null) {
                    config.reloadConfig();
                    PackageManager.reload();
                    plugin.restartAutoDropScheduler();
                    ChatHandler.send(p, "Configuration &areloaded&7.");
                    initializeItems();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) { if (e.getInventory().equals(inv)) e.setCancelled(true); }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inv)) return;
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin == null || !plugin.isEnabled()) { unregister(); return; }
        Bukkit.getScheduler().runTask(plugin, this::unregisterIfIdle);
    }

    private void ensureListenerRegistered() {
        if (listenerRegistered) return;
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin != null && plugin.isEnabled()) { Bukkit.getPluginManager().registerEvents(this, plugin); listenerRegistered = true; }
    }
    private void unregister() { HandlerList.unregisterAll(this); listenerRegistered = false; }
    private void unregisterIfIdle() { if (!inv.getViewers().isEmpty()) return; unregister(); }
}
