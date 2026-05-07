package me.marquinho.protectedAreaPlugin;

import me.marquinho.protectedAreaPlugin.commands.AreaCommand;
import me.marquinho.protectedAreaPlugin.listeners.*;
import me.marquinho.protectedAreaPlugin.managers.*;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProtectedAreaPlugin extends JavaPlugin {

    private static ProtectedAreaPlugin instance;
    private AreaManager areaManager;
    private AdvancedRulesManager advancedRulesManager;
    private NotificationManager notificationManager;
    private ConfigManager configManager;
    private DebugManager debugManager;
    private AreaCommandManager areaCommandManager;
    private ModVerificationListener modVerificationListener;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        areaCommandManager = new AreaCommandManager(this);

        configManager = new ConfigManager(this);

        areaManager = new AreaManager(this);
        areaManager.loadAllAreas();

        advancedRulesManager = new AdvancedRulesManager(this);
        advancedRulesManager.loadAllRules();

        notificationManager = new NotificationManager(this);

        debugManager = new DebugManager(this);

        modVerificationListener = new ModVerificationListener(this);

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            AreaCommand.register(this, event.registrar());
        });

        getServer().getPluginManager().registerEvents(new PlayerJoinCacheListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new AreaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(modVerificationListener, this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);

        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                modVerificationListener.cleanupPlayer(event.getPlayer().getUniqueId());
                debugManager.removeSession(event.getPlayer().getUniqueId());
            }
        }, this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "protectedarea:main");
        getServer().getMessenger().registerIncomingPluginChannel(this, "protectedarea:main", new ClientMessageListener(this));

        getLogger().info("ProtectedAreaPlugin habilitado correctamente!");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);

        getLogger().info("ProtectedAreaPlugin deshabilitado correctamente!");
    }

    public static ProtectedAreaPlugin getInstance() {
        return instance;
    }

    public AreaCommandManager getAreaCommandManager() { return areaCommandManager; }

    public AreaManager getAreaManager() {
        return areaManager;
    }

    public AdvancedRulesManager getAdvancedRulesManager() {
        return advancedRulesManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }

    public ModVerificationListener getModVerificationListener() {
        return modVerificationListener;
    }
}