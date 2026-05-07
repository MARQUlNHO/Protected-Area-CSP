package me.marquinho.protectedAreaPlugin.managers;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

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
                config.set("kick-message", "§c¡Necesitas tener el mod de cliente instalado para jugar en este servidor!");

                config.save(configFile);
                plugin.getLogger().info("Archivo Config.yml creado con valores por defecto");
            } catch (IOException e) {
                plugin.getLogger().severe("Error al crear Config.yml");
                e.printStackTrace();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Configuración recargada");
    }

    public boolean isModRequired() {
        return config.getBoolean("mod-required", false);
    }

    public void setModRequired(boolean required) {
        config.set("mod-required", required);
        saveConfig();
    }

    public String getKickMessage() {
        return config.getString("kick-message", "§c¡Necesitas tener el mod de cliente instalado para jugar en este servidor!");
    }

    public void setKickMessage(String message) {
        config.set("kick-message", message);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar Config.yml");
            e.printStackTrace();
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}