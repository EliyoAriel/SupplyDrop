package com.supplydrop.gui.config;

import com.supplydrop.SupplyDrop;
import com.supplydrop.Config;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.helpers.ChatHandler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reusable GUI for toggling trap mob types on/off.
 * Accepts a config key path (e.g. "crate.trap-mobs" or "auto-drop.trap-mobs").
 *
 * Layout (54 slots):
 *   Rows 0-4 (0-44): mob items
 *   Row 5 (45-53): glass | glass | glass | glass | [BACK] | glass | glass | glass | glass
 */
public class ConfigTrapMobsGui implements Listener {

    private static final int SIZE = 54;
    private static final int BACK_SLOT = 49;

    private static final Map<String, Material> MOB_MATERIALS = new LinkedHashMap<>();
    static {
        MOB_MATERIALS.put("ZOMBIE", Material.ZOMBIE_HEAD);
        MOB_MATERIALS.put("SKELETON", Material.SKELETON_SKULL);
        MOB_MATERIALS.put("CREEPER", Material.CREEPER_HEAD);
        MOB_MATERIALS.put("SPIDER", Material.SPAWNER);
        MOB_MATERIALS.put("CAVE_SPIDER", Material.SPAWNER);
        MOB_MATERIALS.put("ENDERMAN", Material.ENDER_PEARL);
        MOB_MATERIALS.put("WITCH", Material.GLASS_BOTTLE);
        MOB_MATERIALS.put("BLAZE", Material.BLAZE_ROD);
        MOB_MATERIALS.put("GHAST", Material.GHAST_TEAR);
        MOB_MATERIALS.put("MAGMA_CUBE", Material.MAGMA_CREAM);
        MOB_MATERIALS.put("SLIME", Material.SLIME_BALL);
        MOB_MATERIALS.put("PIG_ZOMBIE", Material.GOLDEN_SWORD);
        MOB_MATERIALS.put("ZOMBIFIED_PIGLIN", Material.GOLDEN_SWORD);
        MOB_MATERIALS.put("HUSK", Material.ZOMBIE_HEAD);
        MOB_MATERIALS.put("DROWNED", Material.TRIDENT);
        MOB_MATERIALS.put("STRAY", Material.ARROW);
        MOB_MATERIALS.put("VINDICATOR", Material.IRON_AXE);
        MOB_MATERIALS.put("EVOKER", Material.TOTEM_OF_UNDYING);
        MOB_MATERIALS.put("PILLAGER", Material.CROSSBOW);
        MOB_MATERIALS.put("RAVAGER", Material.SADDLE);
        MOB_MATERIALS.put("WARDEN", Material.ECHO_SHARD);
        MOB_MATERIALS.put("SHULKER", Material.SHULKER_SHELL);
        MOB_MATERIALS.put("GUARDIAN", Material.PRISMARINE_CRYSTALS);
        MOB_MATERIALS.put("ELDER_GUARDIAN", Material.SPONGE);
        MOB_MATERIALS.put("PHEROMONE", Material.HONEYCOMB);
        MOB_MATERIALS.put("CAVE_SPIDER", Material.FERMENTED_SPIDER_EYE);
        MOB_MATERIALS.put("SILVERFISH", Material.STONE);
        MOB_MATERIALS.put("SKELETON_HORSE", Material.BONE);
        MOB_MATERIALS.put("STRIDER", Material.WARPED_FUNGUS_ON_A_STICK);
        MOB_MATERIALS.put("HOGLIN", Material.PORKCHOP);
        MOB_MATERIALS.put("ZOGLIN", Material.ROTTEN_FLESH);
        MOB_MATERIALS.put("PANDA", Material.BAMBOO);
        MOB_MATERIALS.put("GOAT", Material.GOAT_HORN);
        MOB_MATERIALS.put("AXOLOTL", Material.TROPICAL_FISH_BUCKET);
        MOB_MATERIALS.put("FROGLIGHT", Material.OCHRE_FROGLIGHT);
        MOB_MATERIALS.put("TURTLE", Material.TURTLE_SCUTE);
        MOB_MATERIALS.put("PHANTOM", Material.PHANTOM_MEMBRANE);
        MOB_MATERIALS.put("VEX", Material.IRON_SWORD);
        MOB_MATERIALS.put("BREEZE", Material.BREEZE_ROD);
        MOB_MATERIALS.put("BREEZE_VEIL", Material.BREEZE_ROD);
        MOB_MATERIALS.put("CREAKING", Material.PALE_OAK_LOG);
    }

