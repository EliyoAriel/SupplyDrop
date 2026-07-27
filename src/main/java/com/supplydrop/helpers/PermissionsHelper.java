package com.supplydrop.helpers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PermissionsHelper {

    private PermissionsHelper() {}

    public static boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("supplydrop.admin") || sender.isOp();
    }

    public static boolean hasPermission(Player player, String packageName) {
        if (isAdmin(player)) return true;
        if (packageName == null || packageName.isBlank()) return false;
        return player.hasPermission("supplydrop.package.all")
                || player.hasPermission("supplydrop.package." + packageName);
    }
}
