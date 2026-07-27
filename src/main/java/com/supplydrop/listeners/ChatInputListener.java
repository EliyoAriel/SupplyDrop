package com.supplydrop.listeners;

import com.supplydrop.SupplyDrop;
import com.supplydrop.gui.TemplateItemsGui;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.loot.LootEntry;
import com.supplydrop.loot.LootTable;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

/**
 * Listens for chat input when a player is in a pending state
 * (template creation or weight editing).
 */
public class ChatInputListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin == null || !plugin.isEnabled()) return;

        // Check weight edit first (more specific)
        SupplyDrop.WeightEditData weightData = plugin.consumePendingWeightEdit(player.getUniqueId());
        if (weightData != null) {
            e.setCancelled(true);
            handleWeightInput(player, weightData, e.getMessage().trim());
            return;
        }

        // Check template creation
        if (!plugin.consumePendingTemplateCreation(player.getUniqueId())) return;

        e.setCancelled(true);
        String input = e.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            ChatHandler.send(player, "Template creation &ccancelled&7.");
            return;
        }

        if (input.isBlank()) {
            ChatHandler.sendError(player, "Name cannot be blank. Type a template name or &c/cancel&7.");
            plugin.setPendingTemplateCreation(player.getUniqueId());
            return;
        }

        if (!input.matches("^[A-Za-z0-9_-]+$")) {
            ChatHandler.sendError(player, "Template names may only contain letters, numbers, underscores, and dashes.");
            plugin.setPendingTemplateCreation(player.getUniqueId());
            return;
        }

        if (PackageManager.has(input)) {
            ChatHandler.sendError(player, "A template named &c" + input + " &calready exists.");
            plugin.setPendingTemplateCreation(player.getUniqueId());
            return;
        }

        PackageManager.createPackage(input);
        ChatHandler.send(player, "Template &b" + input + " &acreated! Use &e/supplydrop templates &ato edit it.");
    }

    private void handleWeightInput(Player player, SupplyDrop.WeightEditData data, String input) {
        if (input.equalsIgnoreCase("cancel")) {
            ChatHandler.send(player, "Weight edit &ccancelled&7.");
            reopenItemsGui(player, data);
            return;
        }

        int newWeight;
        try {
            newWeight = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            ChatHandler.sendError(player, "Not a number. Type a weight value or &c/cancel&7.");
            SupplyDrop.getPluginInstance().setPendingWeightEdit(
                    player.getUniqueId(), data.templateName(), data.rarity(), data.filteredIndex(), data.page());
            return;
        }

        if (newWeight < 1) {
            ChatHandler.sendError(player, "Weight must be at least 1.");
            SupplyDrop.getPluginInstance().setPendingWeightEdit(
                    player.getUniqueId(), data.templateName(), data.rarity(), data.filteredIndex(), data.page());
            return;
        }

        Package pkg = PackageManager.get(data.templateName());
        if (pkg == null) {
            ChatHandler.sendError(player, "Template not found.");
            return;
        }

        LootTable table = pkg.getLootTable();
        List<LootEntry> entries = table.getEntriesByRarity(data.rarity());
        if (data.filteredIndex() < 0 || data.filteredIndex() >= entries.size()) {
            ChatHandler.sendError(player, "Item no longer exists.");
            return;
        }

        LootEntry entry = entries.get(data.filteredIndex());
        String itemName = entry.getItem().getType().name();
        entry.setWeight(newWeight);
        PackageManager.saveConfig();

        ChatHandler.send(player, "Weight for &f" + itemName + " &7set to &f" + newWeight
                + " &7in " + data.rarity().prefix() + data.rarity().key().toUpperCase());

        reopenItemsGui(player, data);
    }

    private void reopenItemsGui(Player player, SupplyDrop.WeightEditData data) {
        Bukkit.getScheduler().runTask(SupplyDrop.getPluginInstance(), () -> {
            Package pkg = PackageManager.get(data.templateName());
            if (pkg == null) return;
            TemplateItemsGui gui = new TemplateItemsGui(pkg, data.rarity());
            gui.setPage(data.page());
            Bukkit.getPluginManager().registerEvents(gui, SupplyDrop.getPluginInstance());
            gui.openInventory(player);
        });
    }
}
