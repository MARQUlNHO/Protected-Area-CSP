package me.marquinho.protectedAreaPlugin.api.events;

import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Fired when a player crosses a flat area boundary.
// toPositiveSide: true = crossed toward the positive side of the plane.
public class PlayerCrossedFlatEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ProtectedArea area;
    private final boolean toPositiveSide;

    public PlayerCrossedFlatEvent(Player player, ProtectedArea area, boolean toPositiveSide) {
        this.player = player;
        this.area = area;
        this.toPositiveSide = toPositiveSide;
    }

    public Player getPlayer() { return player; }
    public ProtectedArea getArea() { return area; }
    public boolean isToPositiveSide() { return toPositiveSide; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
