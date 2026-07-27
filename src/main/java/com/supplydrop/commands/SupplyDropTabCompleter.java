package com.supplydrop.commands;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Crate;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import com.supplydrop.helpers.CrateManager;
import com.supplydrop.helpers.PermissionsHelper;
import com.supplydrop.packages.PackageManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SupplyDropTabCompleter implements TabCompleter {

    private static final List<String> ADMIN_SUB_COMMANDS = List.of("call", "spawn", "active", "db", "preview", "history", "package", "templates", "reload", "version", "pause", "resume", "auto", "subscribe", "unsubscribe", "toggle", "delete", "config");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            SupplyDrop plugin = SupplyDrop.getPluginInstance();
            if (plugin == null || !plugin.isEnabled()) return new ArrayList<>();

            if (args.length == 1) {
                Set<String> suggestions = new LinkedHashSet<>();
                if (PermissionsHelper.isAdmin(sender)) {
                    suggestions.addAll(ADMIN_SUB_COMMANDS);
                }
                suggestions.addAll(PackageManager.getPackageNames());
                return new ArrayList<>(suggestions);
            }

            // /supplydrop spawn <template> [conditions...]
            if (args.length >= 2 && args[0].equalsIgnoreCase("spawn") && PermissionsHelper.isAdmin(sender)) {
                if (args.length == 2) {
                    return new ArrayList<>(PackageManager.getPackageNames());
                }
                // For args 3+, offer condition flags
                List<String> conditions = new ArrayList<>();
                String alreadyUsed = String.join(" ", args).toLowerCase();
                if (!alreadyUsed.contains("team")) conditions.add("team");
                if (!alreadyUsed.contains("trap")) conditions.add("trap");
                if (!alreadyUsed.contains("wave:")) conditions.add("wave:");
                if (!alreadyUsed.contains("players:")) conditions.add("players:");
                if (!alreadyUsed.contains("expiry:")) conditions.add("expiry:");
                return conditions;
            }

            // /supplydrop preview <template>
            if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return new ArrayList<>(PackageManager.getPackageNames());
                }
            }

            // /supplydrop history [page] [event:<type>] [player:<name>]
            if (args.length >= 2 && args[0].equalsIgnoreCase("history")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    String lastArg = args[args.length - 1].toLowerCase();
                    if (lastArg.startsWith("event:")) {
                        return List.of("event:SPAWN", "event:OPEN", "event:TRAP", "event:EXPIRE", "event:DESTROY");
                    }
                    if (lastArg.startsWith("player:")) {
                        return List.of("player:");
                    }
                    if (args.length == 2) {
                        return List.of("event:", "player:", "1", "2", "3");
                    }
                }
            }

            // /supplydrop toggle <type>
            if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return List.of("hologram", "announce", "notify");
                }
            }

            // /supplydrop delete <id|number|all>
            if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    List<Crate> active = CrateManager.getActiveCrates();
                    for (int i = 0; i < active.size(); i++) {
                        Crate crate = active.get(i);
                        options.add(crate.getShortId());
                    }
                    return options;
                }
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("package")) {
                List<String> subcommands = new ArrayList<>();
                if (PermissionsHelper.isAdmin(sender)) {
                    subcommands.add("create");
                    subcommands.add("delete");
                    subcommands.add("setdisplay");
                }
                subcommands.addAll(PackageManager.getPackageNames());
                return subcommands;
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("package")) {
                String sub = args[1].toLowerCase();
                if (PermissionsHelper.isAdmin(sender)) {
                    if (sub.equals("create") || sub.equals("delete")) {
                        return List.of("[templateName]");
                    }
                    if (sub.equals("setdisplay")) {
                        return new ArrayList<>(PackageManager.getPackageNames());
                    }
                }
            }

            if (args.length == 4 && args[0].equalsIgnoreCase("package") && args[1].equalsIgnoreCase("setdisplay")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return List.of("[display name]", "clear");
                }
            }

            // /supplydrop auto <sub>
            if (args.length == 2 && args[0].equalsIgnoreCase("auto")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return List.of("status", "enable", "disable", "pause", "resume",
                                    "interval", "interval-min", "interval-max",
                                    "world", "radius", "templates", "announce", "announce-delay",
                                   "announce-actionbar", "coord-reveal-delay", "wave-count",
                                    "expiry", "team-crate", "trap-chance", "trap-mobs",
                                    "loot-scaling", "escalating", "fall-duration");
                }
            }

            // /supplydrop auto templates <sub>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("templates")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return List.of("add", "remove", "set");
                }
            }

            // /supplydrop auto templates add|set <name>
            if (args.length == 4 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("templates")) {
                String sub = args[2].toLowerCase();
                if (PermissionsHelper.isAdmin(sender) && (sub.equals("add") || sub.equals("remove") || sub.equals("set"))) {
                    return new ArrayList<>(PackageManager.getPackageNames());
                }
            }

            // /supplydrop auto templates remove|set <name>  — no more args for remove
            // /supplydrop auto templates add|set <name> <weight>
            if (args.length == 5 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("templates")) {
                String sub = args[2].toLowerCase();
                if (PermissionsHelper.isAdmin(sender) && (sub.equals("add") || sub.equals("set"))) {
                    return List.of("[weight]");
                }
            }

            // /supplydrop auto templates — show current templates when just typed
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("templates")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
                    return templates.stream().map(TemplateWeight::name).toList();
                }
            }

            // /supplydrop auto interval <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("interval")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return List.of("200", "1200", "7200", "36000", "72000");
                }
            }

            // /supplydrop auto world <name>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("world")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return Bukkit.getWorlds().stream()
                            .map(w -> w.getName())
                            .toList();
                }
            }

            // /supplydrop auto radius <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("radius")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return List.of("100", "250", "500", "1000", "2000");
                }
            }

            // /supplydrop auto announce <true|false>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("announce")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return List.of("true", "false");
                }
            }

            // /supplydrop auto announce-delay <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("announce-delay")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    return List.of("20", "40", "60", "100", "200");
                }
            }

            // /supplydrop auto random-interval <true|false>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("random-interval")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("true", "false");
            }

            // /supplydrop auto interval-min <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("interval-min")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("18000", "36000", "72000");
            }

            // /supplydrop auto interval-max <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("interval-max")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("36000", "72000", "144000");
            }

            // /supplydrop auto announce-actionbar <true|false>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("announce-actionbar")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("true", "false");
            }

            // /supplydrop auto coord-reveal-delay <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("coord-reveal-delay")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("0", "60", "100", "200", "400");
            }

            // /supplydrop auto wave-count <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("wave-count")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("1", "2", "3", "5");
            }

            // /supplydrop auto expiry <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("expiry")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("0", "6000", "12000", "24000", "72000");
            }

            // /supplydrop auto team-crate <percent>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("team-crate")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("0", "5", "10", "15", "25");
            }

            // /supplydrop auto trap-chance <value>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("trap-chance")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("0", "5", "10", "15", "25");
            }

            // /supplydrop auto trap-mobs <types>
            if (args.length >= 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("trap-mobs")) {
                if (PermissionsHelper.isAdmin(sender)) {
                    List<String> all = List.of("ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "WITCH", "BLAZE", "ENDERMAN");
                    // Filter out already-typed mobs
                    List<String> typed = new ArrayList<>();
                    for (int i = 2; i < args.length; i++) {
                        typed.add(args[i].toUpperCase());
                    }
                    return all.stream().filter(m -> !typed.contains(m)).toList();
                }
            }

            // /supplydrop auto loot-scaling <true|false>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("loot-scaling")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("true", "false");
            }

            // /supplydrop auto escalating <true|false>
            if (args.length == 3 && args[0].equalsIgnoreCase("auto") && args[1].equalsIgnoreCase("escalating")) {
                if (PermissionsHelper.isAdmin(sender)) return List.of("true", "false");
            }
        } catch (Exception e) {
            // Prevent tab completion errors from crashing the server
        }

        return new ArrayList<>();
    }
}
