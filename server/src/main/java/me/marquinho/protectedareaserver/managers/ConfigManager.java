package me.marquinho.protectedareaserver.managers;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.util.SimpleYaml;

import java.io.*;
import java.nio.file.Files;

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
            plugin.getLogger().error("No se pudo crear el directorio de configuración: " + configFolder.getAbsolutePath(), e);
        }

        configFile = new File(configFolder, "Config.yml");

        if (!configFile.exists()) {
            try {
                config = new SimpleYaml();
                config.set("mod-required", false);
                config.set("kick-message", "<red>¡Necesitas tener el mod de cliente instalado para jugar en este servidor!");
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().error("Error al crear Config.yml", e);
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
        return config.getString("kick-message", "<red>¡Necesitas tener el mod de cliente instalado para jugar en este servidor!");
    }

    public void setKickMessage(String message) {
        config.set("kick-message", message);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().error("Error al guardar Config.yml", e);
        }
    }
}
