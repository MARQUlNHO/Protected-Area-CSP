package me.marquinho.protectedareaserver.commands.subcommands.cube;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class PriorityCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("priority")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                .executes(context -> executePriority(context, plugin))
                        )
                );
    }

    private static int executePriority(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "id");
        int priority = IntegerArgumentType.getInteger(context, "value");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        if (plugin.getAreaManager().setAreaPriority(areaId, priority)) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Prioridad actualizada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Prioridad: <gold>" + priority), false);
            source.sendFeedback(() -> TextUtil.parse(""), false);
            source.sendFeedback(() -> TextUtil.parse("<gray>Nota: Mayor número = Mayor prioridad"), false);
            return 1;
        }
        source.sendError(TextUtil.parse("<red>Error al actualizar la prioridad"));
        return 0;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAreaIds(ProtectedAreaInit plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(java.util.Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
