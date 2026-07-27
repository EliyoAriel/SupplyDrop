package com.supplydrop.gui.config;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Config;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GUI for managing auto-drop templates.
 * Shows all templates from PackageManager, with weight and enabled toggle.
 * Templates not in auto-drop.templates are shown as disabled (weight 0).
 *
 * Layout (54 slots):
 *   Rows 0-4 (0-44): template items
 *   Row 5 (45-53): glass | glass | glass | glass | [BACK] | glass | glass | glass | glass
 */
public class ConfigAutoDropTemplatesGui implements Listener {

    private static final int SIZE = 54;
    private static final int BACK_SLOT = 49;
    private final Inventory inv;
    private boolean listenerRegistered = false;

    public ConfigAutoDropTemplatesGui() {
        inv = Bukkit.createInventory(null, SIZE, "§9Auto-Drop Templates");
        initializeItems();
    }

    private Map<String, Integer> loadTemplateWeights() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return weights;
        FileConfiguration fc = config.getConfig();
        if (fc == null) return weights;

        ConfigurationSection section = fc.getConfigurationSection(ConfigKeys.AUTO_DROP_TEMPLATES);
        if (section != null) {
            for (String name : section.getKeys(false)) {
                weights.put(name, Math.max(0, section.getInt(name + ".weight", 10)));
            }
        }
        return weights;
    }

    private void saveTemplateWeight(String name, int weight) {
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return;
        String path = ConfigKeys.AUTO_DROP_TEMPLATES + "." + name + ".weight";
        config.getConfig().set(path, Math.max(0, weight));
        config.saveConfig();
    }

    public void initializeItems() {
        inv.clear();

        Map<String, Integer> weights = loadTemplateWeights();
        Set<String> allTemplates = PackageManager.getPackageNames();

        // Merge: show all templates, even those not in auto-drop yet
        List<String> ordered = new ArrayList<>();
        for (String name : weights.keySet()) {
            if (!ordered.contains(name)) ordered.add(name);
        }
        for (String name : allTemplates) {
            if (!ordered.contains(name)) ordered.add(name);
        }

        int slot = 0;
        for (String name : ordered) {
            if (slot >= 45) break;

            int weight = weights.getOrDefault(name, 0);
            boolean enabled = weight > 0;
            Package pkg = PackageManager.get(name);
            int itemCount = pkg != null ? pkg.getLootTable().size() : 0;

            Material mat;
            if (!enabled) {
                mat = Material.GRAY_DYE;
            } else if (itemCount == 0) {
                mat = Material.BARRIER;
            } else {
                mat = Material.EMERALD;
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String status = enabled ? "§aON" : "§cOFF";
                meta.setDisplayName("§e" + name + " " + status);
                List<String> lore = new ArrayList<>();
                lore.add("§7Weight: §f" + weight);
                lore.add("§7Items in table: §f" + itemCount);
                if (pkg != null) {
                    lore.add("§7Display: §f" + pkg.getDisplayName());
                }
                lore.add("");
                if (enabled) {
                    lore.add("§aLeft-click: edit weight");
                    lore.add("§cShift-click: disable");
                } else {
                    lore.add("§aLeft-click: edit weight");
                    lore.add("§aShift-click: enable");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slot++;
        }

        // Fill rest with glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) { glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta); }
        for (int i = slot; i < 45; i++) {
            inv.setItem(i, glass);
        }

        // Row 5: all glass + back
        for (int i = 45; i < SIZE; i++) {
            inv.setItem(i, glass);
        }

        ItemStack backBtn = new ItemStack(Material.BLUE_WOOL);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§9← Back");
            backBtn.setItemMeta(backMeta);
        }
        inv.setItem(BACK_SLOT, backBtn);
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

        // Back
        if (slot == BACK_SLOT) {
            new ConfigAutoDropPage().openInventory(p);
            return;
        }

        // Template items (0-44)
        if (slot < 0 || slot >= 45) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        // Extract template name from display name: "§e<name> §aON" or "§e<name> §cOFF"
        String raw = meta.getDisplayName();
        String extractedName = raw.replace("§e", "").trim();
        if (extractedName.endsWith(" §aON")) extractedName = extractedName.substring(0, extractedName.length() - 5).trim();
        else if (extractedName.endsWith(" §cOFF")) extractedName = extractedName.substring(0, extractedName.length() - 6).trim();
        final String tplName = extractedName;

        Map<String, Integer> weights = loadTemplateWeights();
        int currentWeight = weights.getOrDefault(tplName, 0);

        // Shift-click → toggle enable/disable
        if (e.isShiftClick()) {
            int newWeight = currentWeight > 0 ? 0 : 10;
            saveTemplateWeight(tplName, newWeight);
            String status = newWeight > 0 ? "&aenabled" : "&cdisabled";
            ChatHandler.send(p, "Template &e" + tplName + " &7" + status + "&7.");
            plugin.restartAutoDropScheduler();
            initializeItems();
            return;
        }

        // Left-click → edit weight via number input
        p.closeInventory();
        new ConfigNumberInputGui(
                ConfigKeys.AUTO_DROP_TEMPLATES + "." + tplName + ".weight",
                tplName + " weight",
                currentWeight, 0, 1000,
                newVal -> {
                    saveTemplateWeight(tplName, newVal);
                    ChatHandler.send(p, "Weight for &e" + tplName + " &7set to &f" + newVal + "&7.");
                    plugin.restartAutoDropScheduler();
                    Bukkit.getScheduler().runTask(plugin, () -> openInventory(p));
                },
                () -> Bukkit.getScheduler().runTask(plugin, () -> openInventory(p))
        ).openInventory(p);
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
