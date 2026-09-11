package me.marquinho.protectedarea.managers;

import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WandManager {

    public record PendingCreation(String type, BlockPos pos1, BlockPos pos2, String dimension) {}

    private final Map<UUID, BlockPos> pos1ByPlayer = new HashMap<>();
    private final Map<UUID, String> pos1DimensionByPlayer = new HashMap<>();
    private final Map<UUID, BlockPos> pos2ByPlayer = new HashMap<>();
    private final Map<UUID, String> pos2DimensionByPlayer = new HashMap<>();
    private final Map<UUID, PendingCreation> pendingByPlayer = new HashMap<>();

    public void setPos1(UUID uuid, BlockPos pos, String dimension) {
        pos1ByPlayer.put(uuid, pos);
        pos1DimensionByPlayer.put(uuid, dimension);
    }

    public void setPos2(UUID uuid, BlockPos pos, String dimension) {
        pos2ByPlayer.put(uuid, pos);
        pos2DimensionByPlayer.put(uuid, dimension);
    }

    public BlockPos getPos1(UUID uuid) { return pos1ByPlayer.get(uuid); }
    public BlockPos getPos2(UUID uuid) { return pos2ByPlayer.get(uuid); }
    public String getPos1Dimension(UUID uuid) { return pos1DimensionByPlayer.get(uuid); }

    public boolean hasBothPositions(UUID uuid) {
        return pos1ByPlayer.containsKey(uuid) && pos2ByPlayer.containsKey(uuid);
    }

    public boolean positionsInSameDimension(UUID uuid) {
        String d1 = pos1DimensionByPlayer.get(uuid);
        String d2 = pos2DimensionByPlayer.get(uuid);
        return d1 != null && d1.equals(d2);
    }

    public void clearSelection(UUID uuid) {
        pos1ByPlayer.remove(uuid);
        pos1DimensionByPlayer.remove(uuid);
        pos2ByPlayer.remove(uuid);
        pos2DimensionByPlayer.remove(uuid);
    }

    public void beginPendingCreation(UUID uuid, String type, BlockPos pos1, BlockPos pos2, String dimension) {
        pendingByPlayer.put(uuid, new PendingCreation(type, pos1, pos2, dimension));
    }

    public PendingCreation getPendingCreation(UUID uuid) {
        return pendingByPlayer.get(uuid);
    }

    public void clearPendingCreation(UUID uuid) {
        pendingByPlayer.remove(uuid);
    }

    public void removePlayer(UUID uuid) {
        clearSelection(uuid);
        clearPendingCreation(uuid);
    }
}
