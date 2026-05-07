package me.marquinho.protectedareaserver.managers;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.util.SimpleYaml;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class NotificationManager {

    private final ProtectedAreaInit plugin;
    private final File notificationsFolder;
    private final Map<String, SimpleYaml> configCache;

    public NotificationManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        this.notificationsFolder = new File(plugin.getDataPath(), "AreaNotification");
        this.configCache = new HashMap<>();

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
        config.set("no_break", "<red>¡No puedes romper bloques en esta área!</red>");
        config.set("no_place", "<red>¡No puedes colocar bloques en esta área!</red>");
        config.set("no_place_fluid", "<red>¡No puedes colocar fluidos en esta área!</red>");
        config.set("no_interact", "<red>¡No puedes interactuar en esta área!</red>");
        config.set("no_interact_entity", "<red>¡No puedes interactuar con entidades en esta área!</red>");
        config.set("no_interact_vehicle", "<red>¡No puedes montarte en vehículos en esta área!</red>");
        config.set("no_interact_inventory", "<red>¡No puedes abrir inventarios en esta área!</red>");
        config.set("no_pvp", "<red>¡No puedes atacar jugadores en esta área!</red>");
        config.set("no_entityattack", "<red>¡No puedes atacar entidades en esta área!</red>");
        config.set("no_drop", "<red>¡No puedes tirar items en esta área!</red>");
        config.set("no_collect", "<red>¡No puedes recoger items en esta área!</red>");
        config.set("no_entry_collision", "");
        config.set("no_exit_collision", "");
        config.set("no_exit_returned", "");

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().error("Error al crear Rules.yml", e);
        }
    }

    private void createAdvancedRulesConfig() {
        File file = new File(notificationsFolder, "AdvancedRules.yml");
        if (file.exists()) return;

        SimpleYaml config = new SimpleYaml();
        config.set("no_break_specific", "<red>¡No puedes romper <gold>{blockid}</gold> en esta área!</red>");
        config.set("no_place_specific", "<red>¡No puedes colocar <gold>{blockid}</gold> en esta área!</red>");
        config.set("no_place_fluid_specific", "<red>¡No puedes colocar <gold>{blockid}</gold> en esta área!</red>");
        config.set("no_interact_block", "<red>¡No puedes interactuar con <gold>{blockid}</gold> en esta área!</red>");
        config.set("no_interact_entity", "<red>¡No puedes interactuar con <gold>{entityid}</gold> en esta área!</red>");
        config.set("no_interact_vehicle", "<red>¡No puedes montarte en <gold>{entityid}</gold> en esta área!</red>");
        config.set("no_interact_inventory", "<red>¡No puedes abrir inventarios de <gold>{entityid}</gold> en esta área!</red>");
        config.set("no_drop_specific", "<red>¡No puedes tirar <gold>{itemid}</gold> en esta área!</red>");
        config.set("no_collect_specific", "<red>¡No puedes recoger <gold>{itemid}</gold> en esta área!</red>");

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().error("Error al crear AdvancedRules.yml", e);
        }
    }

    public void reloadConfigs() {
        configCache.clear();
    }

    private SimpleYaml getConfig(String fileName) {
        if (configCache.containsKey(fileName)) return configCache.get(fileName);

        File file = new File(notificationsFolder, fileName + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warn("Archivo de notificación no encontrado: " + fileName + ".yml");
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
        player.sendMessage(TextUtil.parse(message));
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
