package me.marquinho.protectedareaserver.api;

import me.marquinho.protectedareaserver.models.AdvancedRuleType;
import me.marquinho.protectedareaserver.models.AreaRule;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ProtectedAreaServerEvents {

    // Fired when a new area is created via createArea().
    public static final Event<AreaCallback> AREA_CREATED = EventFactory.createArrayBacked(
            AreaCallback.class, callbacks -> area -> {
                for (AreaCallback cb : callbacks) cb.onArea(area);
            }
    );

    // Fired when an area is removed via removeArea().
    public static final Event<AreaRemovedCallback> AREA_REMOVED = EventFactory.createArrayBacked(
            AreaRemovedCallback.class, callbacks -> areaId -> {
                for (AreaRemovedCallback cb : callbacks) cb.onAreaRemoved(areaId);
            }
    );

    // Fired when the server receives confirmation that a player entered a cube area.
    public static final Event<PlayerAreaCallback> PLAYER_ENTERED_AREA = EventFactory.createArrayBacked(
            PlayerAreaCallback.class, callbacks -> (player, area) -> {
                for (PlayerAreaCallback cb : callbacks) cb.onPlayerArea(player, area);
            }
    );

    // Fired when the server receives confirmation that a player left a cube area.
    public static final Event<PlayerAreaCallback> PLAYER_LEFT_AREA = EventFactory.createArrayBacked(
            PlayerAreaCallback.class, callbacks -> (player, area) -> {
                for (PlayerAreaCallback cb : callbacks) cb.onPlayerArea(player, area);
            }
    );

    // Fired when a player crosses a flat area boundary.
    // toPositiveSide: true = crossed toward positive side of the plane.
    public static final Event<PlayerFlatCrossCallback> PLAYER_CROSSED_FLAT = EventFactory.createArrayBacked(
            PlayerFlatCrossCallback.class, callbacks -> (player, area, toPositiveSide) -> {
                for (PlayerFlatCrossCallback cb : callbacks) cb.onCross(player, area, toPositiveSide);
            }
    );

    // Fired when a basic rule blocks a player action (break, place, interact, pvp, etc.).
    public static final Event<RuleBlockedCallback> RULE_BLOCKED = EventFactory.createArrayBacked(
            RuleBlockedCallback.class, callbacks -> (player, area, rule) -> {
                for (RuleBlockedCallback cb : callbacks) cb.onRuleBlocked(player, area, rule);
            }
    );

    // Fired when an advanced rule (by block/entity) blocks a player action.
    public static final Event<AdvancedRuleBlockedCallback> ADVANCED_RULE_BLOCKED = EventFactory.createArrayBacked(
            AdvancedRuleBlockedCallback.class, callbacks -> (player, area, ruleType, targetId) -> {
                for (AdvancedRuleBlockedCallback cb : callbacks) cb.onAdvancedRuleBlocked(player, area, ruleType, targetId);
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
    public interface PlayerAreaCallback {
        void onPlayerArea(ServerPlayerEntity player, ProtectedArea area);
    }

    @FunctionalInterface
    public interface PlayerFlatCrossCallback {
        void onCross(ServerPlayerEntity player, ProtectedArea area, boolean toPositiveSide);
    }

    @FunctionalInterface
    public interface RuleBlockedCallback {
        void onRuleBlocked(ServerPlayerEntity player, ProtectedArea area, AreaRule rule);
    }

    @FunctionalInterface
    public interface AdvancedRuleBlockedCallback {
        void onAdvancedRuleBlocked(ServerPlayerEntity player, ProtectedArea area, AdvancedRuleType ruleType, String targetId);
    }

    private ProtectedAreaServerEvents() {}
}
