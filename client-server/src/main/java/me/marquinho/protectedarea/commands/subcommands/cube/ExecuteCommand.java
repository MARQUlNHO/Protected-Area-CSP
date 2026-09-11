package me.marquinho.protectedarea.commands.subcommands.cube;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.CommandSuggestions;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class ExecuteCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("execute")
                .then(CommandManager.argument("area_id", StringArgumentType.string())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                .suggests(CommandSuggestions.nestedCommand())
                                .executes(context -> executeCommand(context, plugin))
                        )
                );
    }

    private static int executeCommand(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        String command = StringArgumentType.getString(context, "command");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        List<ServerPlayerEntity> playersInArea = getPlayersInArea(plugin, area);
        if (playersInArea.isEmpty()) {
            Messages.send(source, "protectedarea.command.execute.no_players_in_area", Messages.arg("area", areaId));
            return 0;
        }
        int successCount = 0;
        for (ServerPlayerEntity player : playersInArea) {
            String playerName = player.getGameProfile().getName();
            String finalCommand = command.replace("{player}", playerName);
            try {
                plugin.getServer().getCommandManager().executeWithPrefix(plugin.getServer().getCommandSource(), finalCommand);
                successCount++;
            } catch (Exception e) {
                Messages.error(source, "protectedarea.command.execute.error",
                        Messages.arg("player", playerName), Messages.arg("message", String.valueOf(e.getMessage())));
            }
        }
        final int count = successCount;
        final int total = playersInArea.size();
        Messages.send(source, "protectedarea.command.execute.result",
                Messages.arg("count", count), Messages.arg("total", total), Messages.arg("area", areaId));
        return 1;
    }

    private static List<ServerPlayerEntity> getPlayersInArea(ProtectedAreaInit plugin, ProtectedArea area) {
        return plugin.getAreaManager().getPlayersInsideArea(area.getId());
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
