package com.supplydrop.gui;

import com.supplydrop.SupplyDrop;
import com.supplydrop.gui.config.ConfigNumberInputGui;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.packages.Package;
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

/**
 * Per-template settings GUI.
 * Layout (27 slots):
 *   Row 0: glass | title | glass
 *   Row 1: [display-name] [fall-duration] [item-count]
 *   Row 2: glass | [BACK] | glass
 */
public class TemplateSettingsGui implements Listener {

    private static final int SIZE = 27;
    private final Inventory inv;
    private final Package pkg;
    private boolean listenerRegistered = false;

    public TemplateSettingsGui(Package pkg) {
        this.pkg = pkg;
        this.inv = Bukkit.createInventory(null, SIZE, "§9Template Settings - " + pkg.getName());
        initializeItems();
    }

    private void initializeItems() {
        inv.clear();

        // Row 0: glass + title
        ItemStack titleItem = new ItemStack(Material.NAME_TAG);
        ItemMeta titleMeta = titleItem.getItemMeta();
        if (titleMeta != null) {
            String displayName = pkg.getDisplayName();
            titleMeta.setDisplayName("§b§l" + pkg.getName());
            titleMeta.setLore(List.of(
                    "§7Display name: §f" + displayName,
                    "§7Items in loot table: §f" + pkg.getLootTable().size(),
                    "",
                    "§7Configure this template's settings."
            ));
            titleItem.setItemMeta(titleMeta);
        }

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) { glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta); }

        inv.setItem(0, glass);
        inv.setItem(1, titleItem);
        inv.setItem(2, glass);

        // Row 1: settings items
        // Display name
        ItemStack displayNameItem = new ItemStack(Material.ANVIL);
        ItemMeta dnMeta = displayNameItem.getItemMeta();
        if (dnMeta != null) {
            dnMeta.setDisplayName("§eDisplay Name");
            dnMeta.setLore(List.of(
                    "§7Current: §f" + pkg.getDisplayName(),
                    "",
                    "§aClick to change display name",
                    "§7via chat input"
            ));
            displayNameItem.setItemMeta(dnMeta);
        }
        inv.setItem(10, displayNameItem);

        // Fall duration
        int fallDuration = pkg.getFallDuration();
        ItemStack fallDurItem = new ItemStack(Material.CLOCK);
        ItemMeta fdMeta = fallDurItem.getItemMeta();
        if (fdMeta != null) {
            fdMeta.setDisplayName("§eFall Duration");
            if (fallDuration > 0) {
                fdMeta.setLore(List.of(
                        "§7Current: §f" + fallDuration + "s §7(" + (fallDuration * 20) + " ticks)",
                        "",
                        "§aClick to change",
                        "§7Set to §f0 §7to use global default"
                ));
            } else {
                fdMeta.setLore(List.of(
                        "§7Current: §f0 §7(use global default)",
                        "",
                        "§aClick to change",
                        "§7Overrides the global fall duration"
                ));
            }
            fallDurItem.setItemMeta(fdMeta);
        }
        inv.setItem(11, fallDurItem);

        // Item count info
        int itemCount = pkg.getLootTable().size();
        ItemStack countItem = new ItemStack(Material.CHEST);
        ItemMeta countMeta = countItem.getItemMeta();
        if (countMeta != null) {
            countMeta.setDisplayName("§eLoot Table Info");
            countMeta.setLore(List.of(
                    "§7Total items: §f" + itemCount,
                    "",
                    "§7Edit items via the rarity tiers",
                    "§7in the previous menu"
            ));
            countItem.setItemMeta(countMeta);
        }
        inv.setItem(12, countItem);

        // Row 2: glass + back
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, glass);
        }

        ItemStack backBtn = new ItemStack(Material.BLUE_WOOL);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§9← Back");
            backBtn.setItemMeta(backMeta);
        }
        inv.setItem(22, backBtn);
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
            case 10 -> {
                // Display name → chat input
                p.closeInventory();
                int prevPage = -1;
                plugin.setPendingStringInput(p.getUniqueId(), input -> {
                    PackageManager.setDisplayName(pkg.getName(), input);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        new TemplateSettingsGui(pkg).openInventory(p);
                    });
                });
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ChatHandler.send(p, "Enter a new display name for &b" + pkg.getName()
                            + " &7in chat. Type &c/cancel &7to cancel.");
                });
            }
            case 11 -> {
                // Fall duration → ConfigNumberInputGui
                p.closeInventory();
                int currentFd = pkg.getFallDuration();
                new ConfigNumberInputGui(
                        "fall-duration:" + pkg.getName(),
                        pkg.getName() + " fall-duration",
                        currentFd, 0, 300,
                        // onConfirm
                        newVal -> {
                            PackageManager.setFallDuration(pkg.getName(), newVal);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                new TemplateSettingsGui(pkg).openInventory(p);
                            });
                        },
                        // onCancel → reopen settings
                        () -> Bukkit.getScheduler().runTask(plugin, () -> {
                            new TemplateSettingsGui(pkg).openInventory(p);
                        })
                ).openInventory(p);
            }
            case 22 -> {
                // Back → reopen rarity GUI
                TemplateRarityGui rarityGui = new TemplateRarityGui(pkg);
                Bukkit.getPluginManager().registerEvents(rarityGui, plugin);
                Bukkit.getScheduler().runTask(plugin, () -> rarityGui.openInventory(p));
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
