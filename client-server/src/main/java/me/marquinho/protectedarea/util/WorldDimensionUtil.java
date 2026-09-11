package me.marquinho.protectedarea.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public final class WorldDimensionUtil {

    private WorldDimensionUtil() {}

    public static String getDimensionKey(RegistryKey<World> worldKey) {
        return worldKey.getValue().toString();
    }
}
