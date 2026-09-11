package me.marquinho.protectedarea.commands.subcommands.flat;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public class FlatLimitCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("limit")
                .then(CommandManager.literal("pass")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    plugin.getAreaManager().getAreas().entrySet().stream()
                                            .filter(e -> e.getValue().isFlat())
                                            .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                                            .map(java.util.Map.Entry::getKey)
                                            .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
                                    return builder.buildFuture();
                                })
                                .then(CommandManager.literal("negative")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> executeSetPass(ctx, plugin, false))
                                        )
                                )
                                .then(CommandManager.literal("positive")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> executeSetPass(ctx, plugin, true))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    plugin.getAreaManager().getAreas().entrySet().stream()
                                            .filter(e -> e.getValue().isFlat())
                                            .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                                            .map(java.util.Map.Entry::getKey)
                                            .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeInfo(ctx, plugin))
                        )
                );
    }

    private static int executeSetPass(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isPositive) {
        String areaId = StringArgumentType.getString(ctx, "area_id");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        String side = isPositive ? "positive (+)" : "negative (-)";
        String status = value ? "<green>OPEN" : "<red>BLOCKED";

        if (areaId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(areaId);
            List<ProtectedArea> flatList = areaList.stream().filter(ProtectedArea::isFlat).toList();
            if (flatList.isEmpty()) { Messages.error(ctx.getSource(), "protectedarea.command.flatlimit.no_flat_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            for (ProtectedArea a : flatList) {
                if (isPositive) a.setPassPositive(value);
                else a.setPassNegative(value);
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            Messages.send(ctx.getSource(), "protectedarea.command.flatlimit.side_status.folder",
                    Messages.arg("side", side), Messages.markup("status", status),
                    Messages.arg("count", flatList.size()), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null || !area.isFlat()) { Messages.error(ctx.getSource(), "protectedarea.command.flatlimit.area_not_found", Messages.arg("area", areaId)); return 0; }

        if (isPositive) area.setPassPositive(value);
        else area.setPassNegative(value);
        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        Messages.send(ctx.getSource(), "protectedarea.command.common.area_line", Messages.arg("area", areaId));
        Messages.send(ctx.getSource(), "protectedarea.command.flatlimit.side_status",
                Messages.arg("side", side), Messages.markup("status", status));
        return 1;
    }

    private static int executeInfo(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin) {
        String areaId = StringArgumentType.getString(ctx, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null || !area.isFlat()) {
            Messages.error(ctx.getSource(), "protectedarea.command.flatlimit.area_not_found", Messages.arg("area", areaId));
            return 0;
        }

        String negStatus = area.isPassNegative() ? "<green>true (open)" : "<red>false (blocked)";
        String posStatus = area.isPassPositive() ? "<green>true (open)" : "<red>false (blocked)";

        Messages.send(ctx.getSource(), "protectedarea.command.flatlimit.info.area_line", Messages.arg("area", areaId));
        Messages.send(ctx.getSource(), "protectedarea.command.flatlimit.info.negative",
                Messages.markup("status", negStatus));
        Messages.send(ctx.getSource(), "protectedarea.command.flatlimit.info.positive",
                Messages.markup("status", posStatus));
        return 1;
    }
}
