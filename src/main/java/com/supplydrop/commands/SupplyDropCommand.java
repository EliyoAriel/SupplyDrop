package com.supplydrop.commands;

import com.supplydrop.Config;
import com.supplydrop.SupplyDrop;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import com.supplydrop.controllers.DropController;
import com.supplydrop.exceptions.SkyNotClearException;
import com.supplydrop.gui.PreviewGui;
import com.supplydrop.gui.TemplateListGui;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.helpers.CrateManager;
import com.supplydrop.helpers.DatabaseManager;
import com.supplydrop.helpers.PermissionsHelper;
import com.supplydrop.loot.LootTable;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;

import com.supplydrop.Crate;

import org.bukkit.Bukkit;
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
                case "package" -> handlePackageSubcommand(sender, args);
                case "templates" -> handleTemplatesCommand(sender);
                case "reload" -> handleReloadSubcommand(sender);
                case "version" -> handleVersionSubcommand(sender);
                case "pause" -> handlePauseCommand(sender, true);
                case "resume" -> handlePauseCommand(sender, false);
                case "auto" -> handleAutoCommand(sender, args);
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

        ChatHandler.send(sender, "&bActive Supply Crates &7(" + activeCrates.size() + "):");
        for (Crate crate : activeCrates) {
            String name = crate.getDisplayName() != null ? crate.getDisplayName() : "Unknown";
            String loc = crate.getLandedLocation() != null
                    ? crate.getLandedLocation().getWorld().getName() + " "
                      + crate.getLandedLocation().getBlockX() + " "
                      + crate.getLandedLocation().getBlockY() + " "
                      + crate.getLandedLocation().getBlockZ()
                    : "Falling";
            ChatHandler.send(sender, " &e" + name + " &7- &f" + loc);
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

    private void handleVersionSubcommand(CommandSender sender) {
        String version = SupplyDrop.getPluginVersion() != null ? SupplyDrop.getPluginVersion() : "unknown";
        ChatHandler.sendWithoutPrefix(sender,
                "&9━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "&f  SupplyDrop &bv" + version + "\n" +
                "&f  Paper API &b1.21+\n" +
                "&9━━━━━━━━━━━━━━━━━━━━━━━━");
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
            case "random-interval" -> handleAutoRandomInterval(sender, args);
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
            case "escalating"    -> handleAutoEscalating(sender, args);
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
                "&e random-interval <true|false> &8- &7Toggle random interval\n" +
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
                "&e team-crate <percent> &8- &7Team crate chance\n" +
                "&e trap-chance <percent> &8- &7Trap crate chance\n" +
                "&e trap-mobs <type> &8- &7Set trap mob type\n" +
                "&e loot-scaling <true|false> &8- &7Loot scaling\n" +
                "&e escalating <true|false> &8- &7Escalating rarity\n" +
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

    private void handleAutoRandomInterval(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto random-interval <true|false>");
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

        config.getConfig().set(ConfigKeys.AUTO_DROP_RANDOM_INTERVAL, enabled);
        config.saveConfig();
        ChatHandler.send(sender, "Random interval set to &b" + (enabled ? "enabled" : "disabled") + "&7.");
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

    private void handleAutoEscalating(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatHandler.sendError(sender, "Usage: /supplydrop auto escalating <true|false>");
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

        config.getConfig().set(ConfigKeys.AUTO_DROP_ESCALATING, enabled);
        config.saveConfig();
        ChatHandler.send(sender, "Auto-drop escalating set to &b" + (enabled ? "enabled" : "disabled") + "&7.");
        plugin.restartAutoDropScheduler();
    }
}
