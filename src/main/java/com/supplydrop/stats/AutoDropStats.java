package com.supplydrop.stats;

import com.supplydrop.SupplyDrop;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AutoDropStats {

    private static AutoDropStats instance;
    private final File file;
    private YamlConfiguration data;

    private int totalDrops;
    private int totalCrates;
    private final Map<String, Integer> templateCounts = new HashMap<>();
    private int trapCount;
    private int teamCount;
    private int chainCount;
    private long lastDropTime;

    public AutoDropStats(File dataFolder) {
        this.file = new File(dataFolder, "stats.yml");
        instance = this;
    }

    public static AutoDropStats get() {
        return instance;
    }

    public void load() {
        if (file.exists()) {
            data = YamlConfiguration.loadConfiguration(file);
        } else {
            data = new YamlConfiguration();
        }

        totalDrops = data.getInt("total-drops", 0);
        totalCrates = data.getInt("total-crates", 0);
        trapCount = data.getInt("trap-count", 0);
        teamCount = data.getInt("team-count", 0);
        chainCount = data.getInt("chain-count", 0);
        lastDropTime = data.getLong("last-drop-time", 0);

        templateCounts.clear();
        if (data.contains("template-counts")) {
            for (String key : data.getConfigurationSection("template-counts").getKeys(false)) {
                templateCounts.put(key, data.getInt("template-counts." + key));
            }
        }
    }

    public void save() {
        data.set("total-drops", totalDrops);
        data.set("total-crates", totalCrates);
        data.set("trap-count", trapCount);
        data.set("team-count", teamCount);
        data.set("chain-count", chainCount);
        data.set("last-drop-time", lastDropTime);

        for (Map.Entry<String, Integer> entry : templateCounts.entrySet()) {
            data.set("template-counts." + entry.getKey(), entry.getValue());
        }

        try {
            data.save(file);
        } catch (IOException e) {
            SupplyDrop.getPluginInstance().getLogger().warning("Failed to save stats.yml: " + e.getMessage());
        }
    }

    public void reset() {
        totalDrops = 0;
        totalCrates = 0;
        templateCounts.clear();
        trapCount = 0;
        teamCount = 0;
        chainCount = 0;
        lastDropTime = 0;
        save();
    }

    public void incrementDrop() { totalDrops++; }
    public void incrementCrates(int count) { totalCrates += count; }
    public void incrementTemplate(String name) { templateCounts.merge(name, 1, Integer::sum); }
    public void incrementTrap() { trapCount++; }
    public void incrementTeam() { teamCount++; }
    public void incrementChain() { chainCount++; }
    public void setLastDropTime(long time) { lastDropTime = time; }

    public int getTotalDrops() { return totalDrops; }
    public int getTotalCrates() { return totalCrates; }
    public Map<String, Integer> getTemplateCounts() { return Map.copyOf(templateCounts); }
    public int getTrapCount() { return trapCount; }
    public int getTeamCount() { return teamCount; }
    public int getChainCount() { return chainCount; }
    public long getLastDropTime() { return lastDropTime; }
}
