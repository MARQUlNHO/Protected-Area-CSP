package me.marquinho.protectedareaserver.commands.subcommands.flat;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class FlatLimitCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("limit")
                .then(CommandManager.literal("pass")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    plugin.getAreaManager().getAreas().entrySet().stream()
                                            .filter(e -> e.getValue().isFlat())
                                            .map(java.util.Map.Entry::getKey)
                                            .forEach(builder::suggest);
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
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    plugin.getAreaManager().getAreas().entrySet().stream()
                                            .filter(e -> e.getValue().isFlat())
                                            .map(java.util.Map.Entry::getKey)
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> executeInfo(ctx, plugin))
                        )
                );
    }

    private static int executeSetPass(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isPositive) {
        String areaId = StringArgumentType.getString(ctx, "area_id");
        boolean value = BoolArgumentType.getBool(ctx, "value");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null || !area.isFlat()) {
            ctx.getSource().sendError(TextUtil.parse("<red>No existe un área flat con el ID: " + areaId));
            return 0;
        }

        if (isPositive) area.setPassPositive(value);
        else area.setPassNegative(value);

        plugin.getAreaManager().saveAreaManually(area);

        String side = isPositive ? "positivo (+)" : "negativo (-)";
        String status = value ? "<green>ABIERTO" : "<red>BLOQUEADO";
        ctx.getSource().sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
        ctx.getSource().sendFeedback(() -> TextUtil.parse("<yellow>Lado " + side + ": " + status), false);
        return 1;
    }

    private static int executeInfo(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin) {
        String areaId = StringArgumentType.getString(ctx, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null || !area.isFlat()) {
            ctx.getSource().sendError(TextUtil.parse("<red>No existe un área flat con el ID: " + areaId));
            return 0;
        }

        String negStatus = area.isPassNegative() ? "<green>true (abierto)" : "<red>false (bloqueado)";
        String posStatus = area.isPassPositive() ? "<green>true (abierto)" : "<red>false (bloqueado)";

        ctx.getSource().sendFeedback(() -> TextUtil.parse("<yellow>Área flat: <gold>" + areaId), false);
        ctx.getSource().sendFeedback(() -> TextUtil.parse("<yellow>pass.negative: " + negStatus), false);
        ctx.getSource().sendFeedback(() -> TextUtil.parse("<yellow>pass.positive: " + posStatus), false);
        return 1;
    }
}
