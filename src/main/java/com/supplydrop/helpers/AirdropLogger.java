package com.supplydrop.helpers;

import com.supplydrop.SupplyDrop;
import com.supplydrop.config.ConfigKeys;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class AirdropLogger {

    private AirdropLogger() {}

    private static Logger getLogger() {
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        return plugin != null ? plugin.getLogger() : Logger.getLogger("SupplyDrop");
    }

    public static void info(String message) {
        getLogger().info(message);
    }

    public static void warning(String message) {
        getLogger().warning(message);
    }

    public static void severe(String message) {
        getLogger().severe(message);
    }

    public static void log(Level level, String message, Throwable throwable) {
        getLogger().log(level, message, throwable);
    }

    public static void debug(String message) {
        if (ConfigKeys.isDebugLoggingEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
