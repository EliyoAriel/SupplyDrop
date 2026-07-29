package com.supplydrop.warning;

import com.supplydrop.SupplyDrop;
import com.supplydrop.config.ConfigKeys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class DropWarning {

    private final List<BukkitTask> tasks = new ArrayList<>();

    public void schedule(World world, String templateName, int count, long delayTicks) {
        cancelAll();

        if (!ConfigKeys.isAutoDropWarningEnabled()) return;

        List<Integer> secondsBefore = ConfigKeys.getAutoDropWarningSecondsBefore();
        String messageTemplate = ConfigKeys.getAutoDropWarningMessage();

        for (int seconds : secondsBefore) {
            long warnDelay = delayTicks - (seconds * 20L);
            if (warnDelay < 0) continue;

            BukkitTask task = Bukkit.getScheduler().runTaskLater(SupplyDrop.getPluginInstance(), () -> {
                String msg = messageTemplate
                        .replace("{seconds}", String.valueOf(seconds))
                        .replace("{template}", templateName)
                        .replace("{count}", String.valueOf(count));
                for (org.bukkit.entity.Player p : world.getPlayers()) {
                    p.sendActionBar(net.kyori.adventure.text.Component.text(ChatColor.translateAlternateColorCodes('&', msg)));
                }
            }, warnDelay);

            tasks.add(task);
        }
    }

    public void cancelAll() {
        for (BukkitTask task : tasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();
    }
}
