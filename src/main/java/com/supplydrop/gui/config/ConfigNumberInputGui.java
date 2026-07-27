package com.supplydrop.gui.config;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Config;
import com.supplydrop.helpers.ChatHandler;

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
import java.util.function.Consumer;

/**
 * Dedicated GUI for number input.
 * Layout (27 slots):
 *   Row 0: [-100] [-10] [-1] [VALUE DISPLAY] [+1] [+10] [+100]
 *   Row 1: glass panes
 *   Row 2: [CANCEL] glass [CONFIRM]
 */
public class ConfigNumberInputGui implements Listener {

    private static final int SIZE = 27;
    private final Inventory inv;
    private final String configKey;
    private final String settingName;
    private int currentValue;
    private final int minValue;
    private final int maxValue;
    private final Consumer<Integer> onConfirm;
    private final Runnable onCancel;
    private boolean listenerRegistered = false;

    public ConfigNumberInputGui(String configKey, String settingName, int currentValue, int minValue, int maxValue,
                                Consumer<Integer> onConfirm, Runnable onCancel) {
        this.configKey = configKey;
        this.settingName = settingName;
        this.currentValue = currentValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.inv = Bukkit.createInventory(null, SIZE, "§9Set Value: " + settingName);
        initializeItems();
    }

    public void initializeItems() {
        inv.clear();

        // Row 0: step buttons + value display
        inv.setItem(0, stepItem(Material.RED_STAINED_GLASS, "§c-100", -100));
        inv.setItem(1, stepItem(Material.RED_STAINED_GLASS, "§c-10", -10));
        inv.setItem(2, stepItem(Material.RED_STAINED_GLASS, "§c-1", -1));

        // Value display (center)
        ItemStack display = new ItemStack(Material.CLOCK);
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            displayMeta.setDisplayName("§e" + settingName);
            displayMeta.setLore(List.of(
                    "§7Current value: §f" + currentValue,
                    "§7Range: §f" + minValue + " §7- §f" + maxValue,
                    "",
                    "§8Config: " + configKey
            ));
            display.setItemMeta(displayMeta);
        }
        inv.setItem(4, display);

        inv.setItem(6, stepItem(Material.LIME_STAINED_GLASS, "§a+1", 1));
        inv.setItem(7, stepItem(Material.LIME_STAINED_GLASS, "§a+10", 10));
        inv.setItem(8, stepItem(Material.LIME_STAINED_GLASS, "§a+100", 100));

        // Row 1: decorative glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, glass);
        }

        // Row 2: cancel + confirm + set zero + reset
        inv.setItem(19, actionItem(Material.RED_WOOL, "§cCancel", "§7Discard changes"));
        inv.setItem(22, actionItem(Material.LIME_WOOL, "§aConfirm", "§7Save value: §f" + currentValue));
        inv.setItem(23, actionItem(Material.OAK_BUTTON, "§fSet to 0", "§7Quick reset to zero"));
        inv.setItem(25, actionItem(Material.PAPER, "§eReset", "§7Reset to original: §f" + getOriginalValue()));
    }

    private int getOriginalValue() {
        Config config = SupplyDrop.getConfiguration();
        if (config != null) {
            return config.getConfig().getInt(configKey, 0);
        }
        return 0;
    }

    private ItemStack stepItem(Material mat, String name, int step) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of("§7Click to " + (step > 0 ? "add" : "subtract") + " §f" + Math.abs(step)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack actionItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void refreshDisplay() {
        // Update value display
        ItemStack display = new ItemStack(Material.CLOCK);
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            displayMeta.setDisplayName("§e" + settingName);
            displayMeta.setLore(List.of(
                    "§7Current value: §f" + currentValue,
                    "§7Range: §f" + minValue + " §7- §f" + maxValue,
                    "",
                    "§8Config: " + configKey
            ));
            display.setItemMeta(displayMeta);
        }
        inv.setItem(4, display);

        // Update confirm button
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§aConfirm");
            confirmMeta.setLore(List.of("§7Save value: §f" + currentValue));
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(22, confirm);
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

        // Step buttons
        if (slot == 0) { currentValue = clamp(currentValue - 100); refreshDisplay(); return; }
        if (slot == 1) { currentValue = clamp(currentValue - 10); refreshDisplay(); return; }
        if (slot == 2) { currentValue = clamp(currentValue - 1); refreshDisplay(); return; }
        if (slot == 6) { currentValue = clamp(currentValue + 1); refreshDisplay(); return; }
        if (slot == 7) { currentValue = clamp(currentValue + 10); refreshDisplay(); return; }
        if (slot == 8) { currentValue = clamp(currentValue + 100); refreshDisplay(); return; }

        // Reset
        if (slot == 25) {
            currentValue = getOriginalValue();
            refreshDisplay();
            return;
        }

        // Set to 0
        if (slot == 23) {
            currentValue = 0;
            refreshDisplay();
            return;
        }

        // Cancel
        if (slot == 19) {
            p.closeInventory();
            onCancel.run();
            return;
        }

        // Confirm
        if (slot == 22) {
            p.closeInventory();
            onConfirm.accept(currentValue);
            return;
        }
    }

    private int clamp(int value) {
        return Math.max(minValue, Math.min(maxValue, value));
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
