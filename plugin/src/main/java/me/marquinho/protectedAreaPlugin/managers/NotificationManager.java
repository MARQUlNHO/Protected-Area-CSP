package me.marquinho.protectedAreaPlugin.managers;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NotificationManager {
    private final ProtectedAreaPlugin plugin;
    private final File notificationsFolder;
    private final MiniMessage miniMessage;
    private final Map<String, YamlConfiguration> configCache;

    public NotificationManager(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
        this.notificationsFolder = new File(plugin.getDataFolder(), "AreaNotification");
        this.miniMessage = MiniMessage.miniMessage();
        this.configCache = new HashMap<>();

        if (!notificationsFolder.exists()) {
            notificationsFolder.mkdirs();
        }

        createDefaultConfigs();
    }

    private void createDefaultConfigs() {
        createRulesConfig();
        createAdvancedRulesConfig();
    }

    private void createRulesConfig() {
        File file = new File(notificationsFolder, "Rules.yml");
        if (file.exists()) return;

        YamlConfiguration config = new YamlConfiguration();

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
            plugin.getLogger().info("Creado archivo de configuración: Rules.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Error al crear Rules.yml");
            e.printStackTrace();
        }
    }

    private void createAdvancedRulesConfig() {
        File file = new File(notificationsFolder, "AdvancedRules.yml");
        if (file.exists()) return;

        YamlConfiguration config = new YamlConfiguration();

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
            plugin.getLogger().info("Creado archivo de configuración: AdvancedRules.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Error al crear AdvancedRules.yml");
            e.printStackTrace();
        }
    }

    public void reloadConfigs() {
        configCache.clear();
        plugin.getLogger().info("Configuraciones de notificaciones recargadas");
    }

    private YamlConfiguration getConfig(String fileName) {
        if (configCache.containsKey(fileName)) {
            return configCache.get(fileName);
        }

        File file = new File(notificationsFolder, fileName + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Archivo de notificación no encontrado: " + fileName + ".yml");
            return new YamlConfiguration();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        configCache.put(fileName, config);
        return config;
    }

    public void sendNotification(Player player, String configFile, String key, Map<String, String> placeholders) {
        YamlConfiguration config = getConfig(configFile);
        String message = config.getString(key);

        if (message == null || message.isEmpty()) {
            return;
        }

        message = replacePlaceholders(message, placeholders);

        Component component = miniMessage.deserialize(message);
        player.sendMessage(component);
    }

    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }

        String result = message;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }

        return result;
    }

    @Deprecated
    public void sendRulesMessage(Player player, String key) {
        sendNotification(player, "Rules", key, null);
    }

    public void sendAdvancedRulesMessage(Player player, String key, Map<String, String> placeholders) {
        sendNotification(player, "AdvancedRules", key, placeholders);
    }

    public void sendCustomMessage(Player player, String configFileName, String key, Map<String, String> placeholders) {
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
        if (blockId == null || blockId.isEmpty()) {
            return "";
        }
        return blockId.contains(":") ? blockId.split(":")[1] : blockId;
    }

    public static String getEntityName(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return "";
        }
        return entityId.contains(":") ? entityId.split(":")[1] : entityId;
    }

    public static String getNamespace(String id) {
        if (id == null || id.isEmpty() || !id.contains(":")) {
            return "minecraft";
        }
        return id.split(":")[0];
    }
}