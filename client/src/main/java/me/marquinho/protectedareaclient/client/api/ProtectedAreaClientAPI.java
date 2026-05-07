package me.marquinho.protectedareaclient.client.api;

import me.marquinho.protectedareaclient.client.ProtectedareaclientClient;
import me.marquinho.protectedareaclient.client.models.ProtectedArea;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

// Entry point for interacting with ProtectedAreaClient from other mods.
// Usage: ProtectedAreaClientAPI api = ProtectedAreaClientAPI.getInstance();
public final class ProtectedAreaClientAPI {

    private static final ProtectedAreaClientAPI INSTANCE = new ProtectedAreaClientAPI();

    private ProtectedAreaClientAPI() {}

    public static ProtectedAreaClientAPI getInstance() {
        return INSTANCE;
    }

    // ---------
    // Query API
    // ---------

    // All areas currently known to the client (any world/dimension).
    public Collection<ProtectedArea> getAreas() {
        return Collections.unmodifiableCollection(
                ProtectedareaclientClient.getAreaTracker().getAreas().values()
        );
    }

    // Area by ID, or empty if not found.
    public Optional<ProtectedArea> getArea(String id) {
        return Optional.ofNullable(
                ProtectedareaclientClient.getAreaTracker().getAreas().get(id)
        );
    }

    // IDs of the cube areas the local player is currently inside.
    // Returns a snapshot — safe to iterate without holding a lock.
    public Set<String> getPlayerCurrentAreas() {
        return ProtectedareaclientClient.getAreaTracker().getCurrentAreas();
    }

    // Whether the local player is currently inside the given cube area.
    public boolean isPlayerInside(String areaId) {
        return ProtectedareaclientClient.getAreaTracker().getCurrentAreas().contains(areaId);
    }

    // All cube areas that contain the given position.
    // worldName: e.g. "world", "world_nether", "world_the_end"
    // dimension: e.g. "minecraft:overworld"
    public Collection<ProtectedArea> getAreasAt(double x, double y, double z,
                                                String worldName, String dimension) {
        return ProtectedareaclientClient.getAreaTracker().getAreas().values().stream()
                .filter(area -> !area.isFlat() && area.isInside(x, y, z, worldName, dimension))
                .toList();
    }

}
