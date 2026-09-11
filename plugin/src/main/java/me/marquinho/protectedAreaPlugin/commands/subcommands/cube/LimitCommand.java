package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.BoolArgumentType;
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

import java.util.List;

public class LimitCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("limit")
                .then(Commands.literal("add")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                        .executes(context -> executeAdd(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("block")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .then(Commands.argument("blocked", BoolArgumentType.bool())
                                        .executes(context -> executeBlock(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .executes(context -> executeRemove(context, plugin))
                        )
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .executes(context -> executeInfo(context, plugin))
                        )
                );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        int limit = IntegerArgumentType.getInteger(context, "limit");

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            int count = 0;
            for (ProtectedArea a : list) if (plugin.getAreaManager().setAreaLimit(a.getId(), limit)) count++;
            sender.sendMessage("§aLimit §6" + limit + " §aapplied to §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo area exists with ID: " + areaId); return 0; }

        if (plugin.getAreaManager().setAreaLimit(areaId, limit)) {
            sender.sendMessage("§aPlayer limit updated!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eLimit: §6" + limit + " player(s)");
            int currentPlayers = plugin.getAreaManager().getPlayersInArea(areaId);
            sender.sendMessage("§eCurrent players: §6" + currentPlayers + "§e/§6" + limit);
            return 1;
        } else {
            sender.sendMessage("§cError setting the limit");
            return 0;
        }
    }

    private static int executeBlock(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        boolean blocked = BoolArgumentType.getBool(context, "blocked");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo area exists with ID: " + areaId); return 0; }

        if (!area.hasPlayerLimit()) {
            sender.sendMessage("§cThe area has no player limit configured");
            sender.sendMessage("§eUse §6/area cube limit add " + areaId + " [number] §efirst");
            return 0;
        }

        if (plugin.getAreaManager().setAreaLimitBlocked(areaId, blocked)) {
            if (blocked) {
                sender.sendMessage("§cArea blocked!");
                sender.sendMessage("§eArea: §6" + areaId);
                int trapped = plugin.getAreaManager().getPlayersInArea(areaId);
                sender.sendMessage("§ePlayers inside: §6" + trapped);
            } else {
                sender.sendMessage("§aArea unblocked!");
                sender.sendMessage("§eArea: §6" + areaId);
            }
            return 1;
        } else {
            sender.sendMessage("§cError changing the block state");
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            int count = 0;
            for (ProtectedArea a : list) if (a.hasPlayerLimit() && plugin.getAreaManager().removeAreaLimit(a.getId())) count++;
            sender.sendMessage("§aLimit removed from §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo area exists with ID: " + areaId); return 0; }
        if (!area.hasPlayerLimit()) { sender.sendMessage("§eThe area has no limit configured"); return 0; }

        if (plugin.getAreaManager().removeAreaLimit(areaId)) {
            sender.sendMessage("§aPlayer limit removed!");
            sender.sendMessage("§eArea: §6" + areaId);
            return 1;
        } else {
            sender.sendMessage("§cError removing the limit");
            return 0;
        }
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo area exists with ID: " + areaId); return 0; }

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lLimit Info - §e" + areaId);
        sender.sendMessage("");

        if (area.hasPlayerLimit()) {
            int limit = area.getPlayerLimit();
            int current = plugin.getAreaManager().getPlayersInArea(areaId);
            sender.sendMessage("  §aLimit: §6" + limit + " player(s)");
            sender.sendMessage("  §aCurrent: §6" + current + "§e/§6" + limit);
            int available = limit - current;
            if (available > 0) sender.sendMessage("  §aAvailable spots: §2" + available);
            else sender.sendMessage("  §cArea full");
            sender.sendMessage("");
            if (area.isLimitBlocked()) {
                sender.sendMessage("  §cStatus: §4§lBLOCKED");
                sender.sendMessage("  §7No one can enter or leave");
            } else {
                sender.sendMessage("  §aStatus: §2§lACTIVE");
                sender.sendMessage("  §7The player limit is enforced");
            }
        } else {
            sender.sendMessage("  §7No limit configured");
        }

        sender.sendMessage("");
        sender.sendMessage("§e§m                                          ");
        return 1;
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
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
