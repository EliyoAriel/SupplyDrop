package com.supplydrop.announce;

import com.supplydrop.Config;
import com.supplydrop.SupplyDrop;
import com.supplydrop.helpers.NotificationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;

public class AnnouncementManager {

    private static final Map<String, AnnouncementTier> tiers = new HashMap<>();

    private AnnouncementManager() {}

    public static void load(Config config) {
        tiers.clear();
        if (config == null) return;

        ConfigurationSection section = config.getConfig().getConfigurationSection("announce.tiers");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String prefix = section.getString(key + ".prefix", "");
            String message = section.getString(key + ".message", "&e{template} &bincoming!");
            boolean actionbar = section.getBoolean(key + ".actionbar", false);
            Integer coordRevealDelay = section.contains(key + ".coord-reveal-delay")
                    ? section.getInt(key + ".coord-reveal-delay") : null;

            tiers.put(key, new AnnouncementTier(key, prefix, message, actionbar, coordRevealDelay));
        }
    }

    public static AnnouncementTier getTier(String name) {
        return tiers.getOrDefault(name, tiers.get("normal"));
    }

    public static void announce(String tierName, String templateName, int count, String seasonPrefix,
                                 Location location, World world, String worldName) {
        AnnouncementTier tier = getTier(tierName);
        if (tier == null) tier = getTier("normal");
        if (tier == null) return;

        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin == null) return;
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return;

        String prefix = seasonPrefix != null && !seasonPrefix.isEmpty() ? seasonPrefix + " " : tier.prefix();
        String prefixColored = prefix.isEmpty() ? "" : ChatColor.translateAlternateColorCodes('&', prefix) + " ";

        String msg = tier.message()
                .replace("{template}", templateName)
                .replace("{count}", String.valueOf(count))
                .replace("{season-prefix}", seasonPrefix != null ? seasonPrefix : "")
                .replace("{coords}", location != null ? location.getBlockX() + ", " + location.getBlockZ() : "?")
                .replace("{world}", worldName);

        int coordDelay = tier.coordRevealDelay() != null ? tier.coordRevealDelay() : 0;

        if (tier.actionbar()) {
            Component actionbarMsg = Component.text(prefixColored + ChatColor.translateAlternateColorCodes('&', msg));
            for (Player p : world.getPlayers()) {
                p.sendActionBar(actionbarMsg);
            }
        } else {
            if (coordDelay > 0 && location != null) {
                String noLocMsg = prefixColored + ChatColor.translateAlternateColorCodes('&', msg.replace("{coords}", "???"));
                NotificationManager.notify(worldName, noLocMsg);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String withLoc = prefixColored + ChatColor.translateAlternateColorCodes('&', msg);
                    NotificationManager.notify(worldName, withLoc);
                }, coordDelay);
            } else {
                String fullMsg = prefixColored + ChatColor.translateAlternateColorCodes('&', msg);
                NotificationManager.notify(worldName, fullMsg);
            }
        }
    }

    public static void clear() {
        tiers.clear();
    }
}
