package me.marquinho.protectedAreaPlugin.api.events;

import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Fired when a basic rule blocks a player action (break, place, interact, pvp, etc.).
public class AreaRuleBlockedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ProtectedArea area;
    private final AreaRule rule;

    public AreaRuleBlockedEvent(Player player, ProtectedArea area, AreaRule rule) {
        this.player = player;
        this.area = area;
        this.rule = rule;
    }

    public Player getPlayer() { return player; }
    public ProtectedArea getArea() { return area; }
    public AreaRule getRule() { return rule; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
