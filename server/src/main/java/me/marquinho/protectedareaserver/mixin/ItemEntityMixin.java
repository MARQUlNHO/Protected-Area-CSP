package me.marquinho.protectedareaserver.mixin;

import me.marquinho.protectedareaserver.listeners.AreaProtectionListener;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void onPickup(PlayerEntity player, CallbackInfo ci) {
        if (AreaProtectionListener.INSTANCE == null) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ItemEntity self = (ItemEntity) (Object) this;
        String itemId = Registries.ITEM.getId(self.getStack().getItem()).toString();

        if (!AreaProtectionListener.INSTANCE.onItemPickup(serverPlayer, self, itemId)) {
            ci.cancel();
        }
    }
}
