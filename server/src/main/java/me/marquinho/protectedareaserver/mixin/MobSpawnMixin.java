package me.marquinho.protectedareaserver.mixin;

import me.marquinho.protectedareaserver.listeners.AreaProtectionListener;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public class MobSpawnMixin {

    @Inject(
        method = "canSpawn(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/entity/SpawnReason;)Z",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onCanSpawn(WorldAccess world, SpawnReason spawnReason, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (AreaProtectionListener.INSTANCE == null) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        MobEntity self = (MobEntity) (Object) this;
        if (!AreaProtectionListener.INSTANCE.onMobSpawn(self.getType(), serverWorld, self.getBlockPos())) {
            cir.setReturnValue(false);
        }
    }
}
