package com.supplydrop.hologram;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * A multi-line floating text hologram above a location.
 * Uses invisible armor stands stacked vertically.
 * Supports dynamic line updates.
 */
public class Hologram {

    private static final double LINE_SPACING = 0.3;
    private static final double BASE_HEIGHT = 1.8;

    private final World world;
    private final Location baseLocation;
    private final List<ArmorStand> armorStands = new ArrayList<>();

    public Hologram(Location baseLocation, List<String> lines) {
        this.world = baseLocation.getWorld();
        this.baseLocation = baseLocation.clone();
        spawnLines(lines);
    }

    private void spawnLines(List<String> lines) {
        int index = 0;
        for (String rawLine : lines) {
            String text = rawLine.trim();
            if (text.isEmpty()) continue;

            double yOffset = BASE_HEIGHT + (index * LINE_SPACING);
            Location loc = baseLocation.clone().add(0.5, yOffset, 0.5);
            text = ChatColor.translateAlternateColorCodes('&', text);

            ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setCustomName(text);
            stand.setCustomNameVisible(true);
            stand.setVisible(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setAI(false);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setMarker(true);
            armorStands.add(stand);
            index++;
        }
    }

    /**
     * Update all lines dynamically. Filters out empty lines, adds/removes armor stands as needed.
     */
    public void updateLines(List<String> rawLines) {
        // Filter out empty lines
        List<String> lines = new ArrayList<>();
        for (String raw : rawLines) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }

        // Remove excess stands
        while (armorStands.size() > lines.size()) {
            ArmorStand removed = armorStands.remove(armorStands.size() - 1);
            if (removed != null && !removed.isDead()) {
                removed.remove();
            }
        }

        // Update existing stands
        for (int i = 0; i < armorStands.size(); i++) {
            ArmorStand stand = armorStands.get(i);
            if (stand != null && !stand.isDead()) {
                String text = ChatColor.translateAlternateColorCodes('&', lines.get(i));
                stand.setCustomName(text);
            }
        }

        // Add new stands if needed
        while (armorStands.size() < lines.size()) {
            int i = armorStands.size();
            double yOffset = BASE_HEIGHT + (i * LINE_SPACING);
            Location loc = baseLocation.clone().add(0.5, yOffset, 0.5);
            String text = ChatColor.translateAlternateColorCodes('&', lines.get(i));

            ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setCustomName(text);
            stand.setCustomNameVisible(true);
            stand.setVisible(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setAI(false);
            stand.setSilent(true);
            stand.setCollidable(false);
            stand.setMarker(true);
            armorStands.add(stand);
        }
    }

    /**
     * Update a single line by index.
     */
    public void updateLine(int index, String line) {
        if (index >= 0 && index < armorStands.size()) {
            ArmorStand stand = armorStands.get(index);
            if (stand != null && !stand.isDead()) {
                stand.setCustomName(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
    }

    public void destroy() {
        for (ArmorStand stand : armorStands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        armorStands.clear();
    }

    public List<ArmorStand> getArmorStands() {
        return List.copyOf(armorStands);
    }

    public int getLineCount() {
        return armorStands.size();
    }

    public boolean isDead() {
        return armorStands.isEmpty() || armorStands.stream().allMatch(Entity::isDead);
    }
}
