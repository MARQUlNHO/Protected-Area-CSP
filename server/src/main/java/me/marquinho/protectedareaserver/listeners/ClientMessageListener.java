package me.marquinho.protectedareaserver.listeners;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.api.ProtectedAreaServerEvents;
import me.marquinho.protectedareaserver.models.AreaRule;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;

public class ClientMessageListener {

    public static void handleMessage(ProtectedAreaInit plugin, ServerPlayerEntity player, byte[] message) {
        if (plugin == null) return;

        ByteArrayInputStream stream = new ByteArrayInputStream(message);
        DataInputStream in = new DataInputStream(stream);

        try {
            String action = in.readUTF();

            switch (action) {
                case "PLAYER_ENTERED_AREA" -> {
                    String areaId = in.readUTF();
                    plugin.getAreaManager().broadcastAreaLimitUpdate(areaId);
                    plugin.getAreaCommandManager().triggerCommands(player, areaId, true);
                    ProtectedArea enteredArea = plugin.getAreaManager().getAreas().get(areaId);
                    if (enteredArea != null) ProtectedAreaServerEvents.PLAYER_ENTERED_AREA.invoker().onPlayerArea(player, enteredArea);
                }
                case "PLAYER_LEFT_AREA" -> {
                    String areaId = in.readUTF();
                    plugin.getAreaManager().broadcastAreaLimitUpdate(areaId);
                    plugin.getAreaCommandManager().triggerCommands(player, areaId, false);
                    ProtectedArea leftArea = plugin.getAreaManager().getAreas().get(areaId);
                    if (leftArea != null) ProtectedAreaServerEvents.PLAYER_LEFT_AREA.invoker().onPlayerArea(player, leftArea);
                }
                case "REQUEST_EXPEL" -> {
                    String areaId = in.readUTF();
                    ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
                    if (area != null) plugin.getAreaManager().expelPlayerFromArea(player, area);
                }
                case "REQUEST_RETURN" -> {
                    String areaId = in.readUTF();
                    ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
                    if (area != null && area.hasRule(AreaRule.NO_EXIT)) {
                        if (!area.hasException(player.getGameProfile().getName(), "no_exit")) {
                            plugin.getAreaManager().returnPlayerToArea(player, area);
                        }
                    }
                }
                case "REQUEST_COLLISION_NOTIFICATION" -> {
                    String areaId = in.readUTF();
                    boolean isNoEntry = in.readBoolean();
                    plugin.getAreaManager().notifyCollision(player, areaId, isNoEntry);
                }
                case "MOD_RESPONSE" -> {
                    plugin.getModVerificationListener().markPlayerVerified(player);
                }
                case "PLAYER_CROSSED_FLAT_AREA" -> {
                    String areaId = in.readUTF();
                    boolean positiveDirection = in.readBoolean();
                    ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
                    if (area != null) {
                        plugin.getLogger().info("[FlatArea] " + player.getGameProfile().getName()
                                + " cruzó: " + areaId
                                + " | Dirección: " + (positiveDirection ? "positiva (+)" : "negativa (-)"));
                        plugin.getAreaCommandManager().triggerCommands(player, areaId, positiveDirection);
                        ProtectedAreaServerEvents.PLAYER_CROSSED_FLAT.invoker().onCross(player, area, positiveDirection);
                    }
                }
                case "REQUEST_DEBUG_PAGE" -> {
                    int page = in.readInt();
                    plugin.getDebugManager().sendPage(player, page);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().error("Error al recibir mensaje del cliente", e);
        }
    }
}
