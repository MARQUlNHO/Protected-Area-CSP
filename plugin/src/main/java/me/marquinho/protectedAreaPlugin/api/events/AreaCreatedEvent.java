package me.marquinho.protectedAreaPlugin.api.events;

import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Fired when a new area is created.
public class AreaCreatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ProtectedArea area;

    public AreaCreatedEvent(ProtectedArea area) {
        this.area = area;
    }

    public ProtectedArea getArea() { return area; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
