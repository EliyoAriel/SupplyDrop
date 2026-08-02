package com.supplydrop.listeners;

import com.supplydrop.integration.LandClaimHook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class PluginEnableListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("LandClaim")) {
            LandClaimHook.init();
        }
    }
}
