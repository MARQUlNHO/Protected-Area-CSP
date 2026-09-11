package me.marquinho.protectedarea;

import net.fabricmc.api.ModInitializer;

public class Protectedarea implements ModInitializer {

    @Override
    public void onInitialize() {
        new ProtectedAreaInit().onInitialize();
    }
}
