package com.supplydrop.commands;

import com.supplydrop.Config;
import com.supplydrop.SupplyDrop;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import com.supplydrop.controllers.DropController;
import com.supplydrop.exceptions.SkyNotClearException;
import com.supplydrop.gui.PreviewGui;
import com.supplydrop.gui.TemplateListGui;
import com.supplydrop.gui.config.ConfigMainMenu;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.helpers.CrateManager;
import com.supplydrop.helpers.DatabaseManager;
import com.supplydrop.helpers.HistoryManager;
import com.supplydrop.helpers.NotificationManager;
import com.supplydrop.helpers.PermissionsHelper;
import com.supplydrop.loot.LootTable;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;

import com.supplydrop.Crate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SupplyDropCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        try {
            switch (args[0].toLowerCase()) {
                case "call" -> handleCallCommand(sender);
                case "spawn" -> handleSpawnCommand(sender, args);
                case "active" -> handleActiveCommand(sender);
                case "db" -> handleDbCommand(sender);
                case "preview" -> handlePreviewCommand(sender, args);
                case "history" -> handleHistoryCommand(sender, args);
                case "package" -> handlePackageSubcommand(sender, args);
                case "templates" -> handleTemplatesCommand(sender);
                case "reload" -> handleReloadSubcommand(sender);
                case "version" -> handleVersionSubcommand(sender);
                case "pause" -> handlePauseCommand(sender, true);
                case "resume" -> handlePauseCommand(sender, false);
                case "auto" -> handleAutoCommand(sender, args);
                case "subscribe" -> handleSubscribeCommand(sender, true);
                case "unsubscribe" -> handleSubscribeCommand(sender, false);
                case "toggle" -> handleToggleCommand(sender, args);
                case "delete" -> handleDeleteCommand(sender, args);
                case "config" -> handleConfigCommand(sender);
                default -> handleCallNamedCommand(sender, args);
            }
        } catch (Exception e) {
            ChatHandler.sendError(sender, "An error occurred: " + e.getMessage());
        }
        return true;
    }

    /**
     * /supplydrop call - calls a random supply drop at the player's location
     */
    private void handleCallCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatHandler.sendError(sender, "Must be a player to call a supply drop.");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        try {
            DropController.callRandomDrop(player);
            ChatHandler.send(player, "Supply drop incoming!");
        } catch (SkyNotClearException e) {
            ChatHandler.sendError(player, "Sky must be clear above your location to call a supply drop.");
        }
    }

    /**
     * /supplydrop active - list all active (unopened) supply crates
     */
    private void handleActiveCommand(CommandSender sender) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        List<Crate> activeCrates = CrateManager.getActiveCrates();
        if (activeCrates.isEmpty()) {
            ChatHandler.send(sender, "&7No active supply crates in the world.");
            return;
        }

        int maxActive = ConfigKeys.getCrateMaxActive();
        String limitStr = maxActive > 0 ? " &7/&e" + maxActive : "";
        ChatHandler.send(sender, "&bActive Supply Crates &7(" + activeCrates.size() + limitStr + "&7):");
        for (int i = 0; i < activeCrates.size(); i++) {
            Crate crate = activeCrates.get(i);
            String name = crate.getDisplayName() != null ? crate.getDisplayName() : "Unknown";
            String shortId = crate.getShortId();
            Location landed = crate.getLandedLocation();
            if (landed != null && sender instanceof Player player) {
                String worldName = landed.getWorld().getName();
                int x = landed.getBlockX();
                int y = landed.getBlockY();
                int z = landed.getBlockZ();
                String cmd = "/tp " + player.getName() + " " + x + " " + y + " " + z;
                String hoverText = "Click to teleport to " + name;

                net.kyori.adventure.text.Component msg = net.kyori.adventure.text.Component.empty()
                    .append(net.kyori.adventure.text.Component.text(" "))
                    .append(net.kyori.adventure.text.Component.text("[" + shortId + "]", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY))
                    .append(net.kyori.adventure.text.Component.text(" " + name, net.kyori.adventure.text.format.NamedTextColor.GOLD))
                    .append(net.kyori.adventure.text.Component.text(" - ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                    .append(net.kyori.adventure.text.Component.text(worldName + " " + x + " " + y + " " + z, net.kyori.adventure.text.format.NamedTextColor.WHITE)
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                            net.kyori.adventure.text.Component.text(hoverText, net.kyori.adventure.text.format.NamedTextColor.GOLD)))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(cmd)));
                player.sendMessage(msg);
            } else {
                Location fallback = landed != null ? landed : crate.getDropLocation();
                if (fallback != null && sender instanceof Player player) {
                    String worldName = fallback.getWorld().getName();
                    int x = fallback.getBlockX();
                    int z = fallback.getBlockZ();
                    int y = fallback.getWorld().getHighestBlockYAt(x, z) + 1;
                    String cmd = "/tp " + player.getName() + " " + x + " " + y + " " + z;
                    String status = landed != null ? "" : " §e[Falling]";
                    String hoverText = "Click to teleport to " + name;

                    net.kyori.adventure.text.Component msg = net.kyori.adventure.text.Component.empty()
                        .append(net.kyori.adventure.text.Component.text(" "))
                        .append(net.kyori.adventure.text.Component.text("[" + shortId + "]", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY))
                        .append(net.kyori.adventure.text.Component.text(" " + name, net.kyori.adventure.text.format.NamedTextColor.GOLD))
                        .append(net.kyori.adventure.text.Component.text(status, net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                        .append(net.kyori.adventure.text.Component.text(" - ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                        .append(net.kyori.adventure.text.Component.text(worldName + " " + x + " " + z, net.kyori.adventure.text.format.NamedTextColor.WHITE)
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                net.kyori.adventure.text.Component.text(hoverText, net.kyori.adventure.text.format.NamedTextColor.GOLD)))
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(cmd)));
                    player.sendMessage(msg);
                } else {
                    String loc = fallback != null
                            ? fallback.getWorld().getName() + " " + fallback.getBlockX() + " " + fallback.getBlockZ()
                            : "Unknown";
                    ChatHandler.send(sender, " &8[" + shortId + "] &e" + name + " &7- &f" + loc);
                }
            }
        }
    }

    /**
     * /supplydrop db - show database status
     */
    private void handleDbCommand(CommandSender sender) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        int dbCount = DatabaseManager.getCount();
        int memoryCount = CrateManager.getTotalCrateCount();
        int fallingCount = CrateManager.getFallingCrateCount();
        int landedCount = memoryCount - fallingCount;

        ChatHandler.send(sender, "&9━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatHandler.send(sender, "&f  &bSupplyDrop Database");
        ChatHandler.send(sender, "&9━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatHandler.send(sender, " &7Storage: &fSQLite (async)");
        ChatHandler.send(sender, " &7DB records: &b" + dbCount);
        ChatHandler.send(sender, " &7In-memory: &b" + memoryCount);
        ChatHandler.send(sender, "   &7Falling: &e" + fallingCount);
        ChatHandler.send(sender, "   &7Landed: &a" + landedCount);
        ChatHandler.send(sender, "&9━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * /supplydrop preview <template> - open a virtual chest showing all items in a template
     */
    private void handlePreviewCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatHandler.sendError(sender, "Must be a player to use this command.");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        if (args.length < 2) {
            ChatHandler.sendError(sender, "Usage: /supplydrop preview <template>");
            return;
        }

        String templateName = args[1];
        if (!PackageManager.has(templateName)) {
            ChatHandler.sendError(player, "Template &c" + templateName + " &cnot found.");
            return;
        }

        Package pkg = PackageManager.get(templateName);
        LootTable lootTable = pkg.getLootTable();
        if (lootTable == null || lootTable.size() == 0) {
            ChatHandler.sendError(player, "Template &c" + templateName + " &chas no items.");
            return;
        }

        PreviewGui gui = new PreviewGui(templateName, lootTable);
        gui.open(player);
    }

    /**
     * /supplydrop spawn <template> [team] [trap] [wave:<count>] [players:<count>]
     * Bypass spawn with forced conditions.
     */
    private void handleSpawnCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatHandler.sendError(sender, "Must be a player to use this command.");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        if (args.length < 2) {
            ChatHandler.sendError(sender, "Usage: /supplydrop spawn <template> [team] [trap] [wave:<count>] [players:<count>] [expiry:<ticks>]");
            return;
        }

        String templateName = args[1];
        if (!PackageManager.has(templateName)) {
            ChatHandler.sendError(player, "Template &c" + templateName + " &cnot found.");
            return;
        }

        Package pkg = PackageManager.get(templateName);
        boolean forceTeam = false;
        boolean forceTrap = false;
        int waveCount = 1;
        int teamPlayers = 2;
        int expiryTicks = ConfigKeys.getCrateExpiry();

        // Parse optional conditions
        for (int i = 2; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.equals("team")) {
                forceTeam = true;
            } else if (arg.equals("trap")) {
                forceTrap = true;
            } else if (arg.startsWith("wave:")) {
                try {
                    waveCount = Math.max(1, Integer.parseInt(arg.substring(5)));
                } catch (NumberFormatException e) {
                    ChatHandler.sendError(sender, "Invalid wave count: &c" + arg.substring(5));
                    return;
                }
            } else if (arg.startsWith("players:")) {
                try {
                    teamPlayers = Math.max(2, Integer.parseInt(arg.substring(8)));
                } catch (NumberFormatException e) {
                    ChatHandler.sendError(sender, "Invalid player count: &c" + arg.substring(8));
                    return;
                }
            } else if (arg.startsWith("expiry:")) {
                try {
                    expiryTicks = Math.max(0, Integer.parseInt(arg.substring(7)));
                } catch (NumberFormatException e) {
                    ChatHandler.sendError(sender, "Invalid expiry ticks: &c" + arg.substring(7));
                    return;
                }
            }
        }

        try {
            if (waveCount > 1) {
                DropController.spawnWaveForced(pkg, player, waveCount, forceTeam, forceTrap, teamPlayers, expiryTicks);
                ChatHandler.send(player, "Spawned &b" + waveCount + " &e" + templateName + " &bcrate(s)!" +
                        (forceTeam ? " &7[Team: " + teamPlayers + " players]" : "") +
                        (forceTrap ? " &7[Trap]" : "") +
                        (expiryTicks > 0 ? " &7[Expiry: " + expiryTicks + "t]" : " &7[No Expiry]"));
            } else {
                DropController.spawnForced(pkg, player, forceTeam, forceTrap, teamPlayers, expiryTicks);
                ChatHandler.send(player, "Spawned &b" + templateName + " &bcrate!" +
                        (forceTeam ? " &7[Team: " + teamPlayers + " players]" : "") +
                        (forceTrap ? " &7[Trap]" : "") +
                        (expiryTicks > 0 ? " &7[Expiry: " + expiryTicks + "t]" : " &7[No Expiry]"));
            }
        } catch (SkyNotClearException e) {
            ChatHandler.sendError(player, "Sky must be clear above your location.");
        }
    }

    /**
     * /supplydrop <templateName> - calls a specific loot table template
     */
    private void handleCallNamedCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatHandler.sendError(sender, "Must be a player to use this command.");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        String templateName = args[0];
        if (!PackageManager.has(templateName)) {
            ChatHandler.sendError(player, "Loot table template &c" + templateName + " &cnot found. Use &e/supplydrop templates &cto see available templates.");
            return;
        }

        Package pkg = PackageManager.get(templateName);
        try {
            DropController.callNamedDrop(pkg, player);
            ChatHandler.send(player, "Supply drop &b" + templateName + " &aincoming!");
        } catch (SkyNotClearException e) {
            ChatHandler.sendError(player, "Sky must be clear above your location to call a supply drop.");
        }
    }

    // ─── /supplydrop history ───────────────────────────────────────
    // Usage: /supplydrop history [page] [event:<type>] [player:<name>]

    private void handleHistoryCommand(CommandSender sender, String[] args) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        int page = 1;
        int perPage = 10;
        String filterEvent = null;
        String filterPlayer = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.startsWith("event:")) {
                filterEvent = arg.substring(6);
            } else if (arg.startsWith("player:")) {
                filterPlayer = arg.substring(7);
            } else {
                try {
                    page = Math.max(1, Integer.parseInt(arg));
                } catch (NumberFormatException e) {
                    ChatHandler.sendError(sender, "Invalid page number: &c" + arg);
                    return;
                }
            }
        }

        int total = HistoryManager.count(filterEvent, filterPlayer);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / perPage));
        page = Math.min(page, totalPages);

        List<HistoryManager.HistoryRecord> history = HistoryManager.query(page, perPage, filterEvent, filterPlayer);

        StringBuilder header = new StringBuilder();
        header.append("&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        header.append("&f  &b&lSupplyDrop History &7(Page &f").append(page).append("&7/&f").append(totalPages).append("&7, &f").append(total).append(" &7total)\n");
        header.append("&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        if (filterEvent != null || filterPlayer != null) {
            header.append("&7  Filters: ");
            if (filterEvent != null) header.append("&eevent:&f").append(filterEvent).append(" ");
            if (filterPlayer != null) header.append("&eplayer:&f").append(filterPlayer);
            header.append("\n");
        }

        if (history.isEmpty()) {
            header.append("&7  No history records found.\n");
        } else {
            for (HistoryManager.HistoryRecord record : history) {
                String eventColor = switch (record.event()) {
                    case "SPAWN" -> "&aSPAWN";
                    case "OPEN" -> "&bOPEN";
                    case "TRAP" -> "&cTRAP";
                    case "EXPIRE" -> "&4EXPIRE";
                    case "DESTROY" -> "&eDESTROY";
                    default -> "&7" + record.event();
                };
                String template = record.template() != null ? record.template() : "?";
                String player = record.player() != null ? " &7by &f" + record.player() : "";
                String extra = record.extra() != null ? " &8(" + record.extra() + ")" : "";
                String loc = record.world() != null
                        ? " &8@ " + record.world() + " " + record.x() + " " + record.z()
                        : "";

                header.append(" ").append(eventColor).append(" &7").append(template)
                        .append(player).append(extra).append(loc).append("\n");
            }
        }

        header.append("&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        header.append("&7  &e/supplydrop history <page> &8- &7next page\n");
        header.append("&7  &e/supplydrop history event:<type> &8- &7filter by event\n");
        header.append("&7  &e/supplydrop history player:<name> &8- &7filter by player\n");
        header.append("&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        ChatHandler.sendWithoutPrefix(sender, header.toString());
    }

    private void handlePackageSubcommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatHandler.sendError(sender, "Must specify a template name.");
            ChatHandler.sendError(sender, "Example: /supplydrop package <name>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> handlePackageCreate(sender, args);
            case "delete" -> handlePackageDelete(sender, args);
            case "setdisplay" -> handlePackageSetDisplay(sender, args);
            default -> handlePackageInfo(sender, args[1]);
        }
    }

    private void handlePackageInfo(CommandSender sender, String packageName) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        Package pkg = PackageManager.get(packageName);
        if (pkg == null) {
            ChatHandler.sendError(sender, "Template &c" + packageName + " &cnot found.");
            return;
        }

        ChatHandler.sendWithoutPrefix(sender,
                "&9━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&f  Loot Table: &b" + packageName + "\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                pkg.getInfo());
    }

    private void handlePackageCreate(CommandSender sender, String[] args) {
        if (args.length != 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop package create <name>");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        String templateName = args[2].trim();
        if (templateName.isBlank()) {
            ChatHandler.sendError(sender, "You must provide a name for the template.");
            return;
        }
        if (!templateName.matches("^[A-Za-z0-9_-]+$")) {
            ChatHandler.sendError(sender, "Template names may only contain letters, numbers, underscores, and dashes.");
            return;
        }
        if (PackageManager.has(templateName)) {
            ChatHandler.sendError(sender, "A template named &c" + templateName + " &calready exists.");
            return;
        }

        PackageManager.createPackage(templateName);
        ChatHandler.send(sender, "Loot table template &b" + templateName + " &awas created. Edit packages.yml to add items.");
    }

    private void handlePackageDelete(CommandSender sender, String[] args) {
        if (args.length != 3) {
            ChatHandler.sendError(sender, "Need to specify a template name to delete.");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        String templateName = args[2];
        if (!PackageManager.has(templateName)) {
            ChatHandler.sendError(sender, "Template &c" + templateName + " &cnot found.");
            return;
        }

        PackageManager.deletePackage(templateName);
        ChatHandler.send(sender, "&b" + templateName + " &awas successfully deleted.");
    }

    private void handlePackageSetDisplay(CommandSender sender, String[] args) {
        if (args.length < 4) {
            ChatHandler.sendError(sender, "Usage: /supplydrop package setdisplay <template> <display name>");
            ChatHandler.sendError(sender, "Use &e/supplydrop package setdisplay <template> clear &cto reset.");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        String templateName = args[2];
        if (!PackageManager.has(templateName)) {
            ChatHandler.sendError(sender, "Template &c" + templateName + " &cnot found.");
            return;
        }

        // Build display name from remaining args (join with spaces)
        StringBuilder displayName = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) displayName.append(" ");
            displayName.append(args[i]);
        }
        String value = displayName.toString().trim();

        if (value.equalsIgnoreCase("clear")) {
            PackageManager.setDisplayName(templateName, null);
            ChatHandler.send(sender, "Display name for &b" + templateName + " &areset to default (template name).");
        } else {
            PackageManager.setDisplayName(templateName, value);
            ChatHandler.send(sender, "Display name for &b" + templateName + " &aset to &e" + value + "&7.");
        }
    }

    private void handleTemplatesCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatHandler.sendError(sender, "Must be a player to open the templates GUI.");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        TemplateListGui gui = new TemplateListGui();
        gui.openInventory(player);
    }

    private void handleReloadSubcommand(CommandSender sender) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        Config config = SupplyDrop.getConfiguration();
        if (config == null || plugin == null || !plugin.isEnabled()) {
            ChatHandler.sendError(sender, "Reload unavailable while plugin is shutting down.");
            return;
        }

        config.reloadConfig();
        if (!PackageManager.reload()) {
            ChatHandler.sendError(sender, "Reload failed because packages configuration is unavailable.");
            return;
        }
        plugin.restartAutoDropScheduler();
        ChatHandler.send(sender, "Configuration, loot tables, and scheduler reloaded.");
    }

    private void handlePauseCommand(CommandSender sender, boolean pause) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        Config config = SupplyDrop.getConfiguration();
        if (config == null || plugin == null || !plugin.isEnabled()) {
            ChatHandler.sendError(sender, "Cannot modify auto-drop state right now.");
            return;
        }

        config.getConfig().set(ConfigKeys.AUTO_DROP_PAUSED, pause);
        config.saveConfig();

        if (pause) {
            ChatHandler.send(sender, "Auto-drop scheduler &cpaused&7.");
        } else {
            ChatHandler.send(sender, "Auto-drop scheduler &aresumed&7.");
        }
        plugin.restartAutoDropScheduler();
    }

    private void handleSubscribeCommand(CommandSender sender, boolean subscribe) {
        if (!(sender instanceof Player player)) {
            ChatHandler.sendError(sender, "Must be a player to use this command.");
            return;
        }

        java.sql.Connection conn = HistoryManager.getConnection();
        if (conn == null) {
            ChatHandler.sendError(sender, "Database not available.");
            return;
        }

        NotificationManager.setSubscribed(conn, player.getUniqueId(), subscribe);

        if (subscribe) {
            NotificationManager.notifyPlayer(player, "&aSubscribed &7to supply drop notifications.");
        } else {
            NotificationManager.notifyPlayer(player, "&cUnsubscribed &7from supply drop notifications.");
        }
    }

    private void handleToggleCommand(CommandSender sender, String[] args) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        if (args.length < 2) {
            ChatHandler.sendError(sender, "Usage: /supplydrop toggle <hologram|announce|notify>");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        if (config == null) {
            ChatHandler.sendError(sender, "Cannot modify config right now.");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "hologram" -> {
                boolean current = ConfigKeys.isHologramEnabled();
                config.getConfig().set(ConfigKeys.HOLOGRAM_ENABLED, !current);
                config.saveConfig();
                ChatHandler.send(sender, "Hologram &b" + (!current ? "&aenabled" : "&cdisabled") + "&7.");
            }
            case "announce" -> {
                boolean current = ConfigKeys.isAnnouncementEnabled();
                config.getConfig().set(ConfigKeys.ANNOUNCE_ENABLED, !current);
                config.saveConfig();
                ChatHandler.send(sender, "Announcements &b" + (!current ? "&aenabled" : "&cdisabled") + "&7.");
            }
            case "notify" -> {
                boolean current = ConfigKeys.isNotificationDefaultSubscribe();
                config.getConfig().set(ConfigKeys.NOTIFICATION_DEFAULT_SUBSCRIBE, !current);
                config.saveConfig();
                ChatHandler.send(sender, "Notification default &b" + (!current ? "&aenabled" : "&cdisabled") + "&7.");
            }
            default -> ChatHandler.sendError(sender, "Unknown toggle: &c" + args[1] + "&7. Use hologram, announce, or notify.");
        }
    }

    private void handleDeleteCommand(CommandSender sender, String[] args) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        List<Crate> activeCrates = CrateManager.getActiveCrates();
        if (activeCrates.isEmpty()) {
            ChatHandler.send(sender, "&7No active supply crates to delete.");
            return;
        }

        if (args.length < 2) {
            ChatHandler.sendError(sender, "Usage: /supplydrop delete <id|number|all>");
            ChatHandler.sendError(sender, "Use &e/supplydrop active &cto see IDs.");
            return;
        }

        String input = args[1].toLowerCase();

        // /supplydrop delete all
        if (input.equals("all")) {
            int count = activeCrates.size();
            for (Crate crate : new ArrayList<>(activeCrates)) {
                Location loc = crate.getLandedLocation();
                crate.destroy();
                DatabaseManager.remove(loc != null ? loc : crate.getDropLocation());
            }
            ChatHandler.send(sender, "&cDeleted &e" + count + " &csupplydrop(s).");
            return;
        }

        // Try short UUID match first
        Crate matched = null;
        for (Crate crate : activeCrates) {
            if (crate.getShortId().equals(input)) {
                matched = crate;
                break;
            }
        }

        // Fallback: try index number
        if (matched == null) {
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < activeCrates.size()) {
                    matched = activeCrates.get(index);
                }
            } catch (NumberFormatException ignored) {}
        }

        if (matched == null) {
            ChatHandler.sendError(sender, "No crate found with ID or number &c" + input + "&c.");
            ChatHandler.sendError(sender, "Use &e/supplydrop active &cto see IDs.");
            return;
        }

        Location loc = matched.getLandedLocation();
        String name = matched.getDisplayName() != null ? matched.getDisplayName() : "Unknown";
        matched.destroy();
        DatabaseManager.remove(loc != null ? loc : matched.getDropLocation());
        ChatHandler.send(sender, "&cDeleted &e" + name + " &c(" + matched.getShortId() + ").");
    }

    private void handleVersionSubcommand(CommandSender sender) {
        String version = SupplyDrop.getPluginVersion() != null ? SupplyDrop.getPluginVersion() : "unknown";
        ChatHandler.sendWithoutPrefix(sender,
                "&9━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&f  SupplyDrop &bv" + version + "\n" +
                "&f  Paper API &b1.21+\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void handleConfigCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatHandler.sendError(sender, "Must be a player to use the config GUI.");
            return;
        }
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }

        ConfigMainMenu gui = new ConfigMainMenu();
        gui.openInventory(player);
    }

    // ─── /supplydrop auto ────────────────────────────────────────────

    private void handleAutoCommand(CommandSender sender, String[] args) {
        if (!PermissionsHelper.isAdmin(sender)) {
            ChatHandler.sendError(sender, "You must have &bsupplydrop.admin &cpermission to do this.");
            return;
        }
        if (args.length < 2) {
            handleAutoHelp(sender);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "status"        -> handleAutoStatus(sender);
            case "enable"        -> handleAutoToggle(sender, true);
            case "disable"       -> handleAutoToggle(sender, false);
            case "pause"         -> handleAutoPause(sender, true);
            case "resume"        -> handleAutoPause(sender, false);
            case "interval"      -> handleAutoInterval(sender, args);
            case "interval-min"  -> handleAutoIntervalMin(sender, args);
            case "interval-max"  -> handleAutoIntervalMax(sender, args);
            case "world"         -> handleAutoWorld(sender, args);
            case "radius"        -> handleAutoRadius(sender, args);
            case "templates"     -> handleAutoTemplates(sender, args);
            case "announce"      -> handleAutoAnnounce(sender, args);
            case "announce-delay"-> handleAutoAnnounceDelay(sender, args);
            case "announce-actionbar" -> handleAutoAnnounceActionbar(sender, args);
            case "coord-reveal-delay" -> handleAutoCoordRevealDelay(sender, args);
            case "wave-count"    -> handleAutoWaveCount(sender, args);
            case "expiry"        -> handleAutoExpiry(sender, args);
            case "team-crate"    -> handleAutoTeamCrate(sender, args);
            case "trap-chance"   -> handleAutoTrapChance(sender, args);
            case "trap-mobs"     -> handleAutoTrapMobs(sender, args);
            case "loot-scaling"  -> handleAutoLootScaling(sender, args);
            case "fall-duration" -> handleAutoFallDuration(sender, args);
            default -> handleAutoHelp(sender);
        }
    }

    private void handleAutoHelp(CommandSender sender) {
        ChatHandler.sendWithoutPrefix(sender,
                "&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&f  &b/supplydrop auto &8- &7Auto-drop config\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&e status &8- &7Show current settings\n" +
                "&e enable &8- &7Enable auto-drop\n" +
                "&e disable &8- &7Disable auto-drop\n" +
                "&e pause &8- &7Pause scheduler\n" +
                "&e resume &8- &7Resume scheduler\n" +
                "&e interval <ticks> &8- &7Set fixed interval\n" +
                "&e interval-min <ticks> &8- &7Set min interval\n" +
                "&e interval-max <ticks> &8- &7Set max interval\n" +
                "&e world <name> &8- &7Set target world\n" +
                "&e radius <blocks> &8- &7Set random radius\n" +
                "&e templates &8- &7List templates\n" +
                "&e templates add|remove|set &8- &7Manage templates\n" +
                "&e announce <true|false> &8- &7Toggle announce\n" +
                "&e announce-delay <ticks> &8- &7Set announce delay\n" +
                "&e announce-actionbar <true|false> &8- &7Use actionbar\n" +
                "&e coord-reveal-delay <ticks> &8- &7Coord reveal delay\n" +
                "&e wave-count <count> &8- &7Crates per drop\n" +
                "&e expiry <ticks> &8- &7Crate auto-destroy\n" +
                "&e fall-duration <seconds> &8- &7Fall duration\n" +
                "&e team-crate <percent> &8- &7Team crate chance\n" +
                "&e trap-chance <percent> &8- &7Trap crate chance\n" +
                "&e trap-mobs <type> &8- &7Set trap mob type\n" +
                "&e loot-scaling <true|false> &8- &7Loot scaling\n" +
                "&e fall-duration <seconds> &8- &7Fall duration\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void handleAutoStatus(CommandSender sender) {
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return;
        var fc = config.getConfig();

        boolean enabled = ConfigKeys.isAutoDropEnabled();
        boolean paused  = ConfigKeys.isAutoDropPaused();
        int interval    = ConfigKeys.getAutoDropInterval();
        String world    = ConfigKeys.getAutoDropWorld();
        int radius      = ConfigKeys.getAutoDropRandomRadius();
        boolean announce = ConfigKeys.isAutoDropAnnounce();
        int delay       = ConfigKeys.getAutoDropAnnounceDelay();
        int fallDur     = ConfigKeys.getAutoDropFallDuration();

        List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
        StringBuilder tplStr = new StringBuilder();
        for (int i = 0; i < templates.size(); i++) {
            TemplateWeight tw = templates.get(i);
            tplStr.append("&b").append(tw.name()).append(" &7(w: ").append(tw.weight()).append(")");
            if (i < templates.size() - 1) tplStr.append("&8, ");
        }

        ChatHandler.sendWithoutPrefix(sender,
                "&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&f  &b&lAuto-Drop Status\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&e Enabled:     &f" + (enabled ? "&aYes" : "&cNo") + "\n" +
                "&e Paused:      &f" + (paused ? "&cYes" : "&aNo") + "\n" +
                "&e Interval:    &f" + interval + " ticks (" + formatTime(interval) + ")\n" +
                "&e World:       &f" + world + "\n" +
                "&e Radius:      &f" + radius + " blocks\n" +
                "&e Templates:   &f" + (tplStr.isEmpty() ? "&cNone" : tplStr) + "\n" +
                "&e Announce:    &f" + (announce ? "&aYes" : "&cNo") + "\n" +
                "&e Ann. Delay:  &f" + delay + " ticks\n" +
                "&e Fall Dur:    &f" + (fallDur > 0 ? fallDur + "s" : "disabled") + "\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String formatTime(int ticks) {
        int seconds = ticks / 20;
        if (seconds < 60) return seconds + "s";
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) return minutes + "m " + seconds + "s";
        int hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }

    private void handleAutoToggle(CommandSender sender, boolean enable) {
        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_ENABLED, enable);
        config.saveConfig();

        if (enable) {
            ChatHandler.send(sender, "Auto-drop &aenabled&7.");
        } else {
            ChatHandler.send(sender, "Auto-drop &cdisabled&7.");
        }
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoPause(CommandSender sender, boolean pause) {
        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_PAUSED, pause);
        config.saveConfig();

        if (pause) {
            ChatHandler.send(sender, "Auto-drop scheduler &cpaused&7.");
        } else {
            ChatHandler.send(sender, "Auto-drop scheduler &aresumed&7.");
        }
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoInterval(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto interval <ticks>");
            ChatHandler.sendError(sender, "Minimum: 200 ticks (10 seconds).");
            return;
        }
        int ticks;
        try {
            ticks = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (ticks < 200) {
            ChatHandler.sendError(sender, "Interval must be at least &b200 &cticks (10 seconds).");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_INTERVAL, ticks);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop interval set to &b" + ticks + " ticks &7(" + formatTime(ticks) + ")&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoWorld(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto world <worldName>");
            return;
        }
        String worldName = args[2];
        if (Bukkit.getWorld(worldName) == null) {
            ChatHandler.sendError(sender, "World &c" + worldName + " &cnot found on this server.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_WORLD, worldName);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop world set to &b" + worldName + "&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoRadius(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto radius <blocks>");
            return;
        }
        int radius;
        try {
            radius = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (radius < 1) {
            ChatHandler.sendError(sender, "Radius must be at least &b1 &cblock.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_RANDOM_RADIUS, radius);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop radius set to &b" + radius + " &cblocks&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoTemplates(CommandSender sender, String[] args) {
        if (args.length < 3) {
            handleAutoTemplatesList(sender);
            return;
        }

        switch (args[2].toLowerCase()) {
            case "add"    -> handleAutoTemplateAdd(sender, args);
            case "remove" -> handleAutoTemplateRemove(sender, args);
            case "set"    -> handleAutoTemplateSet(sender, args);
            default       -> handleAutoTemplatesList(sender);
        }
    }

    private void handleAutoTemplatesList(CommandSender sender) {
        List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
        if (templates.isEmpty()) {
            ChatHandler.sendError(sender, "No auto-drop templates configured.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < templates.size(); i++) {
            TemplateWeight tw = templates.get(i);
            sb.append("&b").append(tw.name()).append(" &7(weight: ").append(tw.weight()).append(")");
            if (i < templates.size() - 1) sb.append("\n");
        }
        ChatHandler.sendWithoutPrefix(sender,
                "&9━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&f  &b&lAuto-Drop Templates\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                sb + "\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void handleAutoTemplateAdd(CommandSender sender, String[] args) {
        if (args.length < 5) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto templates add <name> <weight>");
            return;
        }
        String name = args[3];
        if (!PackageManager.has(name)) {
            ChatHandler.sendError(sender, "Template &c" + name + " &cnot found. Create it first.");
            return;
        }
        int weight;
        try {
            weight = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[4] + " &cis not a valid number.");
            return;
        }
        if (weight < 1) {
            ChatHandler.sendError(sender, "Weight must be at least &b1&c.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_TEMPLATES + "." + name + ".weight", weight);
        config.saveConfig();
        ChatHandler.send(sender, "Added &b" + name + " &7to auto-drop templates with weight &b" + weight + "&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoTemplateRemove(CommandSender sender, String[] args) {
        if (args.length < 4) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto templates remove <name>");
            return;
        }
        String name = args[3];
        List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
        if (templates.stream().noneMatch(tw -> tw.name().equalsIgnoreCase(name))) {
            ChatHandler.sendError(sender, "Template &c" + name + " &cis not in the auto-drop list.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_TEMPLATES + "." + name, null);
        config.saveConfig();
        ChatHandler.send(sender, "Removed &b" + name + " &7from auto-drop templates.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoTemplateSet(CommandSender sender, String[] args) {
        if (args.length < 5) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto templates set <name> <weight>");
            return;
        }
        String name = args[3];
        List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
        if (templates.stream().noneMatch(tw -> tw.name().equalsIgnoreCase(name))) {
            ChatHandler.sendError(sender, "Template &c" + name + " &cis not in the auto-drop list. Use &e/supplydrop auto templates add &cfirst.");
            return;
        }
        int weight;
        try {
            weight = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[4] + " &cis not a valid number.");
            return;
        }
        if (weight < 1) {
            ChatHandler.sendError(sender, "Weight must be at least &b1&c.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_TEMPLATES + "." + name + ".weight", weight);
        config.saveConfig();
        ChatHandler.send(sender, "Set &b" + name + " &7weight to &b" + weight + "&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoAnnounce(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto announce <true|false>");
            return;
        }
        String value = args[2].toLowerCase();
        if (!value.equals("true") && !value.equals("false")) {
            ChatHandler.sendError(sender, "Value must be &btrue &cor &cfalse&c.");
            return;
        }
        boolean announce = value.equals("true");

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_ANNOUNCE, announce);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop announce set to &b" + (announce ? "enabled" : "disabled") + "&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoAnnounceDelay(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto announce-delay <ticks>");
            return;
        }
        int delay;
        try {
            delay = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (delay < 20) {
            ChatHandler.sendError(sender, "Delay must be at least &b20 &cticks (1 second).");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_ANNOUNCE_DELAY, delay);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop announce delay set to &b" + delay + " ticks&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoIntervalMin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto interval-min <ticks>");
            return;
        }
        int ticks;
        try {
            ticks = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (ticks < 200) {
            ChatHandler.sendError(sender, "Minimum interval must be at least &b200 &cticks.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_INTERVAL_MIN, ticks);
        config.saveConfig();
        ChatHandler.send(sender, "Min interval set to &b" + ticks + " ticks&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoIntervalMax(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto interval-max <ticks>");
            return;
        }
        int ticks;
        try {
            ticks = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (ticks < 200) {
            ChatHandler.sendError(sender, "Maximum interval must be at least &b200 &cticks.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_INTERVAL_MAX, ticks);
        config.saveConfig();
        ChatHandler.send(sender, "Max interval set to &b" + ticks + " ticks&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoAnnounceActionbar(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto announce-actionbar <true|false>");
            return;
        }
        String value = args[2].toLowerCase();
        if (!value.equals("true") && !value.equals("false")) {
            ChatHandler.sendError(sender, "Value must be &btrue &cor &cfalse&c.");
            return;
        }
        boolean enabled = value.equals("true");

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_ANNOUNCE_ACTIONBAR, enabled);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop announce actionbar set to &b" + (enabled ? "enabled" : "disabled") + "&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoCoordRevealDelay(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto coord-reveal-delay <ticks>");
            return;
        }
        int delay;
        try {
            delay = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (delay < 0) {
            ChatHandler.sendError(sender, "Delay must be at least &b0 &cticks.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_COORD_REVEAL_DELAY, delay);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop coord reveal delay set to &b" + delay + " ticks&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoWaveCount(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto wave-count <count>");
            return;
        }
        int count;
        try {
            count = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (count < 1) {
            ChatHandler.sendError(sender, "Wave count must be at least &b1&c.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_WAVE_COUNT, count);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop wave count set to &b" + count + "&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoExpiry(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto expiry <ticks>");
            ChatHandler.sendError(sender, "Use &e0 &cto disable expiry.");
            return;
        }
        int ticks;
        try {
            ticks = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (ticks < 0) {
            ChatHandler.sendError(sender, "Expiry must be at least &b0 &cticks.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_EXPIRY, ticks);
        config.saveConfig();
        if (ticks == 0) {
            ChatHandler.send(sender, "Auto-drop crate expiry &cdisabled&7.");
        } else {
            ChatHandler.send(sender, "Auto-drop crate expiry set to &b" + ticks + " ticks&7.");
        }
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoTeamCrate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto team-crate <percent>");
            ChatHandler.sendError(sender, "Use &e0 &cto disable team crate chance.");
            return;
        }
        int chance;
        try {
            chance = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (chance < 0 || chance > 100) {
            ChatHandler.sendError(sender, "Chance must be between &b0 &cand &b100&c.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_TEAM_CRATE_CHANCE, chance);
        config.saveConfig();
        if (chance == 0) {
            ChatHandler.send(sender, "Auto-drop team crate chance &cdisabled&7.");
        } else {
            ChatHandler.send(sender, "Auto-drop team crate chance set to &b" + chance + "%&7.");
        }
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoTrapChance(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto trap-chance <percent>");
            return;
        }
        int chance;
        try {
            chance = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "&c" + args[2] + " &cis not a valid number.");
            return;
        }
        if (chance < 0 || chance > 100) {
            ChatHandler.sendError(sender, "Chance must be between &b0 &cand &b100&c.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_TRAP_CHANCE, chance);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop trap chance set to &b" + chance + "%&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoTrapMobs(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto trap-mobs <type1> <type2> ...");
            ChatHandler.sendError(sender, "Example: /supplydrop auto trap-mobs ZOMBIE SKELETON CREEPER");
            return;
        }

        List<String> mobs = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            mobs.add(args[i].toUpperCase());
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_TRAP_MOBS, mobs);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop trap mobs set to &b" + String.join("&7, &b", mobs) + "&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoLootScaling(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto loot-scaling <true|false>");
            return;
        }
        String value = args[2].toLowerCase();
        if (!value.equals("true") && !value.equals("false")) {
            ChatHandler.sendError(sender, "Value must be &btrue &cor &cfalse&c.");
            return;
        }
        boolean enabled = value.equals("true");

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_LOOT_SCALING, enabled);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop loot scaling set to &b" + (enabled ? "enabled" : "disabled") + "&7.");
        plugin.restartAutoDropScheduler();
    }

    private void handleAutoFallDuration(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto fall-duration <seconds>");
            ChatHandler.sendError(sender, "Use &e0 &cto disable.");
            return;
        }
        int ticks;
        try {
            ticks = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            ChatHandler.sendError(sender, "Invalid number: &c" + args[2]);
            return;
        }
        if (ticks < 0) {
            ChatHandler.sendError(sender, "Value must be &e0 &cor positive&c.");
            return;
        }

        Config config = SupplyDrop.getConfiguration();
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (config == null || plugin == null) return;

        config.getConfig().set(ConfigKeys.AUTO_DROP_FALL_DURATION, ticks);
        config.saveConfig();
        if (ticks == 0) {
            ChatHandler.send(sender, "Auto-drop fall duration &cdisabled&7.");
        } else {
            ChatHandler.send(sender, "Auto-drop fall duration set to &b" + ticks + "s&7.");
        }
        plugin.restartAutoDropScheduler();
    }
}
