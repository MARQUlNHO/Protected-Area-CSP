package me.marquinho.protectedarea.listeners;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.items.WandItems;
import me.marquinho.protectedarea.managers.WandManager;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import me.marquinho.protectedarea.util.WorldDimensionUtil;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

public class WandListener {

    public static void register(ProtectedAreaInit plugin) {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

            if (!WandItems.isWand(sp.getMainHandStack())) return ActionResult.PASS;

            String dimension = WorldDimensionUtil.getDimensionKey(sw.getRegistryKey());
            plugin.getWandManager().setPos1(sp.getUuid(), pos, dimension);
            Messages.sendOverlay(sp, "protectedarea.wand.position1",
                    Messages.arg("x", pos.getX()), Messages.arg("y", pos.getY()), Messages.arg("z", pos.getZ()));
            return ActionResult.FAIL;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

            String wandType = WandItems.getWandType(sp.getMainHandStack());
            if (wandType == null) return ActionResult.PASS;

            if (sp.isSneaking()) {
                return tryFinalize(plugin, sp, wandType);
            }

            BlockPos pos = hitResult.getBlockPos();
            String dimension = WorldDimensionUtil.getDimensionKey(sw.getRegistryKey());
            plugin.getWandManager().setPos2(sp.getUuid(), pos, dimension);
            Messages.sendOverlay(sp, "protectedarea.wand.position2",
                    Messages.arg("x", pos.getX()), Messages.arg("y", pos.getY()), Messages.arg("z", pos.getZ()));
            return ActionResult.FAIL;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp) || !sp.isSneaking()) return ActionResult.PASS;

            String wandType = WandItems.getWandType(sp.getMainHandStack());
            if (wandType == null) return ActionResult.PASS;

            return tryFinalize(plugin, sp, wandType);
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (hand != Hand.MAIN_HAND) return TypedActionResult.pass(stack);
            if (!(player instanceof ServerPlayerEntity sp) || !sp.isSneaking()) return TypedActionResult.pass(stack);

            String wandType = WandItems.getWandType(stack);
            if (wandType == null) return TypedActionResult.pass(stack);

            ActionResult result = tryFinalize(plugin, sp, wandType);
            return result == ActionResult.FAIL
                    ? TypedActionResult.fail(stack)
                    : TypedActionResult.success(stack);
        });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            WandManager wandManager = plugin.getWandManager();
            WandManager.PendingCreation pending = wandManager.getPendingCreation(sender.getUuid());
            if (pending == null) return true;

            String raw = message.getContent().getString().trim();

            if (raw.equalsIgnoreCase("cancel")) {
                wandManager.clearPendingCreation(sender.getUuid());
                Messages.sendTo(sender, "protectedarea.wand.creation_cancelled");
                return false;
            }

            if (raw.isEmpty()) {
                Messages.sendTo(sender, "protectedarea.wand.name_empty");
                return false;
            }

            String id = raw.replace(' ', '_');

            if (plugin.getAreaManager().getAreas().containsKey(id)) {
                Messages.sendTo(sender, "protectedarea.wand.id_exists", Messages.arg("id", id));
                return false;
            }

            BlockPos p1 = pending.pos1();
            BlockPos p2 = pending.pos2();
            boolean flat = "flat".equals(pending.type());

            boolean created = plugin.getAreaManager().createArea(id, pending.dimension(),
                    p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ(),
                    flat ? "flat" : "cube", flat ? 8 : 0);

            if (created) {
                Messages.sendTo(sender, "protectedarea.wand.area_created", Messages.arg("id", id));
                ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
                plugin.getAreaManager().broadcastNewArea(area);
                wandManager.clearPendingCreation(sender.getUuid());
                wandManager.clearSelection(sender.getUuid());
            } else {
                Messages.sendTo(sender, "protectedarea.wand.area_creation_failed");
            }

            return false;
        });
    }

    private static ActionResult tryFinalize(ProtectedAreaInit plugin, ServerPlayerEntity sp, String wandType) {
        WandManager wandManager = plugin.getWandManager();

        if (!wandManager.hasBothPositions(sp.getUuid())) {
            Messages.sendOverlay(sp, "protectedarea.wand.need_both_positions");
            return ActionResult.FAIL;
        }
        if (!wandManager.positionsInSameDimension(sp.getUuid())) {
            Messages.sendOverlay(sp, "protectedarea.wand.positions_different_dimension");
            return ActionResult.FAIL;
        }

        wandManager.beginPendingCreation(sp.getUuid(), wandType,
                wandManager.getPos1(sp.getUuid()), wandManager.getPos2(sp.getUuid()),
                wandManager.getPos1Dimension(sp.getUuid()));

        Messages.sendTo(sp, "protectedarea.wand.prompt_name");
        return ActionResult.FAIL;
    }
}
