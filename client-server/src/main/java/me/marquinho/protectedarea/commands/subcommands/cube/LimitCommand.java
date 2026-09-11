package me.marquinho.protectedarea.commands.subcommands.cube;

import com.mojang.brigadier.arguments.BoolArgumentType;
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

public class LimitCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("limit")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("limit", IntegerArgumentType.integer(1))
                                        .executes(context -> executeAdd(context, plugin)))))
                .then(CommandManager.literal("block")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("blocked", BoolArgumentType.bool())
                                        .executes(context -> executeBlock(context, plugin)))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .executes(context -> executeRemove(context, plugin))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .executes(context -> executeInfo(context, plugin))));
    }

    private static int executeAdd(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        int limit = IntegerArgumentType.getInteger(context, "limit");

        if (areaId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(areaId);
            if (areaList.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().setAreaLimit(a.getId(), limit)) count++;
            final int c = count;
            Messages.send(source, "protectedarea.command.limit.applied.folder",
                    Messages.arg("limit", limit), Messages.arg("count", c), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        if (plugin.getAreaManager().setAreaLimit(areaId, limit)) {
            Messages.send(source, "protectedarea.command.limit.updated.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.limit.line", Messages.arg("limit", limit));
            int current = plugin.getAreaManager().getPlayersInArea(areaId);
            Messages.send(source, "protectedarea.command.limit.current_players", Messages.arg("current", current), Messages.arg("limit", limit));
            return 1;
        }
        Messages.error(source, "protectedarea.command.limit.set_failed");
        return 0;
    }

    private static int executeBlock(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        boolean blocked = BoolArgumentType.getBool(context, "blocked");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        if (!area.hasPlayerLimit()) {
            Messages.error(source, "protectedarea.command.limit.not_configured_error");
            Messages.send(source, "protectedarea.command.limit.use_add_first", Messages.arg("area", areaId));
            return 0;
        }
        if (plugin.getAreaManager().setAreaLimitBlocked(areaId, blocked)) {
            if (blocked) {
                Messages.send(source, "protectedarea.command.limit.blocked");
                Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
                int trapped = plugin.getAreaManager().getPlayersInArea(areaId);
                Messages.send(source, "protectedarea.command.limit.players_inside", Messages.arg("count", trapped));
            } else {
                Messages.send(source, "protectedarea.command.limit.unblocked");
                Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            }
            return 1;
        }
        Messages.error(source, "protectedarea.command.limit.toggle_failed");
        return 0;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");

        if (areaId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(areaId);
            if (areaList.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            int count = 0;
            for (ProtectedArea a : areaList) if (a.hasPlayerLimit() && plugin.getAreaManager().removeAreaLimit(a.getId())) count++;
            final int c = count;
            Messages.send(source, "protectedarea.command.limit.removed.folder", Messages.arg("count", c), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        if (!area.hasPlayerLimit()) { Messages.send(source, "protectedarea.command.limit.not_configured"); return 0; }
        if (plugin.getAreaManager().removeAreaLimit(areaId)) {
            Messages.send(source, "protectedarea.command.limit.removed.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            return 1;
        }
        Messages.error(source, "protectedarea.command.limit.remove_failed");
        return 0;
    }

    private static int executeInfo(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        Messages.send(source, "protectedarea.command.exception.list.divider");
        Messages.send(source, "protectedarea.command.limit.info.header", Messages.arg("area", areaId));
        Messages.send(source, "protectedarea.command.common.blank_line");
        if (area.hasPlayerLimit()) {
            int limit = area.getPlayerLimit();
            int current = plugin.getAreaManager().getPlayersInArea(areaId);
            Messages.send(source, "protectedarea.command.limit.info.limit_line", Messages.arg("limit", limit));
            Messages.send(source, "protectedarea.command.limit.info.current_line", Messages.arg("current", current), Messages.arg("limit", limit));
            int available = limit - current;
            if (available > 0) Messages.send(source, "protectedarea.command.limit.info.available", Messages.arg("available", available));
            else Messages.send(source, "protectedarea.command.limit.info.full");
            Messages.send(source, "protectedarea.command.common.blank_line");
            if (area.isLimitBlocked()) {
                Messages.send(source, "protectedarea.command.limit.info.status_blocked");
                Messages.send(source, "protectedarea.command.limit.info.blocked_desc");
            } else {
                Messages.send(source, "protectedarea.command.limit.info.status_active");
                Messages.send(source, "protectedarea.command.limit.info.active_desc");
            }
        } else {
            Messages.send(source, "protectedarea.command.limit.info.none");
        }
        Messages.send(source, "protectedarea.command.common.blank_line");
        Messages.send(source, "protectedarea.command.exception.list.divider");
        return 1;
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
