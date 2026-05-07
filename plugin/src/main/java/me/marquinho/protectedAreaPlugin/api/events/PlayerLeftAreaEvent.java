package me.marquinho.protectedAreaPlugin.api.events;

import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Fired when the server receives confirmation that a player left a cube area.
public class PlayerLeftAreaEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ProtectedArea area;

    public PlayerLeftAreaEvent(Player player, ProtectedArea area) {
        this.player = player;
        this.area = area;
    }

    public Player getPlayer() { return player; }
    public ProtectedArea getArea() { return area; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
