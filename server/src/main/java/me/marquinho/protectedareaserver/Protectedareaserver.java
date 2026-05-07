package me.marquinho.protectedareaserver;

import net.fabricmc.api.ModInitializer;

public class Protectedareaserver implements ModInitializer {

    @Override
    public void onInitialize() {
        new ProtectedAreaInit().onInitialize();
    }
}
