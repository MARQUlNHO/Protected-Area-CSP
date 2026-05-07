package me.marquinho.protectedareaclient.client.api;

import me.marquinho.protectedareaclient.client.models.ProtectedArea;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class ProtectedAreaEvents {

    // Fired when the server sends a new area (ADD_AREA).
    public static final Event<AreaCallback> AREA_ADDED = EventFactory.createArrayBacked(
            AreaCallback.class, callbacks -> area -> {
                for (AreaCallback cb : callbacks) cb.onArea(area);
            }
    );

    // Fired when the server removes an area (REMOVE_AREA).
    public static final Event<AreaRemovedCallback> AREA_REMOVED = EventFactory.createArrayBacked(
            AreaRemovedCallback.class, callbacks -> areaId -> {
                for (AreaRemovedCallback cb : callbacks) cb.onAreaRemoved(areaId);
            }
    );

    // Fired when the server clears all areas (CLEAR_AREAS) or on disconnect.
    public static final Event<AreasCleared> AREAS_CLEARED = EventFactory.createArrayBacked(
            AreasCleared.class, callbacks -> () -> {
                for (AreasCleared cb : callbacks) cb.onCleared();
            }
    );

    // Fired when the local player enters a cube area.
    public static final Event<AreaCallback> PLAYER_ENTERED_AREA = EventFactory.createArrayBacked(
            AreaCallback.class, callbacks -> area -> {
                for (AreaCallback cb : callbacks) cb.onArea(area);
            }
    );

    // Fired when the local player leaves a cube area.
    public static final Event<AreaCallback> PLAYER_LEFT_AREA = EventFactory.createArrayBacked(
            AreaCallback.class, callbacks -> area -> {
                for (AreaCallback cb : callbacks) cb.onArea(area);
            }
    );

    // Fired when the local player crosses a flat area boundary.
    public static final Event<PlayerFlatCrossCallback> PLAYER_CROSSED_FLAT = EventFactory.createArrayBacked(
            PlayerFlatCrossCallback.class, callbacks -> (area, toPositiveSide) -> {
                for (PlayerFlatCrossCallback cb : callbacks) cb.onCross(area, toPositiveSide);
            }
    );

    @FunctionalInterface
    public interface AreaCallback {
        void onArea(ProtectedArea area);
    }

    @FunctionalInterface
    public interface AreaRemovedCallback {
        void onAreaRemoved(String areaId);
    }

    @FunctionalInterface
    public interface AreasCleared {
        void onCleared();
    }

    @FunctionalInterface
    public interface PlayerFlatCrossCallback {
        void onCross(ProtectedArea area, boolean toPositiveSide);
    }

    private ProtectedAreaEvents() {}
}
