package me.marquinho.protectedAreaPlugin.listeners;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinCacheListener implements Listener {
    private final ProtectedAreaPlugin plugin;

    public PlayerJoinCacheListener(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getAreaCommandManager().loadCache(player);
    }
}