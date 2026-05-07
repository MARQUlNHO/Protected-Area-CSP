package me.marquinho.protectedareaserver.mixin;

import me.marquinho.protectedareaserver.listeners.AreaProtectionListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Explosion.class)
public class ExplosionMixin {

    @Shadow @Final private World world;

    @Inject(method = "affectWorld(Z)V", at = @At("HEAD"))
    private void onAffectWorld(boolean particles, CallbackInfo ci) {
        if (AreaProtectionListener.INSTANCE == null) return;

        Explosion self = (Explosion) (Object) this;
        List<BlockPos> blocks = self.getAffectedBlocks();
        if (blocks == null || blocks.isEmpty()) return;

        String dim = world.getRegistryKey().getValue().toString();
        blocks.removeIf(pos ->
            !AreaProtectionListener.INSTANCE.onExplosionBlockDestruction(dim, pos.getX(), pos.getY(), pos.getZ())
        );
    }
}
