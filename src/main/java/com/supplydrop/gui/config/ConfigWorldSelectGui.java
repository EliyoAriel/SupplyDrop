package com.supplydrop.gui.config;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Config;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.helpers.ChatHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
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
 * GUI for selecting a world from the server's world list.
 * Each world is displayed as a grass block with world name and environment.
 */
public class ConfigWorldSelectGui implements Listener {

    private static final int SIZE = 27;
    private final Inventory inv;
    private final String configKey;
    private final String currentWorld;
    private boolean listenerRegistered = false;

    public ConfigWorldSelectGui(String configKey, String currentWorld) {
        this.configKey = configKey;
        this.currentWorld = currentWorld;
        this.inv = Bukkit.createInventory(null, SIZE, "§9Select World");
        initializeItems();
    }

    public void initializeItems() {
        inv.clear();

        List<World> worlds = Bukkit.getWorlds();
        int slot = 0;
        for (World world : worlds) {
            if (slot >= 18) break; // Leave room for back button

            boolean isSelected = world.getName().equals(currentWorld);
            Material mat = isSelected ? Material.LIME_STAINED_GLASS : Material.GRASS_BLOCK;
            String prefix = isSelected ? "§a" : "§f";

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(prefix + world.getName());
                meta.setLore(List.of(
                        "§7Environment: §f" + world.getEnvironment().name(),
                        "§7Players: §f" + world.getPlayers().size(),
                        "§7Time: §f" + formatWorldTime(world.getTime()),
                        isSelected ? "" : "§7Click to select this world"
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slot++;
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cBack to Menu");
            backMeta.setLore(List.of("§7Click to return"));
            back.setItemMeta(backMeta);
        }
        inv.setItem(22, back);
    }

    private String formatWorldTime(long time) {
        int hours = (int) ((time / 1000 + 6) % 24);
        int minutes = (int) ((time % 1000) * 60 / 1000);
        String period = hours >= 12 ? "PM" : "AM";
        int displayHour = hours % 12;
        if (displayHour == 0) displayHour = 12;
        return displayHour + ":" + String.format("%02d", minutes) + " " + period;
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

        // Back button
        if (slot == 22) {
            ConfigMainMenu menu = new ConfigMainMenu();
            menu.openInventory(p);
            return;
        }

        // World selection
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        String worldName = meta.getDisplayName().replace("§a", "").replace("§f", "").trim();

        // Save selection
        Config config = SupplyDrop.getConfiguration();
        if (config != null) {
            config.getConfig().set(configKey, worldName);
            config.saveConfig();
        }

        ChatHandler.send(p, "World set to §b" + worldName + "§7.");

        // Reopen the previous page
        ConfigAutoDropPage page = new ConfigAutoDropPage();
        page.openInventory(p);
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
