package me.marquinho.protectedarea.listeners;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.SchedulerUtil;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerQuitListener {

    public static void onQuit(ProtectedAreaInit plugin, ServerPlayerEntity player) {
        plugin.getAreaCommandManager().saveCache(player);

        String dim = player.getServerWorld().getRegistryKey().getValue().toString();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, player.getX(), player.getY(), player.getZ());

        if (area != null && area.hasPlayerLimit()) {
            if (!area.hasException(player.getGameProfile().getName(), "limit")) {
                SchedulerUtil.runLater(() ->
                    plugin.getAreaManager().broadcastAreaLimitUpdate(area.getId()), 20);
            }
        }
    }
}
