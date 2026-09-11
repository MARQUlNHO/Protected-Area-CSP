package me.marquinho.protectedarea.commands.subcommands.cube;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public class PriorityCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("priority")
                .then(CommandManager.argument("id", StringArgumentType.string())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("value", IntegerArgumentType.integer(Integer.MIN_VALUE + 1))
                                .executes(context -> executePriority(context, plugin))
                        )
                );
    }

    private static int executePriority(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "id");
        int priority = IntegerArgumentType.getInteger(context, "value");

        if (areaId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(areaId);
            if (areaList.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().setAreaPriority(a.getId(), priority)) count++;
            final int c = count;
            Messages.send(source, "protectedarea.command.priority.applied.folder",
                    Messages.arg("priority", priority), Messages.arg("count", c), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        if (plugin.getAreaManager().setAreaPriority(areaId, priority)) {
            Messages.send(source, "protectedarea.command.priority.updated.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.priority.line", Messages.arg("priority", priority));
            Messages.send(source, "protectedarea.command.priority.note");
            return 1;
        }
        Messages.error(source, "protectedarea.command.priority.update_failed");
        return 0;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAreaIds(ProtectedAreaInit plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isCube())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(java.util.Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }
}
