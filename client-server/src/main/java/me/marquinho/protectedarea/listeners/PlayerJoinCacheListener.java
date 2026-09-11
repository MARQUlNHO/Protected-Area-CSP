package me.marquinho.protectedarea.listeners;

import me.marquinho.protectedarea.ProtectedAreaInit;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerJoinCacheListener {

    public static void onJoin(ProtectedAreaInit plugin, ServerPlayerEntity player) {
        plugin.getAreaCommandManager().loadCache(player);
    }
}
