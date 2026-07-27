package com.supplydrop.gui.config;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Config;
import com.supplydrop.config.ConfigKeys;

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

public class ConfigTogglesPage implements Listener {

    private static final int SIZE = 27;
    private final Inventory inv;
    private boolean listenerRegistered = false;

    public ConfigTogglesPage() {
        inv = Bukkit.createInventory(null, SIZE, "§9Quick Toggles");
        initializeItems();
    }

    public void initializeItems() {
        inv.clear();
        fillGlass();

        inv.setItem(10, toggleItem("§eHologram", ConfigKeys.HOLOGRAM_ENABLED,
                "§7Show floating text above landed crates"));
        inv.setItem(12, toggleItem("§eAnnounce", ConfigKeys.ANNOUNCE_ENABLED,
                "§7Broadcast when a supply drop occurs"));
        inv.setItem(14, toggleItem("§eNotification Default", ConfigKeys.NOTIFICATION_DEFAULT_SUBSCRIBE,
                "§7New players receive drop notifications"));

        inv.setItem(22, infoItem(Material.ARROW, "§cBack to Menu", "§7Click to return"));
    }

    private void fillGlass() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) { glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta); }
        for (int i = 0; i < SIZE; i++) {
            if (i == 10 || i == 12 || i == 14 || i == 22) continue;
            inv.setItem(i, glass);
        }
    }

    private ItemStack toggleItem(String name, String configKey, String description) {
        boolean enabled = getConfig().getBoolean(configKey, false);
        Material mat = enabled ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS;
        String status = enabled ? "§aON" : "§cOFF";
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + ": " + status);
            meta.setLore(List.of(description, "", "§7Click to toggle", "§8Config: " + configKey));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack infoItem(Material mat, String name, String... lore) {
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

        int slot = e.getRawSlot();
        if (slot == 22) { new ConfigMainMenu().openInventory(p); return; }

        String configKey = getConfigKeyForSlot(slot);
        if (configKey == null) return;

        boolean current = getConfig().getBoolean(configKey, false);
        saveConfigValue(configKey, !current);
        initializeItems();
        p.openInventory(inv);
    }

    private String getConfigKeyForSlot(int slot) {
        return switch (slot) {
            case 10 -> ConfigKeys.HOLOGRAM_ENABLED;
            case 12 -> ConfigKeys.ANNOUNCE_ENABLED;
            case 14 -> ConfigKeys.NOTIFICATION_DEFAULT_SUBSCRIBE;
            default -> null;
        };
    }

    private void saveConfigValue(String key, Object value) {
        Config config = SupplyDrop.getConfiguration();
        if (config != null) { config.getConfig().set(key, value); config.saveConfig(); }
    }

    private org.bukkit.configuration.file.FileConfiguration getConfig() {
        Config config = SupplyDrop.getConfiguration();
        return config != null ? config.getConfig() : null;
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
