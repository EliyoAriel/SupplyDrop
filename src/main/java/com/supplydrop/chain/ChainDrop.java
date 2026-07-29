package com.supplydrop.chain;

import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import com.supplydrop.controllers.DropController;
import com.supplydrop.exceptions.SkyNotClearException;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.helpers.NotificationManager;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChainDrop {

    private final List<String> templateNames;
    private int currentIndex;
    private final boolean decreaseInterval;
    private final int baseInterval;
    private final int count;
    private final World world;
    private final Location centerLocation;
    private final Random random = new Random();

    public ChainDrop(List<String> templateNames, World world, Location centerLocation) {
        this.templateNames = new ArrayList<>(templateNames);
        this.world = world;
        this.centerLocation = centerLocation;
        this.count = ConfigKeys.getAutoDropChainCount();
        this.baseInterval = ConfigKeys.getAutoDropChainInterval();
        this.decreaseInterval = ConfigKeys.isAutoDropChainDecreaseInterval();
        this.currentIndex = 0;
    }

    public void start() {
        if (templateNames.isEmpty() || count <= 0) {
            AirdropLogger.warning("Chain drop cancelled: no templates or zero count.");
            return;
        }
        spawnNext();
    }

    private void spawnNext() {
        if (currentIndex >= count) {
            AirdropLogger.info("Chain drop completed: " + count + " crates delivered.");
            return;
        }

        String selectedName = selectTemplate();
        Package pkg = PackageManager.get(selectedName);
        if (pkg == null) {
            AirdropLogger.warning("Chain drop: template '" + selectedName + "' not found, skipping.");
            currentIndex++;
            spawnNext();
            return;
        }

        int radius = ConfigKeys.getAutoDropRandomRadius();
        Location offset = centerLocation.clone().add(
                (random.nextDouble() - 0.5) * radius * 0.2,
                0,
                (random.nextDouble() - 0.5) * radius * 0.2);

        try {
            int onlineCount = world.getPlayers().size();
            int playerBonusRolls = ConfigKeys.getAutoDropBonusRollsForPlayers(onlineCount);
            DropController.dropAtLocation(pkg, world, offset, playerBonusRolls);

            String remainingMsg = count - currentIndex - 1 > 0 ? " &7(" + (count - currentIndex - 1) + " remaining)" : "";
            NotificationManager.notify(world.getName(),
                    "&c&l⛓ CHAIN &7- &e" + pkg.getDisplayName() + " &bincoming!" + remainingMsg);

            AirdropLogger.info("Chain drop " + (currentIndex + 1) + "/" + count + ": " + selectedName);
        } catch (SkyNotClearException e) {
            AirdropLogger.warning("Chain drop: sky not clear at target location, skipping crate " + (currentIndex + 1));
        }

        currentIndex++;

        if (currentIndex < count) {
            int gap;
            if (decreaseInterval) {
                gap = Math.max(200, baseInterval - (currentIndex * (baseInterval / count)));
            } else {
                gap = baseInterval;
            }
            Bukkit.getScheduler().runTaskLater(
                    com.supplydrop.SupplyDrop.getPluginInstance(),
                    this::spawnNext,
                    gap);
        }
    }

    private String selectTemplate() {
        List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
        if (templates.isEmpty()) return "weapons";

        int totalWeight = templates.stream().mapToInt(TemplateWeight::weight).sum();
        if (totalWeight <= 0) return templates.get(0).name();

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (TemplateWeight tw : templates) {
            cumulative += tw.weight();
            if (roll < cumulative) return tw.name();
        }
        return templates.get(templates.size() - 1).name();
    }
}
