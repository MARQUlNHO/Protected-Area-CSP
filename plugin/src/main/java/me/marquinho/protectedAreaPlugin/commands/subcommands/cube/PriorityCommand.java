package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PriorityCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("priority")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                .executes(context -> executePriority(context, plugin))
                        )
                );
    }

    private static int executePriority(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        Player player = (sender instanceof Player) ? (Player) sender : null;

        String areaId = StringArgumentType.getString(context, "id");
        int priority = IntegerArgumentType.getInteger(context, "value");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        if (plugin.getAreaManager().setAreaPriority(areaId, priority)) {
            sender.sendMessage("§a¡Prioridad actualizada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§ePrioridad: §6" + priority);
            sender.sendMessage("");
            sender.sendMessage("§7Nota: Mayor número = Mayor prioridad");
            return 1;
        } else {
            sender.sendMessage("§cError al actualizar la prioridad");
            return 0;
        }
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(java.util.Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
