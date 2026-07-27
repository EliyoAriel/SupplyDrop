package com.supplydrop.gui.config;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Config;
import com.supplydrop.config.ConfigKeys;
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

import java.util.ArrayList;
import java.util.List;

public class ConfigHologramPage implements Listener {

    private static final int SIZE = 27;
    private final Inventory inv;
    private boolean listenerRegistered = false;

    public ConfigHologramPage() {
        inv = Bukkit.createInventory(null, SIZE, "§9Hologram Settings");
        initializeItems();
    }

    public void initializeItems() {
        inv.clear();
        fillGlass();

        boolean enabled = ConfigKeys.isHologramEnabled();
        Material mat = enabled ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS;
        String status = enabled ? "§aON" : "§cOFF";
        inv.setItem(10, toggleItem(mat, "§eEnabled: " + status,
                "§7Show floating text above landed crates",
                "§7Displays template name, timer, team info"));

        List<String> lines = ConfigKeys.getHologramLines();
        inv.setItem(12, infoItem(Material.PAPER, "§eLines (" + lines.size() + ")",
                "§7Hologram text lines (top to bottom):",
                "§8" + String.join(" §8| ", lines.stream().map(l -> l.length() > 25 ? l.substring(0, 25) + "..." : l).toList()),
                "",
                "§7Placeholders: {template} {time} {team}",
                "§7Shift click: §eEdit lines via chat"));

        inv.setItem(16, infoItem(Material.CHEST, "§bPreview in Chat",
                "§7Click to show current lines"));

        inv.setItem(22, infoItem(Material.ARROW, "§cBack to Menu", "§7Click to return"));
    }

    private void fillGlass() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) { glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta); }
        for (int i = 0; i < SIZE; i++) {
            if (i == 10 || i == 12 || i == 16 || i == 22) continue;
            inv.setItem(i, glass);
        }
    }

    private ItemStack toggleItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            java.util.ArrayList<String> fullLore = new java.util.ArrayList<>();
            for (String line : lore) fullLore.add(line);
            fullLore.add("");
            fullLore.add("§7Click to toggle");
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
        if (slot == 22) { new ConfigMainMenu().openInventory(p); return; }

        if (slot == 10) {
            boolean current = ConfigKeys.isHologramEnabled();
            saveConfigValue(ConfigKeys.HOLOGRAM_ENABLED, !current);
            initializeItems();
            p.openInventory(inv);
            return;
        }

        if (slot == 12 && e.isShiftClick()) {
            p.closeInventory();
            List<String> newLines = new ArrayList<>();
            ChatHandler.send(p, "Enter hologram lines one per message. Type §e/done §7when finished (or §c/cancel§7):");
            plugin.setPendingStringInput(p.getUniqueId(), createLineCollector(plugin, p, newLines));
            return;
        }

        if (slot == 16) {
            List<String> lines = ConfigKeys.getHologramLines();
            ChatHandler.sendWithoutPrefix(p,
                    "&9━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "&f  &bHologram Preview\n" +
                    "&9━━━━━━━━━━━━━━━━━━━━━━━━");
            for (String line : lines) ChatHandler.sendWithoutPrefix(p, " " + line);
            ChatHandler.sendWithoutPrefix(p, "&9━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    private java.util.function.Consumer<String> createLineCollector(SupplyDrop plugin, Player p, List<String> lines) {
        return input -> {
            if (input.equalsIgnoreCase("done")) {
                saveConfigValue(ConfigKeys.HOLOGRAM_LINES, lines);
                ChatHandler.send(p, "Hologram lines §aset§7 (" + lines.size() + " lines).");
                Bukkit.getScheduler().runTask(plugin, () -> openInventory(p));
                return;
            }
            if (input.equalsIgnoreCase("cancel")) {
                ChatHandler.send(p, "Line editing &ccancelled&7.");
                Bukkit.getScheduler().runTask(plugin, () -> openInventory(p));
                return;
            }
            lines.add(input);
            ChatHandler.send(p, "§7Added line " + lines.size() + ": §f" + input + " §7Type §e/done §7to finish.");
            plugin.setPendingStringInput(p.getUniqueId(), createLineCollector(plugin, p, lines));
        };
    }

    private void saveConfigValue(String key, Object value) {
        Config config = SupplyDrop.getConfiguration();
        if (config != null) { config.getConfig().set(key, value); config.saveConfig(); }
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
