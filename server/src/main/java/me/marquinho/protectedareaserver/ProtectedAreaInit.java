package me.marquinho.protectedareaserver;

import me.marquinho.protectedareaserver.commands.AreaCommand;
import java.nio.file.Files;
import me.marquinho.protectedareaserver.listeners.*;
import me.marquinho.protectedareaserver.managers.*;
import me.marquinho.protectedareaserver.network.ProtectedAreaPayload;
import me.marquinho.protectedareaserver.util.SchedulerUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ProtectedAreaInit {

    private static ProtectedAreaInit instance;

    private MinecraftServer server;
    private File dataPath;

    private AreaManager areaManager;
    private AdvancedRulesManager advancedRulesManager;
    private NotificationManager notificationManager;
    private ConfigManager configManager;
    private DebugManager debugManager;
    private AreaCommandManager areaCommandManager;
    private ModVerificationListener modVerificationListener;

    private final Logger logger = LoggerFactory.getLogger("ProtectedAreaServer");

    public static ProtectedAreaInit getInstance() { return instance; }

    public void onInitialize() {
        instance = this;
        SchedulerUtil.class.getName();

        PayloadTypeRegistry.playS2C().register(ProtectedAreaPayload.ID, ProtectedAreaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ProtectedAreaPayload.ID, ProtectedAreaPayload.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            AreaCommand.register(this, dispatcher, registryAccess)
        );

        ServerPlayNetworking.registerGlobalReceiver(ProtectedAreaPayload.ID,
                (payload, context) -> context.server().execute(() ->
                    ClientMessageListener.handleMessage(this, context.player(), payload.data())
                )
        );

        ServerLifecycleEvents.SERVER_STARTED.register(sv -> {
            this.server = sv;
            this.dataPath = FabricLoader.getInstance().getConfigDir().resolve("ProtectedArea").toFile();
            try {
                Files.createDirectories(this.dataPath.toPath());
            } catch (java.io.IOException e) {
                logger.error("No se pudo crear el directorio de datos: " + this.dataPath.getAbsolutePath(), e);
            }

            areaCommandManager    = new AreaCommandManager(this);
            configManager         = new ConfigManager(this);
            areaManager           = new AreaManager(this);
            areaManager.loadAllAreas();
            advancedRulesManager  = new AdvancedRulesManager(this);
            advancedRulesManager.loadAllRules();
            notificationManager   = new NotificationManager(this);
            debugManager          = new DebugManager(this);
            modVerificationListener = new ModVerificationListener(this);

            logger.info("ProtectedAreaServer habilitado correctamente!");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, packetSender, sv) -> {
            PlayerJoinListener.onJoin(this, handler.player);
            PlayerJoinCacheListener.onJoin(this, handler.player);
            modVerificationListener.onPlayerJoin(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, sv) -> {
            PlayerQuitListener.onQuit(this, handler.player);
            if (modVerificationListener != null)
                modVerificationListener.cleanupPlayer(handler.player.getUuid());
            if (debugManager != null)
                debugManager.removeSession(handler.player.getUuid());
            if (areaCommandManager != null)
                areaCommandManager.removeCache(handler.player.getUuid());
        });

        AreaProtectionListener.register(this);

        ServerLifecycleEvents.SERVER_STOPPING.register(sv -> {
            logger.info("ProtectedAreaServer deshabilitado correctamente!");
        });
    }


    public MinecraftServer getServer()                          { return server; }
    public File getDataPath()                                   { return dataPath; }
    public Logger getLogger()                                   { return logger; }
    public AreaManager getAreaManager()                         { return areaManager; }
    public AdvancedRulesManager getAdvancedRulesManager()       { return advancedRulesManager; }
    public NotificationManager getNotificationManager()         { return notificationManager; }
    public ConfigManager getConfigManager()                     { return configManager; }
    public DebugManager getDebugManager()                       { return debugManager; }
    public AreaCommandManager getAreaCommandManager()           { return areaCommandManager; }
    public ModVerificationListener getModVerificationListener() { return modVerificationListener; }
}
