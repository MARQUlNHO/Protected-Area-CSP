package me.marquinho.protectedareaserver.commands.subcommands.cube;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class ExecuteCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("execute")
                .then(CommandManager.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                .executes(context -> executeCommand(context, plugin))
                        )
                );
    }

    private static int executeCommand(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        String command = StringArgumentType.getString(context, "command");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        List<ServerPlayerEntity> playersInArea = getPlayersInArea(plugin, area);
        if (playersInArea.isEmpty()) {
            source.sendFeedback(() -> TextUtil.parse("<yellow>No hay jugadores dentro del área <gold>" + areaId), false);
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
                source.sendError(TextUtil.parse("<red>Error al ejecutar comando para <gold>" + playerName + "<red>: " + e.getMessage()));
            }
        }
        final int count = successCount;
        final int total = playersInArea.size();
        source.sendFeedback(() -> TextUtil.parse("<green>Comando ejecutado en <gold>" + count + "<green>/<gold>" + total + " <green>jugador(es) del área <gold>" + areaId), false);
        return 1;
    }

    private static List<ServerPlayerEntity> getPlayersInArea(ProtectedAreaInit plugin, ProtectedArea area) {
        List<ServerPlayerEntity> playersInArea = new ArrayList<>();
        for (ServerPlayerEntity player : plugin.getServer().getPlayerManager().getPlayerList()) {
            String dim = player.getServerWorld().getRegistryKey().getValue().toString();
            ProtectedArea playerArea = plugin.getAreaManager().getAreaAt(dim, player.getX(), player.getY(), player.getZ());
            if (playerArea != null && playerArea.getId().equals(area.getId())) playersInArea.add(player);
        }
        return playersInArea;
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
