package me.marquinho.protectedareaserver.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.commands.subcommands.DebugCommand;
import me.marquinho.protectedareaserver.commands.subcommands.config.ModRequiredCommand;
import me.marquinho.protectedareaserver.commands.subcommands.cube.*;
import me.marquinho.protectedareaserver.commands.subcommands.flat.FlatLimitCommand;
import me.marquinho.protectedareaserver.models.AreaRule;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AreaCommand {

    public static void register(ProtectedAreaInit plugin, CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess) {
        dispatcher.register(buildCommand(plugin, registryAccess));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin,
                                                                            CommandRegistryAccess registryAccess) {
        return CommandManager.literal("area")
                .requires(source -> source.hasPermissionLevel(4))
                .then(createSubcommand(plugin))
                .then(removeSubcommand(plugin))
                .then(reloadSubcommand(plugin))
                .then(viewSubcommand(plugin))
                .then(colorSubcommand(plugin))
                .then(DebugCommand.buildCommand(plugin))
                .then(cubeSubcommand(plugin, registryAccess))
                .then(flatSubcommand(plugin))
                .then(configSubcommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> cubeSubcommand(ProtectedAreaInit plugin,
                                                                               CommandRegistryAccess registryAccess) {
        return CommandManager.literal("cube")
                .then(rulesSubcommand(plugin))
                .then(AdvancedRulesCommand.buildCommand(plugin, registryAccess))
                .then(exceptionSubcommand(plugin))
                .then(SkyboxCommand.buildCommand(plugin))
                .then(PriorityCommand.buildCommand(plugin))
                .then(TeleportCommand.buildCommand(plugin))
                .then(ExecuteCommand.buildCommand(plugin))
                .then(LimitCommand.buildCommand(plugin))
                .then(CommandCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> flatSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("flat")
                .then(FlatLimitCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> configSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("config")
                .then(ModRequiredCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> exceptionSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("exception")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.literal("all")
                                                .executes(context -> executeAddExceptionAll(context, plugin))
                                        )
                                        .then(CommandManager.argument("rule", StringArgumentType.word())
                                                .suggests(suggestAllRulesAndAdvanced())
                                                .executes(context -> executeAddException(context, plugin))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.literal("all")
                                                .executes(context -> executeRemoveExceptionAll(context, plugin))
                                        )
                                        .then(CommandManager.argument("rule", StringArgumentType.word())
                                                .suggests(suggestAllRulesAndAdvanced())
                                                .executes(context -> executeRemoveException(context, plugin))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .executes(context -> executeListExceptions(context, plugin))
                        )
                );
    }

    private static int executeAddExceptionAll(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) {
            source.sendError(TextUtil.parse("<red>No se encontraron jugadores con ese selector"));
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        int count = 0;
        for (ServerPlayerEntity target : targets) {
            if (area.addException("all", target.getGameProfile().getName())) {
                count++;
            }
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        final int finalCount = count;
        if (count > 0) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Excepción agregada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>all"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Jugadores: <gold>" + finalCount), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("<yellow>Estas excepciones ya existen"), false);
        }

        return 1;
    }

    private static int executeAddException(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) {
            source.sendError(TextUtil.parse("<red>No se encontraron jugadores con ese selector"));
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        if (!isValidRule(ruleKey)) {
            source.sendError(TextUtil.parse("<red>Regla inválida: " + ruleKey));
            return 0;
        }

        int count = 0;
        for (ServerPlayerEntity target : targets) {
            if (area.addException(ruleKey, target.getGameProfile().getName())) {
                count++;
            }
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        final int finalCount = count;
        if (count > 0) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Excepción agregada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>" + ruleKey), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Jugadores: <gold>" + finalCount), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("<yellow>Estas excepciones ya existen"), false);
        }

        return 1;
    }

    private static int executeRemoveExceptionAll(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) {
            source.sendError(TextUtil.parse("<red>No se encontraron jugadores con ese selector"));
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        int count = 0;
        for (ServerPlayerEntity target : targets) {
            if (area.removeException("all", target.getGameProfile().getName())) {
                count++;
            }
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        final int finalCount = count;
        if (count > 0) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Excepción eliminada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>all"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Jugadores: <gold>" + finalCount), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("<yellow>Estas excepciones no existían"), false);
        }

        return 1;
    }

    private static int executeRemoveException(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) {
            source.sendError(TextUtil.parse("<red>No se encontraron jugadores con ese selector"));
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        int count = 0;
        for (ServerPlayerEntity target : targets) {
            if (area.removeException(ruleKey, target.getGameProfile().getName())) {
                count++;
            }
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        final int finalCount = count;
        if (count > 0) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Excepción eliminada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>" + ruleKey), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Jugadores: <gold>" + finalCount), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("<yellow>Estas excepciones no existían"), false);
        }

        return 1;
    }

    private static int executeListExceptions(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        Map<String, Set<String>> exceptions = area.getAllExceptions();

        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        source.sendFeedback(() -> TextUtil.parse("<gold><bold>Excepciones del Área: <yellow>" + areaId), false);
        source.sendFeedback(() -> TextUtil.parse(""), false);

        if (exceptions.isEmpty()) {
            source.sendFeedback(() -> TextUtil.parse("  <gray>Esta área no tiene excepciones configuradas"), false);
        } else {
            for (Map.Entry<String, Set<String>> entry : exceptions.entrySet()) {
                source.sendFeedback(() -> TextUtil.parse("  <green>● <gold>" + entry.getKey().toUpperCase()), false);
                for (String playerName : entry.getValue()) {
                    source.sendFeedback(() -> TextUtil.parse("    <gray>- <yellow>" + playerName), false);
                }
            }
        }

        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAllRulesAndAdvanced() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) {
                builder.suggest(key);
            }
            builder.suggest("no_break");
            builder.suggest("no_place");
            builder.suggest("no_interact");
            builder.suggest("limit");
            return builder.buildFuture();
        };
    }

    private static boolean isValidRule(String ruleKey) {
        if (AreaRule.fromKey(ruleKey) != null) {
            return true;
        }
        return ruleKey.equalsIgnoreCase("no_break") ||
                ruleKey.equalsIgnoreCase("no_place") ||
                ruleKey.equalsIgnoreCase("no_interact") ||
                ruleKey.equalsIgnoreCase("limit");
    }

    private static LiteralArgumentBuilder<ServerCommandSource> rulesSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("rules")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(CommandManager.argument("rule", StringArgumentType.word())
                                        .suggests(suggestAllRules())
                                        .executes(context -> executeAddRule(context, plugin))
                                )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(CommandManager.argument("rule_name", StringArgumentType.word())
                                        .suggests(suggestAllRules())
                                        .executes(context -> executeRemoveRule(context, plugin))
                                )
                        )
                )
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .executes(context -> executeListRules(context, plugin))
                        )
                );
    }

    private static int executeAddRule(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "id");
        String ruleKey = StringArgumentType.getString(context, "rule");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        AreaRule rule = AreaRule.fromKey(ruleKey);
        if (rule == null) {
            source.sendError(TextUtil.parse("<red>Regla inválida: " + ruleKey));
            source.sendFeedback(() -> TextUtil.parse("<yellow>Reglas disponibles: " + String.join(", ", AreaRule.getAllKeys())), false);
            return 0;
        }

        if (area.hasRule(rule)) {
            source.sendFeedback(() -> TextUtil.parse("<yellow>El área '" + areaId + "' ya tiene la regla: <gold>" + rule.getKey()), false);
            return 0;
        }

        if (plugin.getAreaManager().addRuleToArea(areaId, rule)) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Regla agregada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>" + rule.getKey() + " <gray>- " + rule.getDescription()), false);
            return 1;
        } else {
            source.sendError(TextUtil.parse("<red>Error al agregar la regla"));
            return 0;
        }
    }

    private static int executeRemoveRule(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule_name");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        AreaRule rule = AreaRule.fromKey(ruleKey);
        if (rule == null) {
            source.sendError(TextUtil.parse("<red>Regla inválida: " + ruleKey));
            return 0;
        }

        if (!area.hasRule(rule)) {
            source.sendFeedback(() -> TextUtil.parse("<yellow>El área '" + areaId + "' no tiene la regla: <gold>" + rule.getKey()), false);
            return 0;
        }

        if (plugin.getAreaManager().removeRuleFromArea(areaId, rule)) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Regla eliminada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>" + rule.getKey()), false);
            return 1;
        } else {
            source.sendError(TextUtil.parse("<red>Error al eliminar la regla"));
            return 0;
        }
    }

    private static int executeListRules(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        Set<AreaRule> rules = area.getRules();

        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        source.sendFeedback(() -> TextUtil.parse("<gold><bold>Reglas del Área: <yellow>" + areaId), false);
        source.sendFeedback(() -> TextUtil.parse(""), false);

        if (rules.isEmpty()) {
            source.sendFeedback(() -> TextUtil.parse("  <gray>Esta área no tiene reglas configuradas"), false);
        } else {
            for (AreaRule rule : rules) {
                source.sendFeedback(() -> TextUtil.parse("  <green>● <gold>" + rule.getKey()), false);
                source.sendFeedback(() -> TextUtil.parse("    <gray>" + rule.getDescription()), false);
            }
        }

        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAllRules() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) {
                builder.suggest(key);
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestTargetBlockX() {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getEntity() instanceof ServerPlayerEntity p ? p : null;
            if (player == null) return builder.buildFuture();
            HitResult hit = player.raycast(100, 0, false);
            BlockPos pos = (hit instanceof BlockHitResult bhr) ? bhr.getBlockPos() : player.getBlockPos();
            builder.suggest(String.valueOf(pos.getX()));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestTargetBlockY() {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getEntity() instanceof ServerPlayerEntity p ? p : null;
            if (player == null) return builder.buildFuture();
            HitResult hit = player.raycast(100, 0, false);
            BlockPos pos = (hit instanceof BlockHitResult bhr) ? bhr.getBlockPos() : player.getBlockPos();
            builder.suggest(String.valueOf(pos.getY()));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestTargetBlockZ() {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getEntity() instanceof ServerPlayerEntity p ? p : null;
            if (player == null) return builder.buildFuture();
            HitResult hit = player.raycast(100, 0, false);
            BlockPos pos = (hit instanceof BlockHitResult bhr) ? bhr.getBlockPos() : player.getBlockPos();
            builder.suggest(String.valueOf(pos.getZ()));
            return builder.buildFuture();
        };
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("create")
                .then(CommandManager.literal("cube")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .then(CommandManager.argument("x1", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                        .then(CommandManager.argument("y1", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                .then(CommandManager.argument("z1", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                        .then(CommandManager.argument("x2", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                                .then(CommandManager.argument("y2", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                                        .then(CommandManager.argument("z2", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                                .executes(context -> executeCreateCube(context, plugin))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("flat")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .then(CommandManager.argument("flatPosition", IntegerArgumentType.integer(0, 16))
                                        .then(CommandManager.argument("x1", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                .then(CommandManager.argument("y1", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                        .then(CommandManager.argument("z1", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                .then(CommandManager.argument("x2", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                                        .then(CommandManager.argument("y2", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                                                .then(CommandManager.argument("z2", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                                        .executes(context -> executeCreateFlat(context, plugin))
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    private static int executeCreateCube(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity p ? p : null;

        String id = StringArgumentType.getString(context, "id");
        int x1 = IntegerArgumentType.getInteger(context, "x1");
        int y1 = IntegerArgumentType.getInteger(context, "y1");
        int z1 = IntegerArgumentType.getInteger(context, "z1");
        int x2 = IntegerArgumentType.getInteger(context, "x2");
        int y2 = IntegerArgumentType.getInteger(context, "y2");
        int z2 = IntegerArgumentType.getInteger(context, "z2");

        String dimension = (player != null)
                ? player.getServerWorld().getRegistryKey().getValue().toString()
                : "minecraft:overworld";

        if (plugin.getAreaManager().createArea(id, dimension, dimension, x1, y1, z1, x2, y2, z2, "cube", 0)) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Área '" + id + "' creada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Tipo: <gold>cube"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Dimensión: <gold>" + dimension), false);
            ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
            plugin.getAreaManager().broadcastNewArea(area);
            return 1;
        } else {
            source.sendError(TextUtil.parse("<red>Ya existe un área con el ID: " + id));
            return 0;
        }
    }

    private static int executeCreateFlat(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity p ? p : null;

        String id = StringArgumentType.getString(context, "id");
        int flatPosition = IntegerArgumentType.getInteger(context, "flatPosition");
        int x1 = IntegerArgumentType.getInteger(context, "x1");
        int y1 = IntegerArgumentType.getInteger(context, "y1");
        int z1 = IntegerArgumentType.getInteger(context, "z1");
        int x2 = IntegerArgumentType.getInteger(context, "x2");
        int y2 = IntegerArgumentType.getInteger(context, "y2");
        int z2 = IntegerArgumentType.getInteger(context, "z2");

        String dimension = (player != null)
                ? player.getServerWorld().getRegistryKey().getValue().toString()
                : "minecraft:overworld";

        if (plugin.getAreaManager().createArea(id, dimension, dimension, x1, y1, z1, x2, y2, z2, "flat", flatPosition)) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Área '" + id + "' creada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Tipo: <gold>flat <yellow>| Posición: <gold>" + flatPosition + "/16"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Dimensión: <gold>" + dimension), false);
            ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
            plugin.getAreaManager().broadcastNewArea(area);
            return 1;
        } else {
            source.sendError(TextUtil.parse("<red>Ya existe un área con el ID: " + id));
            return 0;
        }
    }

    private static LiteralArgumentBuilder<ServerCommandSource> removeSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("remove")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            String id = StringArgumentType.getString(context, "id");

                            if (plugin.getAreaManager().removeArea(id)) {
                                source.sendFeedback(() -> TextUtil.parse("<green>¡Área '" + id + "' eliminada exitosamente!"), false);
                                plugin.getAreaManager().broadcastRemoveArea(id);
                                return 1;
                            } else {
                                source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + id));
                                return 0;
                            }
                        })
                );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> reloadSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("reload")
                .then(CommandManager.literal("notifications")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> TextUtil.parse("<yellow>Recargando configuraciones de notificaciones..."), false);
                            plugin.getNotificationManager().reloadConfigs();
                            source.sendFeedback(() -> TextUtil.parse("<green>¡Notificaciones recargadas exitosamente!"), false);
                            return 1;
                        }))
                .then(CommandManager.literal("config")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> TextUtil.parse("<yellow>Recargando configuración del plugin..."), false);
                            plugin.getConfigManager().reloadConfig();
                            source.sendFeedback(() -> TextUtil.parse("<green>¡Configuración recargada exitosamente!"), false);

                            boolean modRequired = plugin.getConfigManager().isModRequired();
                            String status = modRequired ? "<green><bold>ACTIVADO" : "<red><bold>DESACTIVADO";
                            source.sendFeedback(() -> TextUtil.parse("<yellow>Estado del mod obligatorio: " + status), false);

                            return 1;
                        }))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    source.sendFeedback(() -> TextUtil.parse("<yellow>Recargando áreas..."), false);
                    plugin.getAreaManager().reloadAreas();
                    source.sendFeedback(() -> TextUtil.parse("<green>¡Áreas recargadas exitosamente! Total: " +
                            plugin.getAreaManager().getAreas().size()), false);
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> viewSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("view")
                .then(CommandManager.argument("target", EntityArgumentType.players())
                        .then(CommandManager.argument("enable", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerPlayerEntity sourcePlayer = source.getEntity() instanceof ServerPlayerEntity p ? p : null;

                                    boolean enable = BoolArgumentType.getBool(context, "enable");

                                    Collection<ServerPlayerEntity> targets;
                                    try {
                                        targets = EntityArgumentType.getPlayers(context, "target");
                                    } catch (CommandSyntaxException e) {
                                        source.sendError(TextUtil.parse("<red>No se encontraron jugadores con ese selector"));
                                        return 0;
                                    }

                                    if (targets.isEmpty()) {
                                        source.sendError(TextUtil.parse("<red>No se encontraron jugadores con ese selector"));
                                        return 0;
                                    }

                                    int count = 0;
                                    for (ServerPlayerEntity t : targets) {
                                        plugin.getAreaManager().sendViewToggle(t, enable);
                                        count++;
                                    }

                                    String statusStr = enable ? "<green>activada" : "<red>desactivada";
                                    final int finalCount = count;
                                    List<ServerPlayerEntity> targetList = List.copyOf(targets);
                                    if (count == 1) {
                                        ServerPlayerEntity target = targetList.get(0);
                                        if (sourcePlayer != null && target.getUuid().equals(sourcePlayer.getUuid())) {
                                            source.sendFeedback(() -> TextUtil.parse("<yellow>Visualización de áreas " + statusStr), false);
                                        } else {
                                            source.sendFeedback(() -> TextUtil.parse("<yellow>Visualización de áreas " + statusStr + " <yellow>para <gold>" + target.getGameProfile().getName()), false);
                                        }
                                    } else {
                                        source.sendFeedback(() -> TextUtil.parse("<yellow>Visualización de áreas " + statusStr + " <yellow>para <gold>" + finalCount + " jugador(es)"), false);
                                    }

                                    return 1;
                                })
                        )
                );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> colorSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("color")
                .then(CommandManager.argument("id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("hexcolor", StringArgumentType.word())
                                .executes(context -> executeColorCommand(context, plugin, ""))
                                .then(CommandManager.argument("alias", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String alias = StringArgumentType.getString(context, "alias");
                                            return executeColorCommand(context, plugin, alias);
                                        })
                                )
                        )
                );
    }

    private static int executeColorCommand(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin, String alias) {
        ServerCommandSource source = context.getSource();

        String providedId = StringArgumentType.getString(context, "id");
        String rawColor = StringArgumentType.getString(context, "hexcolor");

        if (!rawColor.startsWith("#")) {
            rawColor = "#" + rawColor;
        }

        if (!rawColor.matches("^#[0-9A-Fa-f]{6}$")) {
            source.sendError(TextUtil.parse("<red>El color debe estar en formato hexadecimal (#RRGGBB) – ej: #FF0000 o FF0000"));
            return 0;
        }

        final String finalRawColor = rawColor;
        Map<String, ProtectedArea> areas = plugin.getAreaManager().getAreas();
        ProtectedArea area = areas.get(providedId);
        String matchedId = providedId;
        if (area == null) {
            for (String key : areas.keySet()) {
                if (key.equalsIgnoreCase(providedId)) {
                    matchedId = key;
                    area = areas.get(key);
                    break;
                }
            }
        }

        if (area == null) {
            final String pid = providedId;
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + pid));
            return 0;
        }

        String normalizedColor = finalRawColor.toUpperCase();
        final String fMatchedId = matchedId;
        if (plugin.getAreaManager().setAreaColor(matchedId, normalizedColor, alias)) {
            source.sendFeedback(() -> TextUtil.parse("<green>Color del área '" + fMatchedId + "' actualizado a " + normalizedColor), false);
            if (alias != null && !alias.isEmpty()) {
                source.sendFeedback(() -> TextUtil.parse("<green>Alias: " + alias), false);
            }
            ProtectedArea updated = plugin.getAreaManager().getAreas().get(matchedId);
            if (updated != null) {
                plugin.getAreaManager().broadcastUpdateArea(updated);
            }
            return 1;
        } else {
            source.sendError(TextUtil.parse("<red>No se pudo actualizar el color del área: " + fMatchedId));
            return 0;
        }
    }

    private static SuggestionProvider<ServerCommandSource> suggestAreaIds(ProtectedAreaInit plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().keySet().forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestCubeAreaIds(ProtectedAreaInit plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
