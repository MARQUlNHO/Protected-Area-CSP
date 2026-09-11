package me.marquinho.protectedarea.managers;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.SimpleYaml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final ProtectedAreaInit plugin;
    private File configFile;
    private SimpleYaml config;

    public ConfigManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File configFolder = new File(plugin.getDataPath(), "Config");
        try {
            Files.createDirectories(configFolder.toPath());
        } catch (IOException e) {
            plugin.getLogger().error("Could not create the config directory: " + configFolder.getAbsolutePath(), e);
        }

        configFile = new File(configFolder, "Config.yml");

        if (!configFile.exists()) {
            try {
                config = new SimpleYaml();
                config.set("mod-required", false);
                config.set("kick-message", "<red>You need to have the client mod installed to play on this server!");
                config.set("areas-hidden", Collections.emptyList());
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().error("Error creating Config.yml", e);
            }
        } else {
            config = SimpleYaml.load(configFile);
        }
    }

    public void reloadConfig() {
        config = SimpleYaml.load(configFile);
    }

    public boolean isModRequired() {
        return config.getBoolean("mod-required", false);
    }

    public void setModRequired(boolean required) {
        config.set("mod-required", required);
        saveConfig();
    }

    public String getKickMessage() {
        return config.getString("kick-message", "<red>You need to have the client mod installed to play on this server!");
    }

    public void setKickMessage(String message) {
        config.set("kick-message", message);
        saveConfig();
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

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().error("Error saving Config.yml", e);
        }
    }
}
