package me.marquinho.protectedAreaPlugin.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

// Fired when an area is removed.
public class AreaRemovedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String areaId;

    public AreaRemovedEvent(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaId() { return areaId; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
