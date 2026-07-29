package com.supplydrop.chain;

import com.supplydrop.config.ConfigKeys;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.stats.AutoDropStats;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChainManager {

    private static final Random RANDOM = new Random();

    public boolean tryTriggerChain(World world, Location centerLocation) {
        if (!ConfigKeys.isAutoDropChainEnabled()) return false;

        int chance = ConfigKeys.getAutoDropChainChance();
        if (chance <= 0) return false;

        if (RANDOM.nextInt(100) >= chance) return false;

        startChain(world, centerLocation);
        return true;
    }

    public void triggerManualChain(Player player) {
        startChain(player.getWorld(), player.getLocation());
    }

    private void startChain(World world, Location centerLocation) {
        List<String> loadedTemplates = new ArrayList<>(ConfigKeys.getAutoDropTemplates().stream()
                .map(tw -> tw.name())
                .toList());

        if (loadedTemplates.isEmpty()) {
            AirdropLogger.warning("Chain drop: no auto-drop templates available.");
            return;
        }

        ChainDrop chain = new ChainDrop(loadedTemplates, world, centerLocation);
        chain.start();

        AutoDropStats stats = AutoDropStats.get();
        if (stats != null) {
            stats.incrementChain();
        }
    }
}
