package me.marquinho.protectedAreaPlugin.listeners;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final ProtectedAreaPlugin plugin;

    public PlayerQuitListener(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getAreaCommandManager().saveCache(player);

        ProtectedArea area = plugin.getAreaManager().getAreaAt(player.getLocation());

        if (area != null && area.hasPlayerLimit()) {
            if (!area.hasException(player.getName(), "limit")) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getAreaManager().broadcastAreaLimitUpdate(area.getId());
                }, 20L);
            }
        }
    }
}