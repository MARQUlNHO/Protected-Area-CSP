package me.marquinho.protectedarea.client.managers;

import me.marquinho.protectedarea.client.api.ProtectedAreaEvents;
import me.marquinho.protectedarea.client.models.ProtectedArea;
import me.marquinho.protectedarea.client.network.ClientNetworkHandler;
import me.marquinho.protectedarea.client.util.WorldDimensionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AreaTracker {
    private static final Logger log = LoggerFactory.getLogger(AreaTracker.class);
    private final Map<String, ProtectedArea> areas;
    private final Set<String> currentAreas;

    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 1;
    private boolean viewEnabled = false;

    private double prevX = Double.NaN;
    private double prevY = Double.NaN;
    private double prevZ = Double.NaN;

    public AreaTracker() {
        this.areas = new HashMap<>();
        this.currentAreas = new HashSet<>();
    }

    private static void chat(String msg) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) p.sendMessage(Text.literal(msg), false);
    }

    public void addArea(ProtectedArea area) {
        areas.put(area.getId(), area);
        ProtectedAreaEvents.AREA_ADDED.invoker().onArea(area);
    }

    public void removeArea(String areaId) {
        areas.remove(areaId);
        currentAreas.remove(areaId);
        // chat("[ProtectedArea] Área removida: " + areaId);
        ProtectedAreaEvents.AREA_REMOVED.invoker().onAreaRemoved(areaId);
    }

    public void clearAreas() {
        areas.clear();
        currentAreas.clear();
        SkyboxManager.clear();
        // chat("[ProtectedArea] Todas las áreas limpiadas.");
        ProtectedAreaEvents.AREAS_CLEARED.invoker().onCleared();
    }

    public void checkPlayerPosition(ClientPlayerEntity player, ClientWorld world) {
        tickCounter++;

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        String dimension = WorldDimensionUtil.getDimensionKey(world.getRegistryKey());
        String playerName = player.getName().getString();

        if (!Double.isNaN(prevX)) {
            for (ProtectedArea area : areas.values()) {
                if (area.isFlat()) {
                    checkFlatAreaCrossing(area, player, prevX, prevY, prevZ, x, y, z, dimension);
                }
            }
        }

        prevX = x;
        prevY = y;
        prevZ = z;

        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        Set<String> newAreas = new HashSet<>();

        for (ProtectedArea area : areas.values()) {
            if (!area.isFlat()) {
                checkCubeAreaPosition(area, player, x, y, z, dimension, playerName, newAreas);
            }
        }

        for (String areaId : currentAreas) {
            if (!newAreas.contains(areaId)) {
                ProtectedArea area = areas.get(areaId);
                if (area == null || area.isFlat()) continue;

                if (area.hasNoExit() && !area.hasException(playerName, false)) {
                    // player.sendMessage(Text.literal("[CubeArea] NoExit activo, regresando a: " + areaId), false);
                    ClientNetworkHandler.requestReturnToArea(areaId);
                } else if (!area.hasLimitException(playerName)) {
                    // player.sendMessage(Text.literal("[CubeArea] Saliste de: " + areaId), false);
                    ClientNetworkHandler.sendPlayerLeftArea(areaId);
                }
                ProtectedAreaEvents.PLAYER_LEFT_AREA.invoker().onArea(area);
            }
        }

        currentAreas.clear();
        currentAreas.addAll(newAreas);

        SkyboxManager.update(currentAreas, areas, dimension);
    }

    private void checkCubeAreaPosition(ProtectedArea area, ClientPlayerEntity player,
                                        double x, double y, double z,
                                        String dimension, String playerName,
                                        Set<String> newAreas) {
        if (area.isInside(x, y, z, dimension)) {
            newAreas.add(area.getId());
            if (!currentAreas.contains(area.getId()) && !area.hasLimitException(playerName)) {
                // player.sendMessage(Text.literal("[CubeArea] Entraste en: " + area.getId()), false);
                ClientNetworkHandler.sendPlayerEnteredArea(area.getId());
                ProtectedAreaEvents.PLAYER_ENTERED_AREA.invoker().onArea(area);
            }
        }
    }

    private void checkFlatAreaCrossing(ProtectedArea area, ClientPlayerEntity player,
                                        double prevX, double prevY, double prevZ,
                                        double x, double y, double z,
                                        String dimension) {
        if (!area.getDimension().equals(dimension)) return;

        int axis = area.getFlatAxis();
        double planeCoord = area.getFlatPlaneCoord();

        double prevCoord = switch (axis) { case 0 -> prevX; case 1 -> prevY; default -> prevZ; };
        double currCoord = switch (axis) { case 0 -> x;     case 1 -> y;     default -> z; };

        boolean crossingToPositive = prevCoord < planeCoord && currCoord >= planeCoord;
        boolean crossingToNegative = prevCoord >= planeCoord && currCoord < planeCoord;

        if (!crossingToPositive && !crossingToNegative) return;

        double t = (planeCoord - prevCoord) / (currCoord - prevCoord);
        double crossX = prevX + t * (x - prevX);
        double crossY = prevY + t * (y - prevY);
        double crossZ = prevZ + t * (z - prevZ);

        if (!area.isWithinFlatRect(crossX, crossY, crossZ, dimension)) return;

        boolean toPositiveSide = crossingToPositive;
        ClientNetworkHandler.sendPlayerCrossedFlatArea(area.getId(), toPositiveSide);
        ProtectedAreaEvents.PLAYER_CROSSED_FLAT.invoker().onCross(area, toPositiveSide);
    }

    public Map<String, ProtectedArea> getAreas() {
        return areas;
    }

    public Set<String> getCurrentAreas() {
        return Collections.unmodifiableSet(currentAreas);
    }

    public boolean isViewEnabled() {
        return viewEnabled;
    }

    public void setViewEnabled(boolean enabled) {
        this.viewEnabled = enabled;
    }
}
