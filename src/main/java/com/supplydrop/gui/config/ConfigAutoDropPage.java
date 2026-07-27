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

public class ConfigAutoDropPage implements Listener {

    private static final int SIZE = 54;
    private final Inventory inv;
    private boolean listenerRegistered = false;

    public ConfigAutoDropPage() {
        inv = Bukkit.createInventory(null, SIZE, "§9Auto-Drop Settings");
        initializeItems();
    }

    public void initializeItems() {
        inv.clear();
        fillGlass();

        FileConfiguration fc = getFileConfig();

        // Row 10-12: Interval settings (PAPER)
        inv.setItem(10, settingItem(Material.PAPER, "§eInterval", ConfigKeys.AUTO_DROP_INTERVAL,
                fc.getInt(ConfigKeys.AUTO_DROP_INTERVAL, 72000),
                "§7Fixed interval between drops in ticks",
                "§772000 ticks = 1 hour",
                "§7Used when random-interval is OFF"));
        inv.setItem(11, settingItem(Material.PAPER, "§eInterval-Min", ConfigKeys.AUTO_DROP_INTERVAL_MIN,
                fc.getInt(ConfigKeys.AUTO_DROP_INTERVAL_MIN, 36000),
                "§7Minimum interval for random drops",
                "§736000 ticks = 30 minutes"));
        inv.setItem(12, settingItem(Material.PAPER, "§eInterval-Max", ConfigKeys.AUTO_DROP_INTERVAL_MAX,
                fc.getInt(ConfigKeys.AUTO_DROP_INTERVAL_MAX, 72000),
                "§7Maximum interval for random drops",
                "§772000 ticks = 1 hour"));

        // Row 14-16: Drop behavior (CLOCK, COMPASS, etc.)
        inv.setItem(14, settingItem(Material.CLOCK, "§eWave-Count", ConfigKeys.AUTO_DROP_WAVE_COUNT,
                fc.getInt(ConfigKeys.AUTO_DROP_WAVE_COUNT, 1),
                "§7Number of crates to drop simultaneously",
                "§71 = single drop, 2+ = wave"));
        inv.setItem(15, settingItem(Material.PAPER, "§eExpiry", ConfigKeys.AUTO_DROP_EXPIRY,
                fc.getInt(ConfigKeys.AUTO_DROP_EXPIRY, 0),
                "§7Auto-destroy crate after this many ticks",
                "§70 = never expire (unlimited time)"));
        inv.setItem(16, settingItem(Material.FEATHER, "§eFall-Duration", ConfigKeys.AUTO_DROP_FALL_DURATION,
                fc.getInt(ConfigKeys.AUTO_DROP_FALL_DURATION, 0),
                "§7How long the crate takes to fall (seconds)",
                "§70 = use global drop.fall-duration"));

        // Row 19-21: Announce settings (PAPER)
        inv.setItem(19, settingItem(Material.PAPER, "§eAnnounce-Delay", ConfigKeys.AUTO_DROP_ANNOUNCE_DELAY,
                fc.getInt(ConfigKeys.AUTO_DROP_ANNOUNCE_DELAY, 100),
                "§7Ticks before announcement appears",
                "§720 ticks = 1 second"));
        inv.setItem(20, settingItem(Material.PAPER, "§eCoord-Reveal", ConfigKeys.AUTO_DROP_COORD_REVEAL_DELAY,
                fc.getInt(ConfigKeys.AUTO_DROP_COORD_REVEAL_DELAY, 0),
                "§7Ticks after landing to reveal coordinates",
                "§70 = reveal immediately"));

        // Row 23-25: Crate overrides (CHEST, TRAPDOOR)
        inv.setItem(23, settingItem(Material.CHEST, "§eTeam-Range", ConfigKeys.AUTO_DROP_TEAM_CRATE_RANGE,
                fc.getInt(ConfigKeys.AUTO_DROP_TEAM_CRATE_RANGE, 2),
                "§7Max players needed for team crate",
                "§7Actual count is random between 2 and this value"));
        inv.setItem(24, settingItem(Material.OAK_TRAPDOOR, "§eTrap-Chance %", ConfigKeys.AUTO_DROP_TRAP_CHANCE,
                fc.getInt(ConfigKeys.AUTO_DROP_TRAP_CHANCE, 0),
                "§7Chance for a crate to be a trap",
                "§70 = disabled, 100 = always trap"));
        inv.setItem(25, settingItem(Material.EMERALD, "§eLoot-Scaling-Max", ConfigKeys.AUTO_DROP_LOOT_SCALING_MAX,
                fc.getInt(ConfigKeys.AUTO_DROP_LOOT_SCALING_MAX, 20),
                "§7Max bonus rolls from loot scaling",
                "§7More rolls = better loot over time"));
        inv.setItem(26, settingItem(Material.CHEST, "§eTeam-Chance %", ConfigKeys.AUTO_DROP_TEAM_CRATE_CHANCE,
                fc.getInt(ConfigKeys.AUTO_DROP_TEAM_CRATE_CHANCE, 0),
                "§7Chance for a crate to be a team crate",
                "§70 = disabled, 100 = always team"));

        // Row 28-32: Toggles (LIME/RED glass)
        inv.setItem(28, toggleItem("§eEnabled", ConfigKeys.AUTO_DROP_ENABLED, fc.getBoolean(ConfigKeys.AUTO_DROP_ENABLED, false),
                "§7Master toggle for auto-drops"));
        inv.setItem(29, toggleItem("§ePaused", ConfigKeys.AUTO_DROP_PAUSED, fc.getBoolean(ConfigKeys.AUTO_DROP_PAUSED, false),
                "§7Pause scheduler without disabling"));
        inv.setItem(30, toggleItem("§eRandom Interval", ConfigKeys.AUTO_DROP_RANDOM_INTERVAL, fc.getBoolean(ConfigKeys.AUTO_DROP_RANDOM_INTERVAL, true),
                "§7Use random interval between min/max"));
        inv.setItem(31, toggleItem("§eAnnounce", ConfigKeys.AUTO_DROP_ANNOUNCE, fc.getBoolean(ConfigKeys.AUTO_DROP_ANNOUNCE, true),
                "§7Broadcast announcement on drop"));
        inv.setItem(32, toggleItem("§eAnnounce-Actionbar", ConfigKeys.AUTO_DROP_ANNOUNCE_ACTIONBAR, fc.getBoolean(ConfigKeys.AUTO_DROP_ANNOUNCE_ACTIONBAR, false),
                "§7Show countdown on actionbar"));
        inv.setItem(33, toggleItem("§eLoot-Scaling", ConfigKeys.AUTO_DROP_LOOT_SCALING, fc.getBoolean(ConfigKeys.AUTO_DROP_LOOT_SCALING, false),
                "§7Bonus rolls based on time"));

        // Row 37-38: Special items
        int tplCount = ConfigKeys.getAutoDropTemplates().size();
        inv.setItem(37, settingItem(Material.CHEST, "§eTemplates (" + tplCount + ")",
                ConfigKeys.AUTO_DROP_TEMPLATES + ".count",
                tplCount,
                "§7Manage auto-drop templates",
                "§7Click to open template editor",
                "§8Config: " + ConfigKeys.AUTO_DROP_TEMPLATES));

        inv.setItem(38, worldItem("§eWorld", ConfigKeys.getAutoDropWorld()));

        inv.setItem(39, settingItem(Material.COMPASS, "§eRandom-Radius", ConfigKeys.AUTO_DROP_RANDOM_RADIUS,
                fc.getInt(ConfigKeys.AUTO_DROP_RANDOM_RADIUS, 500),
                "§7Random area around spawn for drops",
                "§7500 = ±500 blocks from world spawn"));

        List<String> autoTrapMobs = ConfigKeys.getAutoDropTrapMobs();
        inv.setItem(41, infoItem(Material.ZOMBIE_HEAD, "§eTrap-Mobs (" + autoTrapMobs.size() + ")",
                "§7Current mobs: §f" + String.join(", ", autoTrapMobs),
                "§7Click to manage trap mob types"));

        // Back
        inv.setItem(40, infoItem(Material.ARROW, "§cBack to Menu", "§7Click to return"));
    }