    private final Inventory inv;
    private final String configKey;
    private final String title;
    private final Runnable onBack;
    private boolean listenerRegistered = false;

    public ConfigTrapMobsGui(String configKey, String title, Runnable onBack) {
        this.configKey = configKey;
        this.title = title;
        this.onBack = onBack;
        this.inv = Bukkit.createInventory(null, SIZE, title);
    }

    private Set<String> loadEnabledMobs() {
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return new TreeSet<>();
        FileConfiguration fc = config.getConfig();
        if (fc == null) return new TreeSet<>();
        List<String> list = fc.getStringList(configKey);
        return new TreeSet<>(list);
    }

    private void saveEnabledMobs(Set<String> mobs) {
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return;
        config.getConfig().set(configKey, new ArrayList<>(mobs));
        config.saveConfig();
    }

    public void initializeItems() {
        inv.clear();

        Set<String> enabledMobs = loadEnabledMobs();

        // All known hostile/neutral mobs
        List<String> allMobs = new ArrayList<>(MOB_MATERIALS.keySet());

        // Add any mobs already in config that aren't in our known list
        for (String mob : enabledMobs) {
            if (!allMobs.contains(mob)) allMobs.add(mob);
        }

        int slot = 0;
        for (String mobName : allMobs) {
            if (slot >= 45) break;

            boolean enabled = enabledMobs.contains(mobName);
            Material mat = MOB_MATERIALS.getOrDefault(mobName, Material.SPAWNER);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String status = enabled ? "§aON" : "§cOFF";
                meta.setDisplayName("§e" + mobName + " " + status);
                List<String> lore = new ArrayList<>();
                lore.add("§7Click to toggle");
                if (enabled) {
                    lore.add("§aCurrently: enabled");
                } else {
                    lore.add("§cCurrently: disabled");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slot++;
        }

        // Fill rest with glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) { glassMeta.setDisplayName(" "); glass.setItemMeta(glassMeta); }
        for (int i = slot; i < 45; i++) {
            inv.setItem(i, glass);
        }

        // Row 5: all glass + back
        for (int i = 45; i < SIZE; i++) {
            inv.setItem(i, glass);
        }

        ItemStack backBtn = new ItemStack(Material.BLUE_WOOL);
        ItemMeta backMeta = backBtn.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§9← Back");
            backBtn.setItemMeta(backMeta);
        }
        inv.setItem(BACK_SLOT, backBtn);
    }

    public void openInventory(HumanEntity ent) {
        ensureListenerRegistered();
        initializeItems();
        ent.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        if (e.getRawSlot() >= SIZE) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int slot = e.getRawSlot();

        // Back
        if (slot == BACK_SLOT) {
            onBack.run();
            return;
        }

        // Mob items (0-44)
        if (slot < 0 || slot >= 45) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        // Extract mob name: "§e<MOB> §aON" or "§e<MOB> §cOFF"
        String raw = meta.getDisplayName();
        String mobName = raw.replace("§e", "").trim();
        if (mobName.endsWith(" §aON")) mobName = mobName.substring(0, mobName.length() - 5).trim();
        else if (mobName.endsWith(" §cOFF")) mobName = mobName.substring(0, mobName.length() - 6).trim();

        final String finalMobName = mobName;

        Set<String> enabledMobs = loadEnabledMobs();

        if (enabledMobs.contains(finalMobName)) {
            enabledMobs.remove(finalMobName);
            ChatHandler.send(p, "§c" + finalMobName + " §7disabled for trap.");
        } else {
            enabledMobs.add(finalMobName);
            ChatHandler.send(p, "§a" + finalMobName + " §7enabled for trap.");
        }

        saveEnabledMobs(enabledMobs);
        initializeItems();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) { if (e.getInventory().equals(inv)) e.setCancelled(true); }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!e.getInventory().equals(inv)) return;
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin == null || !plugin.isEnabled()) { unregister(); return; }
        Bukkit.getScheduler().runTask(plugin, this::unregisterIfIdle);
    }

    private void ensureListenerRegistered() {
        if (listenerRegistered) return;
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin != null && plugin.isEnabled()) { Bukkit.getPluginManager().registerEvents(this, plugin); listenerRegistered = true; }
    }
    private void unregister() { HandlerList.unregisterAll(this); listenerRegistered = false; }
    private void unregisterIfIdle() { if (!inv.getViewers().isEmpty()) return; unregister(); }
}
