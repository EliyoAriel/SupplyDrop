package com.supplydrop.packages;

import com.supplydrop.SupplyDrop;
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
import java.util.Set;

public class PackagesGui implements Listener {

    private final Inventory inv;
    private boolean listenerRegistered = false;

    public PackagesGui() {
        inv = Bukkit.createInventory(null, 27, "SupplyDrop Loot Tables");
        initializeItems();
    }

    public void initializeItems() {
        Set<String> names = PackageManager.getPackageNames();
        inv.clear();

        for (String name : names) {
            Package pkg = PackageManager.get(name);
            if (pkg == null) continue;
            int itemCount = pkg.getLootTable().size();
            inv.addItem(createGuiItem(Material.CHEST, name, 1,
                    "Loot table with §b" + itemCount + " §fitems"));
        }
    }

    public void openInventory(HumanEntity ent) {
        ensureListenerRegistered();
        ent.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        e.setCancelled(true);

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        String packageName = getDisplayName(clickedItem);
        if (packageName.isEmpty()) return;

        Package pkg = PackageManager.get(packageName);
        if (pkg == null) {
            ChatHandler.sendError(p, "Loot table &c" + packageName + " &cnot found.");
            return;
        }

        String info = pkg.getInfo().replace("\n", " §7| ");
        ChatHandler.send(p, "&b" + packageName + "&7: " + info);
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
            unregisterIfIdle();
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

    private void unregisterIfIdle() {
        if (!inv.getViewers().isEmpty()) return;
        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }
    }

    private static ItemStack createGuiItem(Material material, String name, int amount, String... lore) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String getDisplayName(ItemStack item) {
        if (item == null) return "";
        ItemMeta meta = item.getItemMeta();
        return (meta != null && meta.hasDisplayName()) ? meta.getDisplayName() : "";
    }
}
