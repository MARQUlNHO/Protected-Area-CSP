package me.marquinho.protectedarea.client.api;

import me.marquinho.protectedarea.client.ProtectedareaClient;
import me.marquinho.protectedarea.client.models.ProtectedArea;

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
                ProtectedareaClient.getAreaTracker().getAreas().values()
        );
    }

    // Area by ID, or empty if not found.
    public Optional<ProtectedArea> getArea(String id) {
        return Optional.ofNullable(
                ProtectedareaClient.getAreaTracker().getAreas().get(id)
        );
    }

    // IDs of the cube areas the local player is currently inside.
    // Returns a snapshot — safe to iterate without holding a lock.
    public Set<String> getPlayerCurrentAreas() {
        return ProtectedareaClient.getAreaTracker().getCurrentAreas();
    }

    // Whether the local player is currently inside the given cube area.
    public boolean isPlayerInside(String areaId) {
        return ProtectedareaClient.getAreaTracker().getCurrentAreas().contains(areaId);
    }

    // All cube areas that contain the given position.
    // dimension: e.g. "minecraft:overworld"
    public Collection<ProtectedArea> getAreasAt(double x, double y, double z, String dimension) {
        return ProtectedareaClient.getAreaTracker().getAreas().values().stream()
                .filter(area -> !area.isFlat() && area.isInside(x, y, z, dimension))
                .toList();
    }

}
