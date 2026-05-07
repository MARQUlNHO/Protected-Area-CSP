package me.marquinho.protectedAreaPlugin.api.events;

import me.marquinho.protectedAreaPlugin.models.AdvancedRuleType;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Fired when an advanced rule (by block/entity) blocks a player action.
public class AreaAdvancedRuleBlockedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ProtectedArea area;
    private final AdvancedRuleType ruleType;
    private final String targetId;

    public AreaAdvancedRuleBlockedEvent(Player player, ProtectedArea area, AdvancedRuleType ruleType, String targetId) {
        this.player = player;
        this.area = area;
        this.ruleType = ruleType;
        this.targetId = targetId;
    }

    public Player getPlayer() { return player; }
    public ProtectedArea getArea() { return area; }
    public AdvancedRuleType getRuleType() { return ruleType; }
    // The block, entity, or item ID that triggered the rule.
    public String getTargetId() { return targetId; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
