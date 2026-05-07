package me.marquinho.protectedareaserver.listeners;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.network.ProtectedAreaPayload;
import me.marquinho.protectedareaserver.util.SchedulerUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModVerificationListener {

    private final ProtectedAreaInit plugin;
    private final Map<UUID, Boolean> verifiedPlayers = new HashMap<>();
    private final Map<UUID, SchedulerUtil.Cancellable> verificationTasks = new HashMap<>();

    public ModVerificationListener(ProtectedAreaInit plugin) {
        this.plugin = plugin;
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        if (!plugin.getConfigManager().isModRequired()) return;

        verifiedPlayers.put(player.getUuid(), false);

        SchedulerUtil.runLater(() -> {
            sendModCheckPacket(player);
            SchedulerUtil.runLater(() -> checkAndKickPlayer(player), 100);
        }, 10);
    }

    private void sendModCheckPacket(ServerPlayerEntity player) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("MOD_CHECK");
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(stream.toByteArray()));
            plugin.getLogger().info("Enviando verificación de mod a: " + player.getGameProfile().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkAndKickPlayer(ServerPlayerEntity player) {
        if (plugin.getServer().getPlayerManager().getPlayer(player.getUuid()) == null) {
            cleanup(player.getUuid());
            return;
        }

        Boolean isVerified = verifiedPlayers.get(player.getUuid());
        if (isVerified == null || !isVerified) {
            String kickMsg = plugin.getConfigManager().getKickMessage();
            player.networkHandler.disconnect(TextUtil.parse(kickMsg));
            plugin.getLogger().warn("Jugador " + player.getGameProfile().getName() + " expulsado por no tener el mod de cliente");
        }
        cleanup(player.getUuid());
    }

    public void markPlayerVerified(ServerPlayerEntity player) {
        verifiedPlayers.put(player.getUuid(), true);
        SchedulerUtil.Cancellable task = verificationTasks.remove(player.getUuid());
        if (task != null) task.cancel();
    }

    private void cleanup(UUID uuid) {
        verifiedPlayers.remove(uuid);
        SchedulerUtil.Cancellable task = verificationTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void cleanupPlayer(UUID uuid) {
        cleanup(uuid);
    }
}
