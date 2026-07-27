package com.supplydrop.gui.config;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Config;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.helpers.ChatHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
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

public class ConfigCratePage implements Listener {

    private static final int SIZE = 54;
    private final Inventory inv;
    private boolean listenerRegistered = false;

    public ConfigCratePage() {
        inv = Bukkit.createInventory(null, SIZE, "§9Crate Settings");
        initializeItems();
    }

    public void initializeItems() {
        inv.clear();
        fillGlass();

        FileConfiguration fc = getFileConfig();

        inv.setItem(10, settingItem(Material.CLOCK, "§eExpiry", ConfigKeys.CRATE_EXPIRY,
                fc.getInt(ConfigKeys.CRATE_EXPIRY, 0),
                "§7Auto-destroy crate after this many ticks",
                "§70 = never expire (unlimited time)"));

        inv.setItem(11, settingItem(Material.BARRIER, "§eProtection-Radius", ConfigKeys.CRATE_PROTECTION_RADIUS,
                fc.getInt(ConfigKeys.CRATE_PROTECTION_RADIUS, 2),
                "§7Anti-grief radius around crate (blocks)",
                "§70 = disabled, 2 = default"));

        inv.setItem(12, settingItem(Material.REDSTONE, "§eMax-Active", ConfigKeys.CRATE_MAX_ACTIVE,
                fc.getInt(ConfigKeys.CRATE_MAX_ACTIVE, 30),
                "§7Maximum active supplydrops at once",
                "§7Falling + landed crates combined",
                "§70 = unlimited"));

        inv.setItem(14, settingItem(Material.EMERALD, "§eTeam-Open-Chance %", ConfigKeys.CRATE_TEAM_OPEN_CHANCE,
                fc.getInt(ConfigKeys.CRATE_TEAM_OPEN_CHANCE, 0),
                "§7Chance for crate to require team to open",
                "§70 = disabled, 100 = always team"));

        inv.setItem(15, settingItem(Material.PLAYER_HEAD, "§eTeam-Range", ConfigKeys.CRATE_TEAM_OPEN_RANGE,
                fc.getInt(ConfigKeys.CRATE_TEAM_OPEN_RANGE, 2),
                "§7Max players needed for team crate",
                "§7Actual count is random between 2 and this"));

        inv.setItem(19, settingItem(Material.OAK_TRAPDOOR, "§eTrap-Chance %", ConfigKeys.CRATE_TRAP_CHANCE,
                fc.getInt(ConfigKeys.CRATE_TRAP_CHANCE, 0),
                "§7Chance for crate to be a trap",
                "§70 = disabled, 100 = always trap"));

        inv.setItem(21, settingItem(Material.FEATHER, "§eFall-Duration (global)", ConfigKeys.DROP_FALL_DURATION,
                fc.getInt(ConfigKeys.DROP_FALL_DURATION, 0),
                "§7Global fall duration in seconds",
                "§70 = use falling-speed instead"));

        List<String> mobs = ConfigKeys.getCrateTrapMobs();
        inv.setItem(28, infoItem(Material.ZOMBIE_HEAD, "§eTrap-Mobs (" + mobs.size() + ")",
                "§7Current mobs: §f" + String.join(", ", mobs),
                "§7Click to manage trap mob types"));

        inv.setItem(40, infoItem(Material.ARROW, "§cBack to Menu", "§7Click to return"));
    }

    private void fillGlass() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) { glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta); }
        for (int i = 0; i < SIZE; i++) {
            if (isSettingSlot(i)) continue;
            inv.setItem(i, glass);
        }
    }

    private boolean isSettingSlot(int slot) {
        return slot == 10 || slot == 11 || slot == 12 || slot == 14 || slot == 15
                || slot == 19 || slot == 21 || slot == 28 || slot == 40;
    }

    private ItemStack settingItem(Material mat, String name, String configKey, int current, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + ": §f" + current);
            java.util.ArrayList<String> fullLore = new java.util.ArrayList<>();
            for (String line : lore) fullLore.add(line);
            fullLore.add("");
            fullLore.add("§7Click to edit this value");
            fullLore.add("§8Config: " + configKey);
            meta.setLore(fullLore);
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
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin == null) return;

        int slot = e.getRawSlot();
        if (slot == 40) { new ConfigMainMenu().openInventory(p); return; }
        if (slot == 28) { new ConfigTrapMobsGui(ConfigKeys.CRATE_TRAP_MOBS, "§9Crate Trap Mobs", () -> new ConfigCratePage().openInventory(p)).openInventory(p); return; }

        String configKey = getConfigKeyForSlot(slot);
        if (configKey != null) {
            int current = getFileConfig().getInt(configKey, 0);
            int min = configKey.equals(ConfigKeys.CRATE_TEAM_OPEN_RANGE) ? 2 : 0;
            int max = configKey.contains("CHANCE") || configKey.contains("RANGE") ? 100 : Integer.MAX_VALUE;
            if (configKey.equals(ConfigKeys.CRATE_MAX_ACTIVE)) max = 1000;

            new ConfigNumberInputGui(configKey, getSlotName(slot), current, min, max, value -> {
                saveConfigValue(configKey, value);
                ChatHandler.send(p, getSlotName(slot) + " §aset to §f" + value + "§7.");
                Bukkit.getScheduler().runTask(plugin, () -> openInventory(p));
            }, () -> Bukkit.getScheduler().runTask(plugin, () -> openInventory(p))).openInventory(p);
        }
    }

    private String getConfigKeyForSlot(int slot) {
        return switch (slot) {
            case 10 -> ConfigKeys.CRATE_EXPIRY;
            case 11 -> ConfigKeys.CRATE_PROTECTION_RADIUS;
            case 12 -> ConfigKeys.CRATE_MAX_ACTIVE;
            case 14 -> ConfigKeys.CRATE_TEAM_OPEN_CHANCE;
            case 15 -> ConfigKeys.CRATE_TEAM_OPEN_RANGE;
            case 19 -> ConfigKeys.CRATE_TRAP_CHANCE;
            case 21 -> ConfigKeys.DROP_FALL_DURATION;
            default -> null;
        };
    }

    private String getSlotName(int slot) {
        return switch (slot) {
            case 10 -> "Expiry";
            case 11 -> "Protection-Radius";
            case 12 -> "Max-Active";
            case 14 -> "Team-Open-Chance%";
            case 15 -> "Team-Range";
            case 19 -> "Trap-Chance%";
            case 21 -> "Fall-Duration";
            default -> "Setting";
        };
    }

    private void saveConfigValue(String key, Object value) {
        Config config = SupplyDrop.getConfiguration();
        if (config != null) { config.getConfig().set(key, value); config.saveConfig(); }
    }

    private FileConfiguration getFileConfig() {
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
