package me.marquinho.protectedarea.commands.subcommands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;

public class DebugCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("debug")
                .then(CommandManager.argument("target", EntityArgumentType.players())
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> executeDebug(context, plugin))
                        )
                );
    }

    private static int executeDebug(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        if (targets.isEmpty()) {
            Messages.error(source, "protectedarea.command.debug.no_players_found");
            return 0;
        }
        ServerPlayerEntity sourcePlayer = source.getEntity() instanceof ServerPlayerEntity p ? p : null;
        for (ServerPlayerEntity target : targets) {
            String targetName = target.getGameProfile().getName();
            if (enabled) {
                plugin.getDebugManager().enableDebug(target);
                Messages.send(source, "protectedarea.command.debug.enabled_for", Messages.arg("player", targetName));
                if (sourcePlayer == null || !target.getUuid().equals(sourcePlayer.getUuid()))
                    Messages.sendTo(target, "protectedarea.command.debug.enabled_by", Messages.arg("source", source.getName()));
            } else {
                plugin.getDebugManager().disableDebug(target);
                Messages.send(source, "protectedarea.command.debug.disabled_for", Messages.arg("player", targetName));
                if (sourcePlayer == null || !target.getUuid().equals(sourcePlayer.getUuid()))
                    Messages.sendTo(target, "protectedarea.command.debug.disabled_by", Messages.arg("source", source.getName()));
            }
        }
        return 1;
    }
}
