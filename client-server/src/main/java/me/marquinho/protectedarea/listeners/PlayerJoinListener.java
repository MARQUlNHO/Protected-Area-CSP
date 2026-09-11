package me.marquinho.protectedarea.listeners;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.util.SchedulerUtil;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerJoinListener {

    public static void onJoin(ProtectedAreaInit plugin, ServerPlayerEntity player) {
        SchedulerUtil.runLater(() -> plugin.getAreaManager().sendAllAreasToPlayer(player), 20);
    }
}
