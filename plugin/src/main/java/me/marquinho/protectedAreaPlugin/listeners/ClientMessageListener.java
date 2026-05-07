package me.marquinho.protectedAreaPlugin.listeners;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.api.events.PlayerCrossedFlatEvent;
import me.marquinho.protectedAreaPlugin.api.events.PlayerEnteredAreaEvent;
import me.marquinho.protectedAreaPlugin.api.events.PlayerLeftAreaEvent;
import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientMessageListener implements PluginMessageListener {
    private final ProtectedAreaPlugin plugin;

    public ClientMessageListener(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("protectedarea:main")) {
            return;
        }

        ByteArrayInputStream stream = new ByteArrayInputStream(message);
        DataInputStream in = new DataInputStream(stream);

        try {
            String action = in.readUTF();

            if (action.equals("PLAYER_ENTERED_AREA")) {
                String areaId = in.readUTF();
//                plugin.getLogger().info("Jugador " + player.getName() + " entró al área: " + areaId);
//                player.sendMessage("§e¡Has entrado al área: §6" + areaId + "§e!");

                plugin.getAreaManager().broadcastAreaLimitUpdate(areaId);
                plugin.getAreaCommandManager().triggerCommands(player, areaId, true);
                ProtectedArea enteredArea = plugin.getAreaManager().getAreas().get(areaId);
                if (enteredArea != null) plugin.getServer().getPluginManager().callEvent(new PlayerEnteredAreaEvent(player, enteredArea));

            } else if (action.equals("PLAYER_LEFT_AREA")) {
                String areaId = in.readUTF();
//                plugin.getLogger().info("Jugador " + player.getName() + " salió del área: " + areaId);
//                player.sendMessage("§e¡Has salido del área: §6" + areaId + "§e!");

                plugin.getAreaManager().broadcastAreaLimitUpdate(areaId);
                plugin.getAreaCommandManager().triggerCommands(player, areaId, false);
                ProtectedArea leftArea = plugin.getAreaManager().getAreas().get(areaId);
                if (leftArea != null) plugin.getServer().getPluginManager().callEvent(new PlayerLeftAreaEvent(player, leftArea));

            } else if (action.equals("REQUEST_EXPEL")) {
                String areaId = in.readUTF();
                ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);

                if (area != null) {
                    plugin.getAreaManager().expelPlayerFromArea(player, area);
                }

            } else if (action.equals("REQUEST_RETURN")) {
                String areaId = in.readUTF();
                ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);

                if (area != null && area.hasRule(AreaRule.NO_EXIT)) {
                    if (!area.hasException(player.getName(), "no_exit")) {
                        plugin.getAreaManager().returnPlayerToArea(player, area);
                    }
                }

            } else if (action.equals("REQUEST_COLLISION_NOTIFICATION")) {
                String areaId = in.readUTF();
                boolean isNoEntry = in.readBoolean();

                plugin.getAreaManager().notifyCollision(player, areaId, isNoEntry);

            } else if (action.equals("MOD_RESPONSE")) {
                plugin.getModVerificationListener().markPlayerVerified(player);

            } else if (action.equals("PLAYER_CROSSED_FLAT_AREA")) {
                String areaId = in.readUTF();
                boolean positiveDirection = in.readBoolean();
                ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
                if (area != null) {
                    plugin.getLogger().info("[FlatArea] " + player.getName() + " cruzó: " + areaId
                            + " | Dirección: " + (positiveDirection ? "positiva (+)" : "negativa (-)"));
                    plugin.getAreaCommandManager().triggerCommands(player, areaId, positiveDirection);
                    plugin.getServer().getPluginManager().callEvent(new PlayerCrossedFlatEvent(player, area, positiveDirection));
                }

            } else if (action.equals("REQUEST_DEBUG_PAGE")) {
                int page = in.readInt();
                plugin.getDebugManager().sendPage(player, page);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Error al recibir mensaje del cliente");
            e.printStackTrace();
        }
    }
}