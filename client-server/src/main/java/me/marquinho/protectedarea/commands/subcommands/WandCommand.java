package me.marquinho.protectedarea.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.items.WandItems;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class WandCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("wand")
                .then(CommandManager.literal("cube").executes(context -> giveWand(context, "cube")))
                .then(CommandManager.literal("flat").executes(context -> giveWand(context, "flat")));
    }

    private static int giveWand(CommandContext<ServerCommandSource> context, String type) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity p ? p : null;

        if (player == null) {
            Messages.error(source, "protectedarea.command.wand.player_only");
            return 0;
        }

        ItemStack wand = WandItems.create(type);
        if (!player.giveItemStack(wand)) {
            Messages.error(source, "protectedarea.command.wand.no_space");
            return 0;
        }

        String label = "flat".equals(type) ? "flat" : "cube";
        Messages.send(source, "protectedarea.command.wand.given", Messages.arg("label", label));
        Messages.send(source, "protectedarea.command.wand.usage");
        return 1;
    }
}
