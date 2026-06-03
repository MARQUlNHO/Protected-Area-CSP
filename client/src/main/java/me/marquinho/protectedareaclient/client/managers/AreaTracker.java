package me.marquinho.protectedareaclient.client.managers;

import me.marquinho.protectedareaclient.client.api.ProtectedAreaEvents;
import me.marquinho.protectedareaclient.client.models.ProtectedArea;
import me.marquinho.protectedareaclient.client.network.ClientNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AreaTracker {
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
        // chat("[ProtectedArea] Área añadida: " + area.getId()
        //         + " | Tipo: " + area.getType()
        //         + " | Mundo: " + area.getWorldName());
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

        String worldName = getWorldName(world.getRegistryKey());
        String dimension = getDimensionKey(world.getRegistryKey());
        String playerName = player.getName().getString();

        // Flat areas: check every tick using sweep detection (prev → curr position)
        if (!Double.isNaN(prevX)) {
            for (ProtectedArea area : areas.values()) {
                if (area.isFlat()) {
                    checkFlatAreaCrossing(area, player, prevX, prevY, prevZ, x, y, z, worldName, dimension);
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
                checkCubeAreaPosition(area, player, x, y, z, worldName, dimension, playerName, newAreas);
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

        SkyboxManager.update(currentAreas, areas);
    }

    private void checkCubeAreaPosition(ProtectedArea area, ClientPlayerEntity player,
                                        double x, double y, double z,
                                        String worldName, String dimension, String playerName,
                                        Set<String> newAreas) {
        if (area.isInside(x, y, z, worldName, dimension)) {
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
                                        String worldName, String dimension) {
        if (!area.getWorldName().equals(worldName) || !area.getDimension().equals(dimension)) return;

        int axis = area.getFlatAxis();
        double planeCoord = area.getFlatPlaneCoord();

        double prevCoord = switch (axis) { case 0 -> prevX; case 1 -> prevY; default -> prevZ; };
        double currCoord = switch (axis) { case 0 -> x;     case 1 -> y;     default -> z; };

        boolean crossingToPositive = prevCoord < planeCoord && currCoord >= planeCoord;
        boolean crossingToNegative = prevCoord >= planeCoord && currCoord < planeCoord;

        if (!crossingToPositive && !crossingToNegative) return;

        // Interpolate the exact crossing point to validate against the flat rect bounds
        double t = (planeCoord - prevCoord) / (currCoord - prevCoord);
        double crossX = prevX + t * (x - prevX);
        double crossY = prevY + t * (y - prevY);
        double crossZ = prevZ + t * (z - prevZ);

        if (!area.isWithinFlatRect(crossX, crossY, crossZ, worldName, dimension)) return;

        boolean toPositiveSide = crossingToPositive;
        ClientNetworkHandler.sendPlayerCrossedFlatArea(area.getId(), toPositiveSide);
        ProtectedAreaEvents.PLAYER_CROSSED_FLAT.invoker().onCross(area, toPositiveSide);
    }

    private String getWorldName(RegistryKey<World> worldKey) {
        String path = worldKey.getValue().getPath();

        if (worldKey == World.OVERWORLD || path.equals("overworld")) {
            return "world";
        } else if (worldKey == World.NETHER || path.equals("the_nether")) {
            return "world_nether";
        } else if (worldKey == World.END || path.equals("the_end")) {
            return "world_the_end";
        } else {
            return path;
        }
    }

    private String getDimensionKey(RegistryKey<World> dimension) {
        if (dimension == World.NETHER) {
            return "minecraft:the_nether";
        } else if (dimension == World.END) {
            return "minecraft:the_end";
        } else {
            return "minecraft:overworld";
        }
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
