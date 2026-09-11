package me.marquinho.protectedAreaPlugin.managers;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ConfigManager {
    private final ProtectedAreaPlugin plugin;
    private File configFile;
    private YamlConfiguration config;

    public ConfigManager(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File configFolder = new File(plugin.getDataFolder(), "Config");
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }

        configFile = new File(configFolder, "Config.yml");

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                config = YamlConfiguration.loadConfiguration(configFile);

                config.set("mod-required", false);
                config.set("kick-message", "§cYou need to have the client mod installed to play on this server!");
                config.set("areas-hidden", Collections.emptyList());

                config.save(configFile);
                plugin.getLogger().info("Config.yml file created with default values");
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating Config.yml");
                e.printStackTrace();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Configuration reloaded");
    }

    public boolean isModRequired() {
        return config.getBoolean("mod-required", false);
    }

    public void setModRequired(boolean required) {
        config.set("mod-required", required);
        saveConfig();
    }

    public String getKickMessage() {
        return config.getString("kick-message", "§cYou need to have the client mod installed to play on this server!");
    }

    public void setKickMessage(String message) {
        config.set("kick-message", message);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving Config.yml");
            e.printStackTrace();
        }
    }

    public List<String> getIgnoreCommandPrefixes() {
        return config.getStringList("areas-hidden");
    }

    public boolean isIgnoredInCommand(ProtectedArea area) {
        List<String> prefixes = getIgnoreCommandPrefixes();
        if (prefixes.isEmpty()) return false;
        String key = area.getStorageKey();
        return prefixes.stream().anyMatch(key::startsWith);
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}