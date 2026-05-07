package me.marquinho.protectedareaserver.commands.subcommands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.util.TextUtil;
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
            source.sendError(TextUtil.parse("<red>No se encontraron jugadores con ese selector."));
            return 0;
        }
        ServerPlayerEntity sourcePlayer = source.getEntity() instanceof ServerPlayerEntity p ? p : null;
        for (ServerPlayerEntity target : targets) {
            String targetName = target.getGameProfile().getName();
            if (enabled) {
                plugin.getDebugManager().enableDebug(target);
                source.sendFeedback(() -> TextUtil.parse("<green>Debug activado para <gold>" + targetName), false);
                if (sourcePlayer == null || !target.getUuid().equals(sourcePlayer.getUuid()))
                    target.sendMessage(TextUtil.parse("<yellow>[Debug] <green>Modo debug activado por <gold>" + source.getName()));
            } else {
                plugin.getDebugManager().disableDebug(target);
                source.sendFeedback(() -> TextUtil.parse("<red>Debug desactivado para <gold>" + targetName), false);
                if (sourcePlayer == null || !target.getUuid().equals(sourcePlayer.getUuid()))
                    target.sendMessage(TextUtil.parse("<yellow>[Debug] <red>Modo debug desactivado por <gold>" + source.getName()));
            }
        }
        return 1;
    }
}
