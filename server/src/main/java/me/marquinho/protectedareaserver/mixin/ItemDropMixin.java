package me.marquinho.protectedareaserver.mixin;

import me.marquinho.protectedareaserver.listeners.AreaProtectionListener;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class ItemDropMixin {

    @Inject(
        method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDropItem(ItemStack stack, boolean throwRandomly, boolean retainOwnership,
                             CallbackInfoReturnable<ItemEntity> cir) {
        if (AreaProtectionListener.INSTANCE == null) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (stack == null || stack.isEmpty()) return;

        String itemId = Registries.ITEM.getId(stack.getItem()).toString();

        if (!AreaProtectionListener.INSTANCE.onItemDrop(serverPlayer, itemId)) {
            serverPlayer.getInventory().insertStack(stack);
            cir.setReturnValue(null);
        }
    }
}
