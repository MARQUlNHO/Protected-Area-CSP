package me.marquinho.protectedAreaPlugin.commands.subcommands.flat;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;

public class FlatLimitCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("limit")
                .then(Commands.literal("pass")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    plugin.getAreaManager().getAreas().entrySet().stream()
                                            .filter(e -> e.getValue().isFlat())
                                            .map(java.util.Map.Entry::getKey)
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(Commands.literal("negative")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> executeSetPass(ctx, plugin, false))
                                        )
                                )
                                .then(Commands.literal("positive")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> executeSetPass(ctx, plugin, true))
                                        )
                                )
                        )
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("area_id", StringArgumentType.word())
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

    private static int executeSetPass(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin, boolean isPositive) {
        CommandSender sender = ctx.getSource().getSender();
        String areaId = StringArgumentType.getString(ctx, "area_id");
        boolean value = BoolArgumentType.getBool(ctx, "value");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null || !area.isFlat()) {
            sender.sendMessage("§cNo existe un área flat con el ID: " + areaId);
            return 0;
        }

        if (isPositive) area.setPassPositive(value);
        else area.setPassNegative(value);

        plugin.getAreaManager().saveAreaManually(area);

        String side = isPositive ? "positivo (+)" : "negativo (-)";
        String status = value ? "§aABIERTO" : "§cBLOQUEADO";
        sender.sendMessage("§eÁrea: §6" + areaId);
        sender.sendMessage("§eLado " + side + ": " + status);
        return 1;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin) {
        CommandSender sender = ctx.getSource().getSender();
        String areaId = StringArgumentType.getString(ctx, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null || !area.isFlat()) {
            sender.sendMessage("§cNo existe un área flat con el ID: " + areaId);
            return 0;
        }

        String negStatus = area.isPassNegative() ? "§atrue (abierto)" : "§cfalse (bloqueado)";
        String posStatus = area.isPassPositive() ? "§atrue (abierto)" : "§cfalse (bloqueado)";

        sender.sendMessage("§eÁrea flat: §6" + areaId);
        sender.sendMessage("§epass.negative: " + negStatus);
        sender.sendMessage("§epass.positive: " + posStatus);
        return 1;
    }
}
