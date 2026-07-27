package com.supplydrop.gui;

import com.supplydrop.SupplyDrop;
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
import java.util.Set;

/**
 * Level 1: Shows all loot table templates.
 * Click a template → opens TemplateRarityGui.
 * Click + button → prompts for new template name.
 * Click gear → opens TemplateSettingsGui for selected template.
 */
public class TemplateListGui implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int CREATE_SLOT = 40;
    private static final int INFO_SLOT = 49;
    private final Inventory inv;
    private boolean listenerRegistered = false;

    public TemplateListGui() {
        inv = Bukkit.createInventory(null, INVENTORY_SIZE, "§9SupplyDrop - Loot Tables");
        initializeItems();
    }

    private ItemStack glassPane() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); glass.setItemMeta(meta); }
        return glass;
    }

    public void initializeItems() {
        inv.clear();

        // Fill glass in row 4 (36-44) and row 5 (45-53)
        for (int i = 36; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, glassPane());
        }

        // Templates — slots 0 to 35 (6 rows of 9, leave last row for buttons)
        Set<String> names = PackageManager.getPackageNames();
        int slot = 0;
        for (String name : names) {
            if (slot >= 36) break;
            Package pkg = PackageManager.get(name);
            int itemCount = pkg != null ? pkg.getLootTable().size() : 0;

            Material mat = switch (itemCount) {
                case 0 -> Material.GRAY_SHULKER_BOX;
                case 1, 2, 3 -> Material.BARREL;
                default -> Material.CHEST;
            };

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + name);
                meta.setLore(List.of(
                        "§7Items: §f" + itemCount,
                        "§7Display: §f" + (pkg != null ? pkg.getDisplayName() : name),
                        "§7Fall: §f" + (pkg != null && pkg.getFallDuration() > 0 ? pkg.getFallDuration() + "s" : "global"),
                        "",
                        "§aClick to edit rarity tiers",
                        "§eShift-click for settings"
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slot++;
        }

        // Create new template button
        ItemStack createBtn = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createBtn.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§a+ Create New Template");
            createMeta.setLore(List.of("§7Click to create a new loot table"));
            createBtn.setItemMeta(createMeta);
        }
        inv.setItem(CREATE_SLOT, createBtn);

        // Info button
        ItemStack infoBtn = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoBtn.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§eLoot Table Info");
            infoMeta.setLore(List.of(
                    "§7Rarity weights determine drop chances:",
                    "§7Common §f= 60 | §aUncommon §f= 25",
                    "§7Rare §f= 10 | §6Legendary §f= 5",
                    "",
                    "§7Click a template to edit items.",
                    "§7Shift-click for template settings."
            ));
            infoBtn.setItemMeta(infoMeta);
        }
        inv.setItem(INFO_SLOT, infoBtn);
    }

    public void openInventory(HumanEntity ent) {
        ensureListenerRegistered();
        initializeItems();
        ent.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        if (e.getRawSlot() >= INVENTORY_SIZE) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (clicked.getType() == Material.EMERALD_BLOCK) {
            p.closeInventory();
            ChatHandler.send(p, "Type the new template name in chat. Type &c/cancel &7to cancel.");
            SupplyDrop.getPluginInstance().setPendingTemplateCreation(p.getUniqueId());
            return;
        }

        if (clicked.getType() == Material.BOOK) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String templateName = meta.getDisplayName().replace("§b", "").trim();

        Package pkg = PackageManager.get(templateName);
        if (pkg == null) {
            ChatHandler.sendError(p, "Template &c" + templateName + " &cnot found.");
            return;
        }

        // Shift-click → settings
        if (e.isShiftClick()) {
            TemplateSettingsGui settingsGui = new TemplateSettingsGui(pkg);
            Bukkit.getPluginManager().registerEvents(settingsGui, SupplyDrop.getPluginInstance());
            settingsGui.openInventory(p);
            return;
        }

        // Normal click → rarity editor
        TemplateRarityGui rarityGui = new TemplateRarityGui(pkg);
        Bukkit.getPluginManager().registerEvents(rarityGui, SupplyDrop.getPluginInstance());
        rarityGui.openInventory(p);
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
