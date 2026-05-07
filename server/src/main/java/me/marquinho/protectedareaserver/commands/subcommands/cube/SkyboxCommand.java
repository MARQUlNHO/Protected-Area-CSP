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

public class SkyboxCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("skybox")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("skybox_name", StringArgumentType.word())
                                        .executes(context -> executeAdd(context, plugin)))))
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
        String skyboxName = StringArgumentType.getString(context, "skybox_name");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        if (plugin.getAreaManager().setSkyboxForArea(areaId, skyboxName)) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Skybox asignada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Skybox: <gold>" + skyboxName), false);
            source.sendFeedback(() -> TextUtil.parse("<gray>Archivo esperado en el cliente: config/ProtectedArea/assets/skybox/" + skyboxName + ".png"), false);
            return 1;
        }
        source.sendError(TextUtil.parse("<red>Error al asignar la skybox"));
        return 0;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        if (!area.hasSkybox()) { source.sendFeedback(() -> TextUtil.parse("<yellow>El área no tiene una skybox asignada"), false); return 0; }
        String previousSkybox = area.getSkybox();
        if (plugin.getAreaManager().setSkyboxForArea(areaId, "")) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Skybox removida del área!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<gray>Skybox anterior: <yellow>" + previousSkybox), false);
            return 1;
        }
        source.sendError(TextUtil.parse("<red>Error al remover la skybox"));
        return 0;
    }

    private static int executeInfo(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        source.sendFeedback(() -> TextUtil.parse("<gold><bold>Skybox del Área: <yellow>" + areaId), false);
        source.sendFeedback(() -> TextUtil.parse(""), false);
        if (area.hasSkybox()) {
            source.sendFeedback(() -> TextUtil.parse("  <green>Skybox: <gold>" + area.getSkybox()), false);
            source.sendFeedback(() -> TextUtil.parse("  <gray>Archivo: config/ProtectedArea/assets/skybox/" + area.getSkybox() + ".png"), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("  <gray>Sin skybox asignada"), false);
            source.sendFeedback(() -> TextUtil.parse("  <gray>Se usa el cielo de la dimensión"), false);
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
