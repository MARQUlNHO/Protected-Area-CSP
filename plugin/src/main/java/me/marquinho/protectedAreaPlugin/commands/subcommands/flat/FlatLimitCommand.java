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

import java.util.List;

public class FlatLimitCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("limit")
                .then(Commands.literal("pass")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    plugin.getAreaManager().getAreas().entrySet().stream()
                                            .filter(e -> e.getValue().isFlat())
                                            .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                                            .map(java.util.Map.Entry::getKey)
                                            .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
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
                        .then(Commands.argument("area_id", StringArgumentType.string())
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

    private static int executeSetPass(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin, boolean isPositive) {
        CommandSender sender = ctx.getSource().getSender();
        String areaId = StringArgumentType.getString(ctx, "area_id");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        String side = isPositive ? "positive (+)" : "negative (-)";
        String status = value ? "§aOPEN" : "§cBLOCKED";

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            List<ProtectedArea> flatList = list.stream().filter(ProtectedArea::isFlat).toList();
            if (flatList.isEmpty()) { sender.sendMessage("§cNo flat areas in folder: " + areaId); return 0; }
            for (ProtectedArea a : flatList) {
                if (isPositive) a.setPassPositive(value);
                else a.setPassNegative(value);
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            sender.sendMessage("§aSide " + side + ": " + status + " §aapplied to §6" + flatList.size() + " §aflat area(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null || !area.isFlat()) {
            sender.sendMessage("§cNo flat area exists with ID: " + areaId);
            return 0;
        }

        if (isPositive) area.setPassPositive(value);
        else area.setPassNegative(value);

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        sender.sendMessage("§eArea: §6" + areaId);
        sender.sendMessage("§eSide " + side + ": " + status);
        return 1;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin) {
        CommandSender sender = ctx.getSource().getSender();
        String areaId = StringArgumentType.getString(ctx, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null || !area.isFlat()) {
            sender.sendMessage("§cNo flat area exists with ID: " + areaId);
            return 0;
        }

        String negStatus = area.isPassNegative() ? "§atrue (open)" : "§cfalse (blocked)";
        String posStatus = area.isPassPositive() ? "§atrue (open)" : "§cfalse (blocked)";

        sender.sendMessage("§eFlat area: §6" + areaId);
        sender.sendMessage("§epass.negative: " + negStatus);
        sender.sendMessage("§epass.positive: " + posStatus);
        return 1;
    }
}
