package com.supplydrop;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Config {

    private final SupplyDrop plugin;
    private final String filename;
    private FileConfiguration config;

    public Config(SupplyDrop plugin) {
        this.plugin = plugin;
        this.filename = "config.yml";
    }

    public void saveDefaultConfig() {
        plugin.saveResource(filename, false);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(
                new java.io.File(plugin.getDataFolder(), filename));

        try (InputStream defaultStream = plugin.getResource(filename)) {
            if (defaultStream != null) {
                try (InputStreamReader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                    config.setDefaults(defaultConfig);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load default config: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void saveConfig() {
        if (config != null) {
            try {
                config.save(new java.io.File(plugin.getDataFolder(), filename));
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save config: " + e.getMessage());
            }
        }
    }

    public int getRotationIndex() {
        return getConfig().getInt("auto-drop.rotation.index", 0);
    }

    public void setRotationIndex(int index) {
        getConfig().set("auto-drop.rotation.index", index);
        saveConfig();
    }

    public int getEscalationLevel() {
        return getConfig().getInt("auto-drop.escalation.level", 0);
    }

    public void setEscalationLevel(int level) {
        getConfig().set("auto-drop.escalation.level", level);
        saveConfig();
    }
}
