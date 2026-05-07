package me.marquinho.protectedareaserver.listeners;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerJoinCacheListener {

    public static void onJoin(ProtectedAreaInit plugin, ServerPlayerEntity player) {
        plugin.getAreaCommandManager().loadCache(player);
    }
}
