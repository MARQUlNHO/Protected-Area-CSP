package me.marquinho.protectedarea.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public class DimensionPriorityCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("priority")
                .then(CommandManager.argument("area_id", StringArgumentType.string())
                        .suggests(DimensionAreas.suggestIds(plugin))
                        .then(CommandManager.argument("value", IntegerArgumentType.integer(Integer.MIN_VALUE + 1))
                                .executes(context -> executePriority(context, plugin))
                        )
                );
    }

    private static int executePriority(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        int priority = IntegerArgumentType.getInteger(context, "value");

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().setAreaPriority(a.getId(), priority)) count++;
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            final int c = count;
            Messages.send(source, "protectedarea.command.priority.applied.folder",
                    Messages.arg("priority", priority), Messages.arg("count", c), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        if (plugin.getAreaManager().setAreaPriority(areaId, priority)) {
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            Messages.send(source, "protectedarea.command.priority.updated.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.priority.line", Messages.arg("priority", priority));
            Messages.send(source, "protectedarea.command.priority.note");
            return 1;
        }
        Messages.error(source, "protectedarea.command.priority.update_failed");
        return 0;
    }
}
