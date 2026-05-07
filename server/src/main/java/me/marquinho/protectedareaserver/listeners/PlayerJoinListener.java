package me.marquinho.protectedareaserver.listeners;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.util.SchedulerUtil;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerJoinListener {

    public static void onJoin(ProtectedAreaInit plugin, ServerPlayerEntity player) {
        SchedulerUtil.runLater(() -> plugin.getAreaManager().sendAllAreasToPlayer(player), 20);
    }
}