    private void fillGlass() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < SIZE; i++) {
            if (isSettingSlot(i)) continue;
            inv.setItem(i, glass);
        }
    }

    private boolean isSettingSlot(int slot) {
        return (slot >= 10 && slot <= 16) || (slot >= 19 && slot <= 20)
                || (slot >= 23 && slot <= 25) || (slot >= 28 && slot <= 33)
                || slot == 37 || slot == 38 || slot == 39 || slot == 40;
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

    private ItemStack toggleItem(String name, String configKey, boolean enabled, String description) {
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

    private ItemStack worldItem(String name, String currentWorld) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + ": §f" + currentWorld);
            meta.setLore(List.of(
                    "§7World where auto-drops occur",
                    "",
                    "§7Click to select a different world"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack infoItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
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

        if (slot == 40) {
            new ConfigMainMenu().openInventory(p);
            return;
        }

        if (slot == 37) {
            new ConfigAutoDropTemplatesGui().openInventory(p);
            return;
        }

        if (slot == 38) {
            new ConfigWorldSelectGui(ConfigKeys.AUTO_DROP_WORLD, ConfigKeys.getAutoDropWorld()).openInventory(p);
            return;
        }

        if (slot == 41) {
            new ConfigTrapMobsGui(ConfigKeys.AUTO_DROP_TRAP_MOBS, "§9Auto-Drop Trap Mobs", () -> new ConfigAutoDropPage().openInventory(p)).openInventory(p);
            return;
        }

        // Number settings → open number input GUI
        String configKey = getConfigKeyForSlot(slot);
        if (configKey != null) {
            int current = getFileConfig().getInt(configKey, 0);
            int min = configKey.equals(ConfigKeys.AUTO_DROP_TEAM_CRATE_RANGE) ? 2 : 0;
            int max = configKey.contains("CHANCE") || configKey.contains("RANGE") ? 100 : Integer.MAX_VALUE;
            if (configKey.equals(ConfigKeys.AUTO_DROP_WAVE_COUNT)) max = 64;

            new ConfigNumberInputGui(configKey, getSlotName(slot), current, min, max, value -> {
                saveConfigValue(configKey, value);
                ChatHandler.send(p, getSlotName(slot) + " §aset to §f" + value + "§7.");
                Bukkit.getScheduler().runTask(plugin, () -> openInventory(p));
            }, () -> Bukkit.getScheduler().runTask(plugin, () -> openInventory(p))).openInventory(p);
            return;
        }

        // Toggle settings
        String toggleKey = getToggleConfigKeyForSlot(slot);
        if (toggleKey != null) {
            boolean current = getFileConfig().getBoolean(toggleKey, false);
            saveConfigValue(toggleKey, !current);
            plugin.restartAutoDropScheduler();
            initializeItems();
            p.openInventory(inv);
        }
    }

    private String getConfigKeyForSlot(int slot) {
        return switch (slot) {
            case 10 -> ConfigKeys.AUTO_DROP_INTERVAL;
            case 11 -> ConfigKeys.AUTO_DROP_INTERVAL_MIN;
            case 12 -> ConfigKeys.AUTO_DROP_INTERVAL_MAX;
            case 14 -> ConfigKeys.AUTO_DROP_WAVE_COUNT;
            case 15 -> ConfigKeys.AUTO_DROP_EXPIRY;
            case 16 -> ConfigKeys.AUTO_DROP_FALL_DURATION;
            case 19 -> ConfigKeys.AUTO_DROP_ANNOUNCE_DELAY;
            case 20 -> ConfigKeys.AUTO_DROP_COORD_REVEAL_DELAY;
            case 23 -> ConfigKeys.AUTO_DROP_TEAM_CRATE_RANGE;
            case 24 -> ConfigKeys.AUTO_DROP_TRAP_CHANCE;
            case 25 -> ConfigKeys.AUTO_DROP_LOOT_SCALING_MAX;
            case 26 -> ConfigKeys.AUTO_DROP_TEAM_CRATE_CHANCE;
            case 39 -> ConfigKeys.AUTO_DROP_RANDOM_RADIUS;
            default -> null;
        };
    }

    private String getToggleConfigKeyForSlot(int slot) {
        return switch (slot) {
            case 28 -> ConfigKeys.AUTO_DROP_ENABLED;
            case 29 -> ConfigKeys.AUTO_DROP_PAUSED;
            case 30 -> ConfigKeys.AUTO_DROP_RANDOM_INTERVAL;
            case 31 -> ConfigKeys.AUTO_DROP_ANNOUNCE;
            case 32 -> ConfigKeys.AUTO_DROP_ANNOUNCE_ACTIONBAR;
            case 33 -> ConfigKeys.AUTO_DROP_LOOT_SCALING;
            default -> null;
        };
    }

    private String getSlotName(int slot) {
        return switch (slot) {
            case 10 -> "Interval";
            case 11 -> "Interval-Min";
            case 12 -> "Interval-Max";
            case 14 -> "Wave-Count";
            case 15 -> "Expiry";
            case 16 -> "Fall-Duration";
            case 19 -> "Announce-Delay";
            case 20 -> "Coord-Reveal";
            case 23 -> "Team-Range";
            case 24 -> "Trap-Chance%";
            case 25 -> "Loot-Scaling-Max";
            case 26 -> "Team-Chance%";
            case 39 -> "Random-Radius";
            default -> "Setting";
        };
    }

    private void saveConfigValue(String key, Object value) {
        Config config = SupplyDrop.getConfiguration();
        if (config != null) {
            config.getConfig().set(key, value);
            config.saveConfig();
        }
    }

    private FileConfiguration getFileConfig() {
        Config config = SupplyDrop.getConfiguration();
        return config != null ? config.getConfig() : null;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) e.setCancelled(true);
    }

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
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
        }
    }
    private void unregister() { HandlerList.unregisterAll(this); listenerRegistered = false; }
    private void unregisterIfIdle() { if (!inv.getViewers().isEmpty()) return; unregister(); }
}
