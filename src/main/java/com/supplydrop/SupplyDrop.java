package com.supplydrop;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.supplydrop.commands.SupplyDropCommand;
import com.supplydrop.commands.SupplyDropTabCompleter;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.helpers.ChatTheme;
import com.supplydrop.helpers.CrateManager;
import com.supplydrop.helpers.DatabaseManager;
import com.supplydrop.helpers.HistoryManager;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.helpers.NotificationManager;
import com.supplydrop.helpers.RewindIntegration;
import com.supplydrop.helpers.ZoneManager;
import com.supplydrop.listeners.AntiGriefListener;
import com.supplydrop.listeners.CrateCleanupListener;
import com.supplydrop.listeners.CrateCloseListener;
import com.supplydrop.listeners.CrateDestroyListener;
import com.supplydrop.listeners.CrateOpenListener;
import com.supplydrop.listeners.ChatInputListener;
import com.supplydrop.listeners.FallingCrateListener;
import com.supplydrop.loot.Rarity;
import com.supplydrop.loot.RarityRegistry;
import com.supplydrop.packages.PackageManager;
import com.supplydrop.packages.PackagesGui;
import com.supplydrop.announce.AnnouncementManager;
import com.supplydrop.schedule.ScheduledDropManager;
import com.supplydrop.seasons.SeasonManager;
import com.supplydrop.stats.AutoDropStats;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class SupplyDrop extends JavaPlugin {

    public static final String PLUGIN_NAME = "SupplyDrop";
    public static final String SUPPLYDROP_COMMAND = "supplydrop";

    private static SupplyDrop pluginInstance;
    private static String pluginVersion;
    private static Config configuration;
    private static PackagesConfig packagesConfiguration;
    private static PackagesGui packagesGui;
    private AutoDropScheduler autoDropScheduler;
    private SeasonManager seasonManager;
    private ScheduledDropManager scheduledDropManager;
    private AutoDropStats autoDropStats;
    private final ConcurrentHashMap<UUID, Boolean> pendingTemplateCreations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, WeightEditData> pendingWeightEdits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, java.util.function.Consumer<Integer>> pendingNumberInputs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, java.util.function.Consumer<String>> pendingStringInputs = new ConcurrentHashMap<>();

    public record WeightEditData(String templateName, Rarity rarity, int filteredIndex, int page) {}

    @Override
    public void onEnable() {
        pluginInstance = this;
        pluginVersion = getDescription().getVersion();

        try {
            configuration = new Config(this);
            configuration.saveDefaultConfig();
            configuration.getConfig();

            RewindIntegration.init();
            com.supplydrop.integration.LandClaimHook.init();

            ChatTheme.init(configuration);
            ChatHandler.init(this);

            Objects.requireNonNull(this.getCommand(SUPPLYDROP_COMMAND)).setExecutor(new SupplyDropCommand());
            Objects.requireNonNull(this.getCommand(SUPPLYDROP_COMMAND)).setTabCompleter(new SupplyDropTabCompleter());

            Bukkit.getPluginManager().registerEvents(new FallingCrateListener(), this);
            Bukkit.getPluginManager().registerEvents(new CrateCloseListener(), this);
            Bukkit.getPluginManager().registerEvents(new CrateOpenListener(), this);
            Bukkit.getPluginManager().registerEvents(new CrateDestroyListener(), this);
            Bukkit.getPluginManager().registerEvents(new CrateCleanupListener(), this);
            Bukkit.getPluginManager().registerEvents(new AntiGriefListener(), this);
            Bukkit.getPluginManager().registerEvents(new ChatInputListener(), this);
            Bukkit.getPluginManager().registerEvents(new com.supplydrop.listeners.PluginEnableListener(), this);

            packagesConfiguration = new PackagesConfig(this);
            packagesConfiguration.getConfig();

            // Load rarity tiers from config
            RarityRegistry.load(configuration.getConfig().getConfigurationSection("rarities"));

            seasonManager = new SeasonManager();
            seasonManager.load(configuration);

            AnnouncementManager.load(configuration);

            autoDropStats = new AutoDropStats(getDataFolder());
            autoDropStats.load();

            PackageManager.reload();

            // Initialize database and load saved crates
            DatabaseManager.init(getDataFolder());

            // Initialize history database
            HistoryManager.init(getDataFolder());

            // Initialize notification manager
            NotificationManager.init(HistoryManager.getConnection(), ConfigKeys.isNotificationDefaultSubscribe());

            // Migrate from old crates.yml if it exists
            DatabaseManager.migrateFromYaml(getDataFolder());

            // Load crates from database into CrateManager
            List<DatabaseManager.CrateRecord> records = DatabaseManager.loadAll();
            int restored = 0;
            for (DatabaseManager.CrateRecord record : records) {
                if (record.location().getBlock().getState() instanceof org.bukkit.block.Barrel) {
                    Crate.State resumeState;
                    try {
                        resumeState = Crate.State.valueOf(record.state());
                    } catch (IllegalArgumentException e) {
                        resumeState = Crate.State.READY_TO_OPEN;
                    }

            Crate crate = Crate.createPersistedInState(record.uuid(), record.location(), record.displayName(),
                            record.isTrap(), record.isTeamCrate(), record.requiredPlayers(),
                            resumeState, record.lockDuration(), record.lockStartTime(),
                            record.expiryTicks(), record.landTime());
                    com.supplydrop.integration.LandClaimHook.tagCrate(record.location().getBlock(), record.uuid());
                    CrateManager.addCrate(record.location(), crate);
                    restored++;
                }
            }
            if (restored > 0) {
                AirdropLogger.info("Restored " + restored + " crate(s) from previous session.");
            }

            RewindIntegration.registerActiveCrates();

            if (ConfigKeys.isAutoDropEnabled()) {
                autoDropScheduler = new AutoDropScheduler(this);
                autoDropScheduler.start();
            }

            if (ConfigKeys.isAutoDropScheduledEnabled()) {
                scheduledDropManager = new ScheduledDropManager(this);
                scheduledDropManager.start();
            }

            // Start zone protection
            if (ConfigKeys.isZoneEnabled()) {
                ZoneManager.start(this);
            }

            AirdropLogger.info("SupplyDrop v" + pluginVersion + " enabled.");
        } catch (Exception e) {
            getLogger().severe("Failed to enable SupplyDrop: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        ZoneManager.stop();

        if (scheduledDropManager != null) {
            scheduledDropManager.stop();
            scheduledDropManager = null;
        }

        if (autoDropScheduler != null) {
            autoDropScheduler.stop();
            autoDropScheduler = null;
        }

        if (autoDropStats != null) {
            autoDropStats.save();
        }

        CrateManager.clearAll();
        DatabaseManager.shutdown();
        HistoryManager.shutdown();
        PackageManager.clear();
        RarityRegistry.clear();

        if (packagesGui != null) {
            HandlerList.unregisterAll(packagesGui);
            packagesGui = null;
        }

        HandlerList.unregisterAll(this);
        pluginInstance = null;
        pluginVersion = null;
        configuration = null;
        packagesConfiguration = null;
    }

    public void setupPackageGuis() {
        if (packagesGui != null) {
            HandlerList.unregisterAll(packagesGui);
        }
        packagesGui = new PackagesGui();
    }

    public void restartAutoDropScheduler() {
        if (autoDropScheduler != null) {
            autoDropScheduler.stop();
            autoDropScheduler = null;
        }
        if (ConfigKeys.isAutoDropEnabled()) {
            autoDropScheduler = new AutoDropScheduler(this);
            autoDropScheduler.start();
        }

        // Also restart scheduled drop manager
        if (scheduledDropManager != null) {
            scheduledDropManager.stop();
            scheduledDropManager = null;
        }
        if (ConfigKeys.isAutoDropScheduledEnabled()) {
            scheduledDropManager = new ScheduledDropManager(this);
            scheduledDropManager.start();
        }
    }

    public static SupplyDrop getPluginInstance() {
        return pluginInstance;
    }

    public static String getPluginVersion() {
        return pluginVersion;
    }

    public static Config getConfiguration() {
        return configuration;
    }

    public static PackagesConfig getPackagesConfiguration() {
        return packagesConfiguration;
    }

    public static PackagesGui getPackagesGui() {
        return packagesGui;
    }

    public AutoDropScheduler getAutoDropScheduler() {
        return autoDropScheduler;
    }

    public SeasonManager getSeasonManager() {
        return seasonManager;
    }

    public AutoDropStats getAutoDropStats() {
        return autoDropStats;
    }

    public ScheduledDropManager getScheduledDropManager() {
        return scheduledDropManager;
    }

    public void setPendingTemplateCreation(UUID playerId) {
        pendingTemplateCreations.put(playerId, true);
    }

    public boolean consumePendingTemplateCreation(UUID playerId) {
        return pendingTemplateCreations.remove(playerId) != null;
    }

    public void setPendingWeightEdit(UUID playerId, String templateName, Rarity rarity, int filteredIndex, int page) {
        pendingWeightEdits.put(playerId, new WeightEditData(templateName, rarity, filteredIndex, page));
    }

    public WeightEditData consumePendingWeightEdit(UUID playerId) {
        return pendingWeightEdits.remove(playerId);
    }

    public void setPendingNumberInput(UUID playerId, java.util.function.Consumer<Integer> callback) {
        pendingNumberInputs.put(playerId, callback);
    }

    public java.util.function.Consumer<Integer> consumePendingNumberInput(UUID playerId) {
        return pendingNumberInputs.remove(playerId);
    }

    public boolean hasPendingNumberInput(UUID playerId) {
        return pendingNumberInputs.containsKey(playerId);
    }

    public void setPendingStringInput(UUID playerId, java.util.function.Consumer<String> callback) {
        pendingStringInputs.put(playerId, callback);
    }

    public java.util.function.Consumer<String> consumePendingStringInput(UUID playerId) {
        return pendingStringInputs.remove(playerId);
    }

    public boolean hasPendingStringInput(UUID playerId) {
        return pendingStringInputs.containsKey(playerId);
    }

    public void cancelPendingInputs(UUID playerId) {
        pendingNumberInputs.remove(playerId);
        pendingStringInputs.remove(playerId);
    }
}
