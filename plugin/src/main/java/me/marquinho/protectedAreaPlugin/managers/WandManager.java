package me.marquinho.protectedAreaPlugin.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandManager {

    public record Point(int x, int y, int z, String dimension) {}

    public record PendingCreation(String type, Point pos1, Point pos2) {}

    private final Map<UUID, Point> pos1ByPlayer = new HashMap<>();
    private final Map<UUID, Point> pos2ByPlayer = new HashMap<>();
    private final Map<UUID, PendingCreation> pendingByPlayer = new HashMap<>();

    public void setPos1(UUID uuid, Point point) { pos1ByPlayer.put(uuid, point); }
    public void setPos2(UUID uuid, Point point) { pos2ByPlayer.put(uuid, point); }
    public Point getPos1(UUID uuid) { return pos1ByPlayer.get(uuid); }
    public Point getPos2(UUID uuid) { return pos2ByPlayer.get(uuid); }

    public boolean hasBothPositions(UUID uuid) {
        return pos1ByPlayer.containsKey(uuid) && pos2ByPlayer.containsKey(uuid);
    }

    public boolean positionsInSameDimension(UUID uuid) {
        Point p1 = pos1ByPlayer.get(uuid);
        Point p2 = pos2ByPlayer.get(uuid);
        return p1 != null && p2 != null && p1.dimension().equals(p2.dimension());
    }

    public void clearSelection(UUID uuid) {
        pos1ByPlayer.remove(uuid);
        pos2ByPlayer.remove(uuid);
    }

    public void beginPendingCreation(UUID uuid, String type, Point pos1, Point pos2) {
        pendingByPlayer.put(uuid, new PendingCreation(type, pos1, pos2));
    }

    public PendingCreation getPendingCreation(UUID uuid) { return pendingByPlayer.get(uuid); }

    public void clearPendingCreation(UUID uuid) { pendingByPlayer.remove(uuid); }

    public void removePlayer(UUID uuid) {
        clearSelection(uuid);
        clearPendingCreation(uuid);
    }
}
