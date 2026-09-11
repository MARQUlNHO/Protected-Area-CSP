package me.marquinho.protectedarea.managers;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.util.SimpleYaml;
import me.marquinho.protectedarea.util.TextUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NotificationManager {

    private final ProtectedAreaInit plugin;
    private final File notificationsFolder;
    private final Map<String, SimpleYaml> configCache;
    private final Map<UUID, TickBucket> recentMessages;

    private static final int PRUNE_THRESHOLD = 256;
    private static final int STALE_TICKS = 20;

    private static final class TickBucket {
        private int tick = Integer.MIN_VALUE;
        private final Set<String> messages = new HashSet<>();
    }

    public NotificationManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        this.notificationsFolder = new File(plugin.getDataPath(), "AreaNotification");
        this.configCache = new HashMap<>();
        this.recentMessages = new HashMap<>();

        notificationsFolder.mkdirs();
        createDefaultConfigs();
    }

    private void createDefaultConfigs() {
        createRulesConfig();
        createAdvancedRulesConfig();
    }

    private void createRulesConfig() {
        File file = new File(notificationsFolder, "Rules.yml");
        if (file.exists()) return;

        SimpleYaml config = new SimpleYaml();
        config.set("no_break", "<red>You can't break blocks in this area!</red>");
        config.set("no_place", "<red>You can't place blocks in this area!</red>");
        config.set("no_place_fluid", "<red>You can't place fluids in this area!</red>");
        config.set("no_interact", "<red>You can't interact in this area!</red>");
        config.set("no_interact_entity", "<red>You can't interact with entities in this area!</red>");
        config.set("no_interact_vehicle", "<red>You can't ride vehicles in this area!</red>");
        config.set("no_interact_inventory", "<red>You can't open inventories in this area!</red>");
        config.set("no_pvp", "<red>You can't attack players in this area!</red>");
        config.set("no_entityattack", "<red>You can't attack entities in this area!</red>");
        config.set("no_drop", "<red>You can't drop items in this area!</red>");
        config.set("no_collect", "<red>You can't collect items in this area!</red>");
        config.set("no_entry_collision", "");
        config.set("no_exit_collision", "");
        config.set("no_exit_returned", "");

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().error("Error creating Rules.yml", e);
        }
    }

    private void createAdvancedRulesConfig() {
        File file = new File(notificationsFolder, "AdvancedRules.yml");
        if (file.exists()) return;

        SimpleYaml config = new SimpleYaml();
        config.set("no_break_specific", "<red>You can't break <gold>{blockid}</gold> in this area!</red>");
        config.set("no_place_specific", "<red>You can't place <gold>{blockid}</gold> in this area!</red>");
        config.set("no_place_fluid_specific", "<red>You can't place <gold>{blockid}</gold> in this area!</red>");
        config.set("no_interact_block", "<red>You can't interact with <gold>{blockid}</gold> in this area!</red>");
        config.set("no_interact_entity", "<red>You can't interact with <gold>{entityid}</gold> in this area!</red>");
        config.set("no_interact_vehicle", "<red>You can't ride <gold>{entityid}</gold> in this area!</red>");
        config.set("no_interact_inventory", "<red>You can't open inventories of <gold>{entityid}</gold> in this area!</red>");
        config.set("no_drop_specific", "<red>You can't drop <gold>{itemid}</gold> in this area!</red>");
        config.set("no_collect_specific", "<red>You can't collect <gold>{itemid}</gold> in this area!</red>");

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().error("Error creating AdvancedRules.yml", e);
        }
    }

    public void reloadConfigs() {
        configCache.clear();
    }

    private SimpleYaml getConfig(String fileName) {
        if (configCache.containsKey(fileName)) return configCache.get(fileName);

        File file = new File(notificationsFolder, fileName + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warn("Notification file not found: " + fileName + ".yml");
            return new SimpleYaml();
        }

        SimpleYaml config = SimpleYaml.load(file);
        configCache.put(fileName, config);
        return config;
    }

    public void sendNotification(ServerPlayerEntity player, String configFile, String key, Map<String, String> placeholders) {
        SimpleYaml config = getConfig(configFile);
        String message = config.getString(key);

        if (message == null || message.isEmpty()) return;

        message = replacePlaceholders(message, placeholders);

        if (isDuplicateThisTick(player, configFile + ":" + key + ":" + message)) return;

        player.sendMessage(TextUtil.parse(message));
    }

    private boolean isDuplicateThisTick(ServerPlayerEntity player, String identity) {
        if (plugin.getServer() == null) return false;

        int tick = plugin.getServer().getTicks();
        TickBucket bucket = recentMessages.get(player.getUuid());

        if (bucket == null) {
            pruneStaleBuckets(tick);
            bucket = new TickBucket();
            recentMessages.put(player.getUuid(), bucket);
        }

        if (bucket.tick != tick) {
            bucket.tick = tick;
            bucket.messages.clear();
        }

        return !bucket.messages.add(identity);
    }

    private void pruneStaleBuckets(int tick) {
        if (recentMessages.size() < PRUNE_THRESHOLD) return;

        Iterator<TickBucket> it = recentMessages.values().iterator();
        while (it.hasNext()) {
            if (tick - it.next().tick > STALE_TICKS) it.remove();
        }
    }

    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) return message;
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public void sendAdvancedRulesMessage(ServerPlayerEntity player, String key, Map<String, String> placeholders) {
        sendNotification(player, "AdvancedRules", key, placeholders);
    }

    public void sendCustomMessage(ServerPlayerEntity player, String configFileName, String key, Map<String, String> placeholders) {
        sendNotification(player, configFileName, key, placeholders);
    }

    public static Map<String, String> createPlaceholders() {
        return new HashMap<>();
    }

    public static Map<String, String> createFullPlaceholders(String blockId, String entityId, String areaId, String playerName) {
        Map<String, String> placeholders = new HashMap<>();

        if (blockId != null && !blockId.isEmpty()) {
            placeholders.put("blockid", blockId);
            placeholders.put("block", getBlockName(blockId));
            placeholders.put("itemid", blockId);
            placeholders.put("item", getBlockName(blockId));
        }
        if (entityId != null && !entityId.isEmpty()) {
            placeholders.put("entityid", entityId);
            placeholders.put("entity", getEntityName(entityId));
        }
        if (areaId != null && !areaId.isEmpty()) {
            placeholders.put("areaid", areaId);
        }
        if (playerName != null && !playerName.isEmpty()) {
            placeholders.put("player", playerName);
        }
        return placeholders;
    }

    public static String getBlockName(String blockId) {
        if (blockId == null || blockId.isEmpty()) return "";
        return blockId.contains(":") ? blockId.split(":")[1] : blockId;
    }

    public static String getEntityName(String entityId) {
        if (entityId == null || entityId.isEmpty()) return "";
        return entityId.contains(":") ? entityId.split(":")[1] : entityId;
    }

    public static String getNamespace(String id) {
        if (id == null || id.isEmpty() || !id.contains(":")) return "minecraft";
        return id.split(":")[0];
    }
}
