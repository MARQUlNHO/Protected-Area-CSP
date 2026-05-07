package me.marquinho.protectedAreaPlugin.listeners;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModVerificationListener implements Listener {
    private final ProtectedAreaPlugin plugin;
    private final Map<UUID, Boolean> verifiedPlayers = new HashMap<>();
    private final Map<UUID, Integer> verificationTasks = new HashMap<>();

    public ModVerificationListener(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getConfigManager().isModRequired()) {
            return;
        }

        verifiedPlayers.put(player.getUniqueId(), false);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sendModCheckPacket(player);

            int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                checkAndKickPlayer(player);
            }, 100L).getTaskId();

            verificationTasks.put(player.getUniqueId(), taskId);

        }, 10L);
    }

    private void sendModCheckPacket(Player player) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF("MOD_CHECK");
            player.sendPluginMessage(plugin, "protectedarea:main", stream.toByteArray());

            plugin.getLogger().info("Enviando verificación de mod a: " + player.getName());
        } catch (IOException e) {
//            plugin.getLogger().severe("Error al enviar paquete de verificación a: " + player.getName());
            e.printStackTrace();
        }
    }

    private void checkAndKickPlayer(Player player) {
        if (!player.isOnline()) {
            cleanup(player.getUniqueId());
            return;
        }

        Boolean isVerified = verifiedPlayers.get(player.getUniqueId());

        if (isVerified == null || !isVerified) {
            String kickMessage = plugin.getConfigManager().getKickMessage();
            player.kickPlayer(kickMessage);

            plugin.getLogger().warning("Jugador " + player.getName() + " expulsado por no tener el mod de cliente");
        }

        cleanup(player.getUniqueId());
    }

    public void markPlayerVerified(Player player) {
        verifiedPlayers.put(player.getUniqueId(), true);

        Integer taskId = verificationTasks.get(player.getUniqueId());
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            verificationTasks.remove(player.getUniqueId());
        }

//        plugin.getLogger().info("Jugador " + player.getName() + " verificado exitosamente con el mod de cliente");
    }

    private void cleanup(UUID uuid) {
        verifiedPlayers.remove(uuid);
        verificationTasks.remove(uuid);
    }

    public void cleanupPlayer(UUID uuid) {
        cleanup(uuid);
    }
}