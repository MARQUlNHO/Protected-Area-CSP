package me.marquinho.protectedareaserver.mixin;

import me.marquinho.protectedareaserver.listeners.AreaProtectionListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockPlaceMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void onPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (AreaProtectionListener.INSTANCE == null) return;

        PlayerEntity player = context.getPlayer();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!(serverPlayer.getWorld() instanceof ServerWorld serverWorld)) return;

        BlockPos pos = context.getBlockPos();
        BlockItem blockItem = (BlockItem) (Object) this;
        String blockId = Registries.BLOCK.getId(blockItem.getBlock()).toString();

        if (!AreaProtectionListener.INSTANCE.onBlockPlace(serverPlayer, serverWorld, pos, blockId)) {
            serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(serverWorld, pos));
            serverPlayer.playerScreenHandler.syncState();
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
