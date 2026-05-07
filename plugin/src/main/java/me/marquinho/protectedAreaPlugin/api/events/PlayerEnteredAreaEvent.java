package me.marquinho.protectedAreaPlugin.api.events;

import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Fired when the server receives confirmation that a player entered a cube area.
public class PlayerEnteredAreaEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ProtectedArea area;

    public PlayerEnteredAreaEvent(Player player, ProtectedArea area) {
        this.player = player;
        this.area = area;
    }

    public Player getPlayer() { return player; }
    public ProtectedArea getArea() { return area; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
