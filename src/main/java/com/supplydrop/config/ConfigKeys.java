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
    public static final String AUTO_DROP_SCALING_ENABLED = "auto-drop.scaling.enabled";
    public static final String AUTO_DROP_SCALING_MIN_PLAYERS = "auto-drop.scaling.min-players";
    public static final String AUTO_DROP_SCALING_MAX_PLAYERS = "auto-drop.scaling.max-players";
    public static final String AUTO_DROP_SCALING_WAVE_MIN = "auto-drop.scaling.wave-min";
    public static final String AUTO_DROP_SCALING_WAVE_MAX = "auto-drop.scaling.wave-max";
    public static final String AUTO_DROP_SCALING_BONUS_ROLLS_PER_PLAYER = "auto-drop.scaling.bonus-rolls-per-player";
    public static final String AUTO_DROP_QUEUE_ENABLED = "auto-drop.queue.enabled";
    public static final String AUTO_DROP_QUEUE_POLL_INTERVAL = "auto-drop.queue.poll-interval";
    public static final String AUTO_DROP_QUEUE_MAX_SIZE = "auto-drop.queue.max-queue-size";
    public static final String AUTO_DROP_CHAIN_ENABLED = "auto-drop.chain.enabled";
    public static final String AUTO_DROP_CHAIN_CHANCE = "auto-drop.chain.chance";
    public static final String AUTO_DROP_CHAIN_COUNT = "auto-drop.chain.count";
    public static final String AUTO_DROP_CHAIN_INTERVAL = "auto-drop.chain.interval";
    public static final String AUTO_DROP_CHAIN_DECREASE_INTERVAL = "auto-drop.chain.decrease-interval";
    public static final String AUTO_DROP_ROTATION_ENABLED = "auto-drop.rotation.enabled";
    public static final String AUTO_DROP_ESCALATION_ENABLED = "auto-drop.escalation.enabled";
    public static final String AUTO_DROP_ESCALATION_INCREMENT = "auto-drop.escalation.increment";
    public static final String AUTO_DROP_ESCALATION_CAP = "auto-drop.escalation.cap";
    public static final String AUTO_DROP_ESCALATION_RESET_ON_OPEN = "auto-drop.escalation.reset-on-open";
    public static final String AUTO_DROP_SCHEDULED_ENABLED = "auto-drop.scheduled.enabled";
    public static final String AUTO_DROP_SCHEDULED_TIMES = "auto-drop.scheduled.times";
    public static final String AUTO_DROP_SCHEDULED_WORLD_OVERRIDE = "auto-drop.scheduled.world-override";
    public static final String AUTO_DROP_WARNING_ENABLED = "auto-drop.warning.enabled";
    public static final String AUTO_DROP_WARNING_SECONDS_BEFORE = "auto-drop.warning.seconds-before";
    public static final String AUTO_DROP_WARNING_MESSAGE = "auto-drop.warning.message";

    public static final String NOTIFICATION_DEFAULT_SUBSCRIBE = "notification.default-subscribe";
    public static final String ANNOUNCE_ENABLED = "announce.enabled";

    public static final String DROP_FALL_DURATION = "drop.fall-duration";
    public static final String DROP_AVOID_CLAIMS = "drop.avoid-claims";

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
    public static final String CRATE_LOCK_ENABLED = "crate.lock.enabled";
    public static final String CRATE_LOCK_DURATION = "crate.lock.duration";
    public static final String CRATE_LOCK_RANDOM = "crate.lock.random";
    public static final String CRATE_LOCK_DURATION_MIN = "crate.lock.duration-min";
    public static final String CRATE_LOCK_DURATION_MAX = "crate.lock.duration-max";
    public static final String CRATE_LOCK_SOUND_LOCK = "crate.lock.sound-lock";
    public static final String CRATE_LOCK_SOUND_READY = "crate.lock.sound-ready";
    public static final String CRATE_LOCK_READY_NOTIFICATION = "crate.lock.ready-notification";
    public static final String CRATE_LOCK_READY_NOTIFICATION_RADIUS = "crate.lock.ready-notification-radius";
    public static final String CRATE_LOCK_BREAK_BEHAVIOR = "crate.lock.break-behavior";
    public static final String CRATE_LOCK_PARTICLE_ENABLED = "crate.lock.particle.enabled";
    public static final String CRATE_LOCK_PARTICLE_TYPE = "crate.lock.particle.type";
    public static final String CRATE_LOCK_PARTICLE_RADIUS = "crate.lock.particle.radius";

    // Zone settings
    public static final String ZONE_ENABLED = "crate.zone.enabled";
    public static final String ZONE_RADIUS = "crate.zone.radius";
    public static final String ZONE_PARTICLE = "crate.zone.particle";
    public static final String ZONE_DENY_MESSAGE = "crate.zone.deny-message";

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
        int max = Math.max(1, getConfig().getInt(AUTO_DROP_WAVE_COUNT, 1));
        if (max <= 1) return 1;
        return 2 + new java.util.Random().nextInt(Math.max(1, max - 1));
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
            return getCrateTrapMobs();
        }
        return mobs;
    }

    public static int getAutoDropTeamCrateChance() {
        return Math.max(0, Math.min(100, getConfig().getInt(AUTO_DROP_TEAM_CRATE_CHANCE, 0)));
    }

    public static int getAutoDropTeamCrateRange() {
        return Math.max(2, getConfig().getInt(AUTO_DROP_TEAM_CRATE_RANGE, 2));
    }

    public static boolean isAutoDropLootScaling() {
        return getConfig().getBoolean(AUTO_DROP_LOOT_SCALING, false);
    }

    public static int getAutoDropLootScalingMax() {
        return Math.max(1, getConfig().getInt(AUTO_DROP_LOOT_SCALING_MAX, 20));
    }

    // Player count scaling
    public static boolean isAutoDropScalingEnabled() {
        return getConfig().getBoolean(AUTO_DROP_SCALING_ENABLED, false);
    }

    public static int getAutoDropScalingMinPlayers() {
        return Math.max(1, getConfig().getInt(AUTO_DROP_SCALING_MIN_PLAYERS, 1));
    }

    public static int getAutoDropScalingMaxPlayers() {
        return Math.max(getAutoDropScalingMinPlayers(), getConfig().getInt(AUTO_DROP_SCALING_MAX_PLAYERS, 20));
    }

    public static int getAutoDropScalingWaveMin() {
        return Math.max(1, getConfig().getInt(AUTO_DROP_SCALING_WAVE_MIN, 1));
    }

    public static int getAutoDropScalingWaveMax() {
        return Math.max(getAutoDropScalingWaveMin(), getConfig().getInt(AUTO_DROP_SCALING_WAVE_MAX, 5));
    }

    public static int getAutoDropScalingBonusRollsPerPlayer() {
        return Math.max(0, getConfig().getInt(AUTO_DROP_SCALING_BONUS_ROLLS_PER_PLAYER, 0));
    }

    public static int getAutoDropWaveCountForPlayers(int onlineCount) {
        if (!isAutoDropScalingEnabled()) {
            return getAutoDropWaveCount();
        }
        int min = getAutoDropScalingMinPlayers();
        int max = getAutoDropScalingMaxPlayers();
        int waveMin = getAutoDropScalingWaveMin();
        int waveMax = getAutoDropScalingWaveMax();

        if (onlineCount <= min) return waveMin;
        if (onlineCount >= max) return waveMax;

        double t = (double) (onlineCount - min) / (max - min);
        int waveCount = (int) Math.round(waveMin + t * (waveMax - waveMin));
        return Math.max(waveMin, Math.min(waveMax, waveCount));
    }

    public static int getAutoDropBonusRollsForPlayers(int onlineCount) {
        if (!isAutoDropScalingEnabled()) return 0;
        int min = getAutoDropScalingMinPlayers();
        int bonusPerPlayer = getAutoDropScalingBonusRollsPerPlayer();
        if (bonusPerPlayer <= 0) return 0;
        return Math.max(0, (onlineCount - min) * bonusPerPlayer);
    }

    // Drop queuing
    public static boolean isAutoDropQueueEnabled() {
        return getConfig().getBoolean(AUTO_DROP_QUEUE_ENABLED, false);
    }

    public static int getAutoDropQueuePollInterval() {
        return Math.max(20, getConfig().getInt(AUTO_DROP_QUEUE_POLL_INTERVAL, 100));
    }

    public static int getAutoDropQueueMaxSize() {
        return Math.max(1, getConfig().getInt(AUTO_DROP_QUEUE_MAX_SIZE, 5));
    }

    // Drop chains
    public static boolean isAutoDropChainEnabled() {
        return getConfig().getBoolean(AUTO_DROP_CHAIN_ENABLED, false);
    }

    public static int getAutoDropChainChance() {
        return Math.max(0, Math.min(100, getConfig().getInt(AUTO_DROP_CHAIN_CHANCE, 0)));
    }

    public static int getAutoDropChainCount() {
        return Math.max(1, getConfig().getInt(AUTO_DROP_CHAIN_COUNT, 3));
    }

    public static int getAutoDropChainInterval() {
        return Math.max(100, getConfig().getInt(AUTO_DROP_CHAIN_INTERVAL, 1200));
    }

    public static boolean isAutoDropChainDecreaseInterval() {
        return getConfig().getBoolean(AUTO_DROP_CHAIN_DECREASE_INTERVAL, true);
    }

    // Loot rotation
    public static boolean isAutoDropRotationEnabled() {
        return getConfig().getBoolean(AUTO_DROP_ROTATION_ENABLED, false);
    }

    // Escalating difficulty
    public static boolean isAutoDropEscalationEnabled() {
        return getConfig().getBoolean(AUTO_DROP_ESCALATION_ENABLED, false);
    }

    public static int getAutoDropEscalationIncrement() {
        return Math.max(0, getConfig().getInt(AUTO_DROP_ESCALATION_INCREMENT, 5));
    }

    public static int getAutoDropEscalationCap() {
        return Math.max(0, Math.min(100, getConfig().getInt(AUTO_DROP_ESCALATION_CAP, 50)));
    }

    public static boolean isAutoDropEscalationResetOnOpen() {
        return getConfig().getBoolean(AUTO_DROP_ESCALATION_RESET_ON_OPEN, true);
    }

    // Scheduled drops
    public static boolean isAutoDropScheduledEnabled() {
        return getConfig().getBoolean(AUTO_DROP_SCHEDULED_ENABLED, false);
    }

    public static List<String> getAutoDropScheduledTimes() {
        return getConfig().getStringList(AUTO_DROP_SCHEDULED_TIMES);
    }

    public static String getAutoDropScheduledWorldOverride() {
        String override = getConfig().getString(AUTO_DROP_SCHEDULED_WORLD_OVERRIDE);
        return (override != null && !override.isEmpty()) ? override : null;
    }

    // Drop warnings
    public static boolean isAutoDropWarningEnabled() {
        return getConfig().getBoolean(AUTO_DROP_WARNING_ENABLED, false);
    }

    public static List<Integer> getAutoDropWarningSecondsBefore() {
        List<Integer> result = new ArrayList<>();
        List<String> raw = getConfig().getStringList(AUTO_DROP_WARNING_SECONDS_BEFORE);
        if (raw.isEmpty()) {
            result.add(60);
            result.add(30);
            result.add(10);
        } else {
            for (String s : raw) {
                try {
                    result.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    public static String getAutoDropWarningMessage() {
        return getConfig().getString(AUTO_DROP_WARNING_MESSAGE, "&e⚠ Supply drop in &c{seconds}s &e!");
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

    public static boolean isCrateLockEnabled() {
        return getConfig().getBoolean(CRATE_LOCK_ENABLED, true);
    }

    public static int getCrateLockDuration() {
        return Math.max(0, getConfig().getInt(CRATE_LOCK_DURATION, 200));
    }

    public static boolean isCrateLockRandom() {
        return getConfig().getBoolean(CRATE_LOCK_RANDOM, false);
    }

    public static int getCrateLockDurationMin() {
        return Math.max(0, getConfig().getInt(CRATE_LOCK_DURATION_MIN, 100));
    }

    public static int getCrateLockDurationMax() {
        return Math.max(0, getConfig().getInt(CRATE_LOCK_DURATION_MAX, 400));
    }

    public static String getCrateLockSoundLock() {
        return getConfig().getString(CRATE_LOCK_SOUND_LOCK, "entity.iron_door.close");
    }

    public static String getCrateLockSoundReady() {
        return getConfig().getString(CRATE_LOCK_SOUND_READY, "entity.player.levelup");
    }

    public static boolean isCrateLockReadyNotification() {
        return getConfig().getBoolean(CRATE_LOCK_READY_NOTIFICATION, true);
    }

    public static int getCrateLockReadyNotificationRadius() {
        return Math.max(1, getConfig().getInt(CRATE_LOCK_READY_NOTIFICATION_RADIUS, 20));
    }

    public static String getCrateLockBreakBehavior() {
        return getConfig().getString(CRATE_LOCK_BREAK_BEHAVIOR, "destroy");
    }

    public static boolean isCrateLockParticleEnabled() {
        return getConfig().getBoolean(CRATE_LOCK_PARTICLE_ENABLED, true);
    }

    public static String getCrateLockParticleType() {
        return getConfig().getString(CRATE_LOCK_PARTICLE_TYPE, "ENCHANTMENT_TABLE");
    }

    public static double getCrateLockParticleRadius() {
        return Math.max(0.5, getConfig().getDouble(CRATE_LOCK_PARTICLE_RADIUS, 1.5));
    }

    // Zone settings
    public static boolean isZoneEnabled() {
        return getConfig().getBoolean(ZONE_ENABLED, true);
    }

    public static double getZoneRadius() {
        return Math.max(5, getConfig().getDouble(ZONE_RADIUS, 25));
    }

    public static String getZoneParticle() {
        return getConfig().getString(ZONE_PARTICLE, "FLAME");
    }

    public static String getZoneDenyMessage() {
        return getConfig().getString(ZONE_DENY_MESSAGE, "&c&lSupply Drop &7- &fYou cannot enter the drop zone!");
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

    public static boolean isAvoidClaimsEnabled() {
        return getConfig().getBoolean(DROP_AVOID_CLAIMS, false);
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
