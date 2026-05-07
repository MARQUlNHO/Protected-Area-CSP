package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DebugCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("debug")
                .then(Commands.argument("target", ArgumentTypes.players())
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> executeDebug(context, plugin))
                        )
                );
    }

    private static int executeDebug(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) throws CommandSyntaxException {
        CommandSender sender = context.getSource().getSender();

        PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(context.getSource());
        boolean enabled = BoolArgumentType.getBool(context, "enabled");

        if (targets.isEmpty()) {
            sender.sendMessage("§cNo se encontraron jugadores con ese selector.");
            return 0;
        }

        for (Player target : targets) {
            if (enabled) {
                plugin.getDebugManager().enableDebug(target);
                sender.sendMessage("§aDebug activado para §6" + target.getName());
                if (!target.equals(sender)) {
                    target.sendMessage("§e[Debug] §aModo debug activado por §6" + sender.getName());
                }
            } else {
                plugin.getDebugManager().disableDebug(target);
                sender.sendMessage("§cDebug desactivado para §6" + target.getName());
                if (!target.equals(sender)) {
                    target.sendMessage("§e[Debug] §cModo debug desactivado por §6" + sender.getName());
                }
            }
        }

        return 1;
    }
}
