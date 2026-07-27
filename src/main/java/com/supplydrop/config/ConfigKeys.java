package com.supplydrop.config;

import com.supplydrop.Config;
import com.supplydrop.SupplyDrop;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class ConfigKeys {

    private static final FileConfiguration FALLBACK_CONFIG = new YamlConfiguration();

    private static final int DEFAULT_PARACHUTE_CHICKEN_COUNT = 5;
    private static final int MIN_PARACHUTE_CHICKEN_COUNT = 1;
    private static final int MAX_PARACHUTE_CHICKEN_COUNT = 64;
    private static final double DEFAULT_DROP_FALLING_SPEED = 0.3;
    private static final double MIN_DROP_FALLING_SPEED = 0.01;
    private static final double MAX_DROP_FALLING_SPEED = 4.0;
    private static final int DEFAULT_DROP_HEIGHT = 20;
    private static final int MIN_DROP_HEIGHT = 1;
    private static final int MAX_DROP_HEIGHT = 320;
    private static final int DEFAULT_SMOKE_HEIGHT = 20;
    private static final int MIN_SMOKE_HEIGHT = 0;
    private static final int MAX_SMOKE_HEIGHT = 128;
    private static final int DEFAULT_AUTO_DROP_INTERVAL = 72000;
    private static final int DEFAULT_AUTO_DROP_RADIUS = 500;
    private static final int DEFAULT_AUTO_DROP_ANNOUNCE_DELAY = 100;
    private static final int DEFAULT_MIN_ROLLS = 3;
    private static final int DEFAULT_MAX_ROLLS = 6;

    private ConfigKeys() {}

    // Drop settings
    public static final String DROP_PARACHUTE_CHICKEN_COUNT = "drop.parachute.chicken-count";
    public static final String DROP_FALLING_SPEED = "drop.falling-speed";
    public static final String DROP_HEIGHT = "drop.height";
    public static final String DROP_LANDING_PARTICLE_EFFECTS = "drop.particles.landing-effects";
    public static final String DROP_CONTINUOUS_PARTICLE_EFFECTS = "drop.particles.continuous-effects";
    public static final String DROP_FLARE_PARTICLE_EFFECTS = "drop.particles.flare-effects";
    public static final String DROP_SMOKE_ENABLED = "drop.particles.smoke.enabled";
    public static final String DROP_SMOKE_HEIGHT = "drop.particles.smoke.height";
    public static final String DROP_LANDING_ZONE_ENABLED = "drop.particles.landing-zone.enabled";
    public static final String DROP_LANDING_ZONE_RADIUS = "drop.particles.landing-zone.radius";
    public static final String DROP_LANDING_ZONE_PARTICLE = "drop.particles.landing-zone.particle";

    // Roll settings
    public static final String ROLLS_MIN = "rolls.min";
    public static final String ROLLS_MAX = "rolls.max";

    // Auto-drop settings
    public static final String AUTO_DROP_ENABLED = "auto-drop.enabled";
    public static final String AUTO_DROP_PAUSED = "auto-drop.paused";
    public static final String AUTO_DROP_RANDOM_INTERVAL = "auto-drop.random-interval";
    public static final String AUTO_DROP_INTERVAL = "auto-drop.interval";
    public static final String AUTO_DROP_INTERVAL_MIN = "auto-drop.interval-min";
    public static final String AUTO_DROP_INTERVAL_MAX = "auto-drop.interval-max";
    public static final String AUTO_DROP_WORLD = "auto-drop.world";
    public static final String AUTO_DROP_RANDOM_RADIUS = "auto-drop.random-radius";
    public static final String AUTO_DROP_TEMPLATE = "auto-drop.template";
    public static final String AUTO_DROP_TEMPLATES = "auto-drop.templates";
    public static final String AUTO_DROP_ANNOUNCE = "auto-drop.announce";
    public static final String AUTO_DROP_ANNOUNCE_DELAY = "auto-drop.announce-delay";
    public static final String AUTO_DROP_ANNOUNCE_ACTIONBAR = "auto-drop.announce-actionbar";
    public static final String AUTO_DROP_COORD_REVEAL_DELAY = "auto-drop.coord-reveal-delay";
    public static final String AUTO_DROP_WAVE_COUNT = "auto-drop.wave-count";
    public static final String AUTO_DROP_EXPIRY = "auto-drop.expiry";
    public static final String AUTO_DROP_TEAM_CRATE_CHANCE = "auto-drop.team-crate-chance";
    public static final String AUTO_DROP_TEAM_CRATE_RANGE = "auto-drop.team-crate-range";
    public static final String AUTO_DROP_TRAP_CHANCE = "auto-drop.trap-chance";
    public static final String AUTO_DROP_TRAP_MOBS = "auto-drop.trap-mobs";
    public static final String AUTO_DROP_LOOT_SCALING = "auto-drop.loot-scaling";
    public static final String AUTO_DROP_LOOT_SCALING_MAX = "auto-drop.loot-scaling-max";

    public static final String NOTIFICATION_DEFAULT_SUBSCRIBE = "notification.default-subscribe";
    public static final String ANNOUNCE_ENABLED = "announce.enabled";

    public static final String DROP_FALL_DURATION = "drop.fall-duration";

    public static final String AUTO_DROP_FALL_DURATION = "auto-drop.fall-duration";
    public static final String CALL_FALL_DURATION = "call.fall-duration";
    public static final String SPAWN_FALL_DURATION = "spawn.fall-duration";

    // Crate settings
    public static final String CRATE_EXPIRY = "crate.expiry";
    public static final String CRATE_PROTECTION_RADIUS = "crate.protection-radius";
    public static final String CRATE_MAX_ACTIVE = "crate.max-active";
    public static final String CRATE_TEAM_OPEN_CHANCE = "crate.team-open-chance";
    public static final String CRATE_TEAM_OPEN_RANGE = "crate.team-open-range";
    public static final String CRATE_TRAP_CHANCE = "crate.trap-chance";
    public static final String CRATE_TRAP_MOBS = "crate.trap-mobs";

    // Hologram settings
    public static final String HOLOGRAM_ENABLED = "drop.hologram.enabled";
    public static final String HOLOGRAM_LINES = "drop.hologram.lines";

    // Announcement settings
    public static final String ANNOUNCE_ACTIONBAR = "announce.actionbar";
    public static final String ANNOUNCE_COORD_REVEAL_DELAY = "announce.coord-reveal-delay";

    public static final String LOGGING_DEBUG = "logging.debug";

    // Drop settings getters
    public static int getParachuteChickenCount() {
        return sanitizeParachuteChickenCount(getConfig().getInt(DROP_PARACHUTE_CHICKEN_COUNT, DEFAULT_PARACHUTE_CHICKEN_COUNT));
    }

    public static double getDropFallingSpeed() {
        return sanitizeDropFallingSpeed(getConfig().getDouble(DROP_FALLING_SPEED, DEFAULT_DROP_FALLING_SPEED));
    }

    public static int getDropHeight() {
        return sanitizeDropHeight(getConfig().getInt(DROP_HEIGHT, DEFAULT_DROP_HEIGHT));
    }

    public static boolean shouldShowLandingParticleEffects() {
        return getConfig().getBoolean(DROP_LANDING_PARTICLE_EFFECTS, true);
    }

    public static boolean shouldShowContinuousParticleEffects() {
        return getConfig().getBoolean(DROP_CONTINUOUS_PARTICLE_EFFECTS, true);
    }

    public static boolean shouldShowFlareParticleEffects() {
        return getConfig().getBoolean(DROP_FLARE_PARTICLE_EFFECTS, true);
    }

    public static boolean isSmokeEnabled() {
        return getConfig().getBoolean(DROP_SMOKE_ENABLED, false);
    }

    public static int getSmokeHeight() {
        return sanitizeSmokeHeight(getConfig().getInt(DROP_SMOKE_HEIGHT, DEFAULT_SMOKE_HEIGHT));
    }

    public static boolean isLandingZoneEnabled() {
        return getConfig().getBoolean(DROP_LANDING_ZONE_ENABLED, true);
    }

    public static int getLandingZoneRadius() {
        return Math.max(1, getConfig().getInt(DROP_LANDING_ZONE_RADIUS, 3));
    }

    public static String getLandingZoneParticle() {
        return getConfig().getString(DROP_LANDING_ZONE_PARTICLE, "FLAME");
    }

    // Roll settings
    public static int getMinRolls() {
        return Math.max(1, getConfig().getInt(ROLLS_MIN, DEFAULT_MIN_ROLLS));
    }

    public static int getMaxRolls() {
        return Math.max(getMinRolls(), getConfig().getInt(ROLLS_MAX, DEFAULT_MAX_ROLLS));
    }

    // Auto-drop settings
    public static boolean isAutoDropEnabled() {
        return getConfig().getBoolean(AUTO_DROP_ENABLED, false);
    }

    public static boolean isAutoDropPaused() {
        return getConfig().getBoolean(AUTO_DROP_PAUSED, false);
    }

    public static boolean isAutoDropRandomInterval() {
        return getConfig().getBoolean(AUTO_DROP_RANDOM_INTERVAL, false);
    }

    public static int getAutoDropInterval() {
        return Math.max(200, getConfig().getInt(AUTO_DROP_INTERVAL, DEFAULT_AUTO_DROP_INTERVAL));
    }

    public static String getAutoDropWorld() {
        return getConfig().getString(AUTO_DROP_WORLD, "world");
    }

    public static int getAutoDropRandomRadius() {
        return Math.max(1, getConfig().getInt(AUTO_DROP_RANDOM_RADIUS, DEFAULT_AUTO_DROP_RADIUS));
    }

    public static String getAutoDropTemplate() {
        return getConfig().getString(AUTO_DROP_TEMPLATE, "weapons");
    }

    /**
     * Get weighted template list from auto-drop.templates section.
     * Falls back to old single template format if templates section doesn't exist.
     */
    public static List<TemplateWeight> getAutoDropTemplates() {
        List<TemplateWeight> list = new ArrayList<>();
        FileConfiguration config = getConfig();

        // New format: auto-drop.templates section
        ConfigurationSection templatesSection = config.getConfigurationSection(AUTO_DROP_TEMPLATES);
        if (templatesSection != null) {
            for (String name : templatesSection.getKeys(false)) {
                int weight = templatesSection.getInt(name + ".weight", 10);
                if (weight <= 0) continue; // disabled
                list.add(new TemplateWeight(name, Math.max(1, weight)));
            }
        }

        // Fallback: old single template format
        if (list.isEmpty()) {
            String single = config.getString(AUTO_DROP_TEMPLATE, "weapons");
            if (single != null && !single.isEmpty()) {
                list.add(new TemplateWeight(single, 10));
            }
        }

        return list;
    }

    public record TemplateWeight(String name, int weight) {}

    public static boolean isAutoDropAnnounce() {
        return getConfig().getBoolean(AUTO_DROP_ANNOUNCE, true);
    }

    public static int getAutoDropAnnounceDelay() {
        return Math.max(20, getConfig().getInt(AUTO_DROP_ANNOUNCE_DELAY, DEFAULT_AUTO_DROP_ANNOUNCE_DELAY));
    }

    public static boolean isAutoDropAnnounceActionbar() {
        return getConfig().getBoolean(AUTO_DROP_ANNOUNCE_ACTIONBAR, false);
    }

    public static int getAutoDropCoordRevealDelay() {
        return Math.max(0, getConfig().getInt(AUTO_DROP_COORD_REVEAL_DELAY, 0));
    }

    public static int getAutoDropIntervalMin() {
        return Math.max(200, getConfig().getInt(AUTO_DROP_INTERVAL_MIN, 36000));
    }

    public static int getAutoDropIntervalMax() {
        return Math.max(getAutoDropIntervalMin(), getConfig().getInt(AUTO_DROP_INTERVAL_MAX, 72000));
    }

    public static int getAutoDropWaveCount() {
        return Math.max(1, getConfig().getInt(AUTO_DROP_WAVE_COUNT, 1));
    }

    public static int getAutoDropExpiry() {
        return Math.max(0, getConfig().getInt(AUTO_DROP_EXPIRY, 0));
    }

    public static int getAutoDropTrapChance() {
        return Math.max(0, Math.min(100, getConfig().getInt(AUTO_DROP_TRAP_CHANCE, 0)));
    }

    public static List<String> getAutoDropTrapMobs() {
        List<String> mobs = getConfig().getStringList(AUTO_DROP_TRAP_MOBS);
        if (mobs.isEmpty()) {
            mobs = new ArrayList<>();
            mobs.add("ZOMBIE");
            mobs.add("SKELETON");
            mobs.add("CREEPER");
        }
        return mobs;
    }

    public static boolean isAutoDropLootScaling() {
        return getConfig().getBoolean(AUTO_DROP_LOOT_SCALING, false);
    }

    public static int getAutoDropLootScalingMax() {
        return Math.max(1, getConfig().getInt(AUTO_DROP_LOOT_SCALING_MAX, 20));
    }

    // Crate settings
    public static int getCrateExpiry() {
        return Math.max(0, getConfig().getInt(CRATE_EXPIRY, 1200));
    }

    public static int getCrateProtectionRadius() {
        return Math.max(0, getConfig().getInt(CRATE_PROTECTION_RADIUS, 2));
    }

    public static int getCrateMaxActive() {
        return Math.max(0, getConfig().getInt(CRATE_MAX_ACTIVE, 0));
    }

    public static int getCrateTeamOpenChance() {
        return Math.max(0, Math.min(100, getConfig().getInt(CRATE_TEAM_OPEN_CHANCE, 0)));
    }

    public static int getCrateTeamOpenPlayers() {
        return Math.max(2, getConfig().getInt(CRATE_TEAM_OPEN_RANGE, 2));
    }

    public static int getCrateTrapChance() {
        return Math.max(0, Math.min(100, getConfig().getInt(CRATE_TRAP_CHANCE, 0)));
    }

    public static List<String> getCrateTrapMobs() {
        List<String> mobs = getConfig().getStringList(CRATE_TRAP_MOBS);
        if (mobs.isEmpty()) {
            mobs = new ArrayList<>();
            mobs.add("ZOMBIE");
            mobs.add("SKELETON");
            mobs.add("CREEPER");
        }
        return mobs;
    }

    // Hologram settings
    public static boolean isHologramEnabled() {
        return getConfig().getBoolean(HOLOGRAM_ENABLED, true);
    }

    public static List<String> getHologramLines() {
        List<String> lines = getConfig().getStringList(HOLOGRAM_LINES);
        if (lines.isEmpty()) {
            lines = new ArrayList<>();
            lines.add("&b&l{template}");
            lines.add("{team}");
            lines.add("{team-progress}");
            lines.add("{time}");
            lines.add("&7Right-click to open");
        }
        return lines;
    }

    public static boolean isDebugLoggingEnabled() {
        return getConfig().getBoolean(LOGGING_DEBUG, false);
    }

    public static boolean isAnnouncementEnabled() {
        return getConfig().getBoolean(ANNOUNCE_ENABLED, true);
    }

    // Sanitizers
    static int sanitizeParachuteChickenCount(int count) {
        if (count < MIN_PARACHUTE_CHICKEN_COUNT || count > MAX_PARACHUTE_CHICKEN_COUNT) return DEFAULT_PARACHUTE_CHICKEN_COUNT;
        return count;
    }

    static double sanitizeDropFallingSpeed(double speed) {
        if (!Double.isFinite(speed)) return DEFAULT_DROP_FALLING_SPEED;
        if (speed < MIN_DROP_FALLING_SPEED || speed > MAX_DROP_FALLING_SPEED) return DEFAULT_DROP_FALLING_SPEED;
        return speed;
    }

    public static boolean isNotificationDefaultSubscribe() {
        return getConfig().getBoolean(NOTIFICATION_DEFAULT_SUBSCRIBE, true);
    }

    /**
     * Fall duration in seconds. When > 0, overrides falling-speed.
     * ParachuteSystem calculates velocity based on actual height and this duration.
     */
    public static int getDropFallDuration() {
        return Math.max(0, getConfig().getInt(DROP_FALL_DURATION, 0));
    }

    public static int getAutoDropFallDuration() {
        int val = getConfig().getInt(AUTO_DROP_FALL_DURATION, 0);
        return val > 0 ? val : getDropFallDuration();
    }

    public static int getCallFallDuration() {
        int val = getConfig().getInt(CALL_FALL_DURATION, 0);
        return val > 0 ? val : getDropFallDuration();
    }

    public static int getSpawnFallDuration() {
        int val = getConfig().getInt(SPAWN_FALL_DURATION, 0);
        return val > 0 ? val : getDropFallDuration();
    }

    static int sanitizeDropHeight(int height) {
        if (height < MIN_DROP_HEIGHT || height > MAX_DROP_HEIGHT) return DEFAULT_DROP_HEIGHT;
        return height;
    }

    static int sanitizeSmokeHeight(int height) {
        if (height < MIN_SMOKE_HEIGHT || height > MAX_SMOKE_HEIGHT) return DEFAULT_SMOKE_HEIGHT;
        return height;
    }

    private static FileConfiguration getConfig() {
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return FALLBACK_CONFIG;
        FileConfiguration fc = config.getConfig();
        return fc == null ? FALLBACK_CONFIG : fc;
    }
}
