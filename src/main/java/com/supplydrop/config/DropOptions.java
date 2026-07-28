package com.supplydrop.config;

import java.util.List;

/**
 * Resolved drop options — single source of truth for all crate settings.
 * Build chain: template > context override (auto-drop/call/spawn) > crate.* global
 * Null field = use global default from crate.*
 */
public class DropOptions {

    private Integer chickenCount;
    private Double fallingSpeed;
    private Integer dropHeight;
    private Boolean showLandingEffects;
    private Boolean showContinuousEffects;
    private Boolean showFlareEffects;
    private Boolean smokeEnabled;
    private Integer smokeHeight;
    private Integer expiryTicks;
    private Integer fallDuration;
    private Integer trapChance;
    private List<String> trapMobs;
    private Integer teamCrateChance;
    private Integer teamCrateRange;

    public static DropOptions createDefault() {
        return new DropOptions();
    }

    public DropOptions withChickenCount(int count) {
        this.chickenCount = count;
        return this;
    }

    public DropOptions withFallingSpeed(double speed) {
        this.fallingSpeed = speed;
        return this;
    }

    public DropOptions withDropHeight(int height) {
        this.dropHeight = height;
        return this;
    }

    public DropOptions withLandingEffects(boolean show) {
        this.showLandingEffects = show;
        return this;
    }

    public DropOptions withContinuousEffects(boolean show) {
        this.showContinuousEffects = show;
        return this;
    }

    public DropOptions withFlareEffects(boolean show) {
        this.showFlareEffects = show;
        return this;
    }

    public DropOptions withSmokeEnabled(boolean enabled) {
        this.smokeEnabled = enabled;
        return this;
    }

    public DropOptions withSmokeHeight(int height) {
        this.smokeHeight = height;
        return this;
    }

    public DropOptions withExpiryTicks(int ticks) {
        this.expiryTicks = ticks;
        return this;
    }

    public DropOptions withFallDuration(int duration) {
        this.fallDuration = duration;
        return this;
    }

    public DropOptions withTrapChance(Integer chance) {
        this.trapChance = chance;
        return this;
    }

    public DropOptions withTrapMobs(List<String> mobs) {
        this.trapMobs = mobs;
        return this;
    }

    public DropOptions withTeamCrateChance(Integer chance) {
        this.teamCrateChance = chance;
        return this;
    }

    public DropOptions withTeamCrateRange(Integer range) {
        this.teamCrateRange = range;
        return this;
    }

    // ─── GETTERS (null = fall back to crate.* global) ────────────────

    public int getChickenCount() {
        return chickenCount != null ? ConfigKeys.sanitizeParachuteChickenCount(chickenCount) : ConfigKeys.getParachuteChickenCount();
    }

    public double getFallingSpeed() {
        return fallingSpeed != null ? ConfigKeys.sanitizeDropFallingSpeed(fallingSpeed) : ConfigKeys.getDropFallingSpeed();
    }

    public int getDropHeight() {
        return dropHeight != null ? ConfigKeys.sanitizeDropHeight(dropHeight) : ConfigKeys.getDropHeight();
    }

    public boolean shouldShowLandingEffects() {
        return showLandingEffects != null ? showLandingEffects : ConfigKeys.shouldShowLandingParticleEffects();
    }

    public boolean shouldShowContinuousEffects() {
        return showContinuousEffects != null ? showContinuousEffects : ConfigKeys.shouldShowContinuousParticleEffects();
    }

    public boolean shouldShowFlareEffects() {
        return showFlareEffects != null ? showFlareEffects : ConfigKeys.shouldShowFlareParticleEffects();
    }

    public boolean isSmokeEnabled() {
        return smokeEnabled != null ? smokeEnabled : ConfigKeys.isSmokeEnabled();
    }

    public int getSmokeHeight() {
        return smokeHeight != null ? ConfigKeys.sanitizeSmokeHeight(smokeHeight) : ConfigKeys.getSmokeHeight();
    }

    public int getExpiryTicks() {
        return expiryTicks != null ? Math.max(0, expiryTicks) : ConfigKeys.getCrateExpiry();
    }

    public int getFallDuration() {
        return fallDuration != null ? Math.max(0, fallDuration) : 0;
    }

    /**
     * Trap chance override. Returns null if not set (caller should use crate.* global).
     */
    public Integer getTrapChance() {
        return trapChance;
    }

    /**
     * Trap mobs override. Returns null if not set (caller should use crate.* global).
     */
    public List<String> getTrapMobs() {
        return trapMobs;
    }

    /**
     * Team crate chance override. Returns null if not set (caller should use crate.* global).
     */
    public Integer getTeamCrateChance() {
        return teamCrateChance;
    }

    /**
     * Team crate range override. Returns null if not set (caller should use crate.* global).
     */
    public Integer getTeamCrateRange() {
        return teamCrateRange;
    }
}
