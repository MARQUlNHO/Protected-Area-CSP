package me.marquinho.protectedareaserver.commands.subcommands.cube;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class LimitCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("limit")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("limit", IntegerArgumentType.integer(1))
                                        .executes(context -> executeAdd(context, plugin)))))
                .then(CommandManager.literal("block")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("blocked", BoolArgumentType.bool())
                                        .executes(context -> executeBlock(context, plugin)))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .executes(context -> executeRemove(context, plugin))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .executes(context -> executeInfo(context, plugin))));
    }

    private static int executeAdd(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        int limit = IntegerArgumentType.getInteger(context, "limit");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        if (plugin.getAreaManager().setAreaLimit(areaId, limit)) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Límite de jugadores actualizado!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Límite: <gold>" + limit + " jugador(es)"), false);
            int current = plugin.getAreaManager().getPlayersInArea(areaId);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Jugadores actuales: <gold>" + current + "<yellow>/<gold>" + limit), false);
            return 1;
        }
        source.sendError(TextUtil.parse("<red>Error al establecer el límite"));
        return 0;
    }

    private static int executeBlock(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        boolean blocked = BoolArgumentType.getBool(context, "blocked");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        if (!area.hasPlayerLimit()) {
            source.sendError(TextUtil.parse("<red>El área no tiene un límite de jugadores configurado"));
            source.sendFeedback(() -> TextUtil.parse("<yellow>Usa <gold>/area cube limit add " + areaId + " <número> <yellow>primero"), false);
            return 0;
        }
        if (plugin.getAreaManager().setAreaLimitBlocked(areaId, blocked)) {
            if (blocked) {
                source.sendFeedback(() -> TextUtil.parse("<red>¡Área bloqueada!"), false);
                source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
                int trapped = plugin.getAreaManager().getPlayersInArea(areaId);
                source.sendFeedback(() -> TextUtil.parse("<yellow>Jugadores dentro: <gold>" + trapped), false);
            } else {
                source.sendFeedback(() -> TextUtil.parse("<green>¡Área desbloqueada!"), false);
                source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            }
            return 1;
        }
        source.sendError(TextUtil.parse("<red>Error al cambiar el estado de bloqueo"));
        return 0;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        if (!area.hasPlayerLimit()) { source.sendFeedback(() -> TextUtil.parse("<yellow>El área no tiene un límite configurado"), false); return 0; }
        if (plugin.getAreaManager().removeAreaLimit(areaId)) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Límite de jugadores removido!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            return 1;
        }
        source.sendError(TextUtil.parse("<red>Error al remover el límite"));
        return 0;
    }

    private static int executeInfo(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        source.sendFeedback(() -> TextUtil.parse("<gold><bold>Información de Límite - <yellow>" + areaId), false);
        source.sendFeedback(() -> TextUtil.parse(""), false);
        if (area.hasPlayerLimit()) {
            int limit = area.getPlayerLimit();
            int current = plugin.getAreaManager().getPlayersInArea(areaId);
            source.sendFeedback(() -> TextUtil.parse("  <green>Límite: <gold>" + limit + " jugador(es)"), false);
            source.sendFeedback(() -> TextUtil.parse("  <green>Actuales: <gold>" + current + "<yellow>/<gold>" + limit), false);
            int available = limit - current;
            if (available > 0) source.sendFeedback(() -> TextUtil.parse("  <green>Espacios disponibles: <dark_green>" + available), false);
            else source.sendFeedback(() -> TextUtil.parse("  <red>Área llena"), false);
            source.sendFeedback(() -> TextUtil.parse(""), false);
            if (area.isLimitBlocked()) {
                source.sendFeedback(() -> TextUtil.parse("  <red>Estado: <dark_red><bold>BLOQUEADA"), false);
                source.sendFeedback(() -> TextUtil.parse("  <gray>Nadie puede entrar o salir"), false);
            } else {
                source.sendFeedback(() -> TextUtil.parse("  <green>Estado: <dark_green><bold>ACTIVA"), false);
                source.sendFeedback(() -> TextUtil.parse("  <gray>Se respeta el límite de jugadores"), false);
            }
        } else {
            source.sendFeedback(() -> TextUtil.parse("  <gray>No hay límite configurado"), false);
        }
        source.sendFeedback(() -> TextUtil.parse(""), false);
        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        return 1;
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
