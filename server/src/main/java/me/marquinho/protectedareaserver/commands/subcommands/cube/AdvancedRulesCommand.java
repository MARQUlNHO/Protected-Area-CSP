package me.marquinho.protectedareaserver.commands.subcommands.cube;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.models.AdvancedAreaRules;
import me.marquinho.protectedareaserver.models.AdvancedRuleType;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.Set;

public class AdvancedRulesCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin,
                                                                           CommandRegistryAccess registryAccess) {
        return CommandManager.literal("advanced")
                .then(CommandManager.literal("rules")
                        .then(addSubcommand(plugin, registryAccess))
                        .then(removeSubcommand(plugin, registryAccess))
                        .then(listSubcommand(plugin)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> addSubcommand(ProtectedAreaInit plugin,
                                                                             CommandRegistryAccess registryAccess) {
        return CommandManager.literal("add")
                .then(CommandManager.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("rule_type", StringArgumentType.word())
                                .suggests(suggestAdvancedRuleTypes())
                                .then(CommandManager.argument("block_id", BlockStateArgumentType.blockState(registryAccess))
                                        .executes(context -> executeAddBlock(context, plugin)))
                                .then(CommandManager.argument("entity_id", StringArgumentType.word())
                                        .suggests(suggestEntityTypes())
                                        .executes(context -> executeAddEntity(context, plugin)))));
    }

    private static int executeAddBlock(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");

        var blockArg = BlockStateArgumentType.getBlockState(context, "block_id");
        String blockId = Registries.BLOCK.getId(blockArg.getBlockState().getBlock()).toString();

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) {
            source.sendError(TextUtil.parse("<red>Tipo de regla inválido: " + ruleTypeKey));
            return 0;
        }

        boolean added = plugin.getAdvancedRulesManager().addBlockRule(areaId, ruleType, blockId);

        if (added) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Regla avanzada agregada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>" + ruleType.getKey()), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Tipo: <gold>Bloque"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>ID: <gold>" + blockId), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("<yellow>Esta regla ya existe para ese bloque"), false);
        }

        return 1;
    }

    private static int executeAddEntity(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        String entityId = StringArgumentType.getString(context, "entity_id");

        if (!entityId.contains(":")) entityId = "minecraft:" + entityId;
        Identifier entityIdentifier = Identifier.tryParse(entityId);
        if (entityIdentifier == null || !Registries.ENTITY_TYPE.containsId(entityIdentifier)) {
            final String eid = entityId;
            source.sendError(TextUtil.parse("<red>Tipo de entidad inválido: " + eid));
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId));
            return 0;
        }

        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) {
            source.sendError(TextUtil.parse("<red>Tipo de regla inválido: " + ruleTypeKey));
            return 0;
        }

        final String finalEntityId = entityIdentifier.toString();
        boolean added = plugin.getAdvancedRulesManager().addEntityRule(areaId, ruleType, finalEntityId);

        if (added) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Regla avanzada agregada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>" + ruleType.getKey()), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Tipo: <gold>Entidad"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>ID: <gold>" + finalEntityId), false);
        } else {
            source.sendFeedback(() -> TextUtil.parse("<yellow>Esta regla ya existe para esa entidad"), false);
        }

        return 1;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> removeSubcommand(ProtectedAreaInit plugin,
                                                                                CommandRegistryAccess registryAccess) {
        return CommandManager.literal("remove")
                .then(CommandManager.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("rule_type", StringArgumentType.word())
                                .suggests(suggestAdvancedRuleTypes())
                                .then(CommandManager.literal("all")
                                        .executes(context -> executeRemoveAll(context, plugin)))
                                .then(CommandManager.argument("block_id", BlockStateArgumentType.blockState(registryAccess))
                                        .executes(context -> executeRemoveBlock(context, plugin)))
                                .then(CommandManager.argument("entity_id", StringArgumentType.word())
                                        .suggests(suggestEntityTypes())
                                        .executes(context -> executeRemoveEntity(context, plugin)))));
    }

    private static int executeRemoveAll(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) {
            source.sendError(TextUtil.parse("<red>Tipo de regla inválido: " + ruleTypeKey));
            return 0;
        }
        plugin.getAdvancedRulesManager().clearRule(areaId, ruleType);
        source.sendFeedback(() -> TextUtil.parse("<green>¡Todas las reglas de tipo <gold>" + ruleType.getKey() + " <green>han sido eliminadas!"), false);
        return 1;
    }

    private static int executeRemoveBlock(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        var blockArg = BlockStateArgumentType.getBlockState(context, "block_id");
        String blockId = Registries.BLOCK.getId(blockArg.getBlockState().getBlock()).toString();
        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) {
            source.sendError(TextUtil.parse("<red>Tipo de regla inválido: " + ruleTypeKey));
            return 0;
        }
        boolean removed = plugin.getAdvancedRulesManager().removeBlockRule(areaId, ruleType, blockId);
        if (removed) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Regla avanzada eliminada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>" + ruleType.getKey()), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Bloque: <gold>" + blockId), false);
        } else {
            source.sendError(TextUtil.parse("<red>No se encontró esa regla"));
        }
        return 1;
    }

    private static int executeRemoveEntity(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        String entityId = StringArgumentType.getString(context, "entity_id");
        if (!entityId.contains(":")) entityId = "minecraft:" + entityId;
        final String finalEntityId = entityId;
        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) {
            source.sendError(TextUtil.parse("<red>Tipo de regla inválido: " + ruleTypeKey));
            return 0;
        }
        boolean removed = plugin.getAdvancedRulesManager().removeEntityRule(areaId, ruleType, finalEntityId);
        if (removed) {
            source.sendFeedback(() -> TextUtil.parse("<green>¡Regla avanzada eliminada exitosamente!"), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Regla: <gold>" + ruleType.getKey()), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Entidad: <gold>" + finalEntityId), false);
        } else {
            source.sendError(TextUtil.parse("<red>No se encontró esa regla"));
        }
        return 1;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> listSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("list")
                .then(CommandManager.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("rule_type", StringArgumentType.word())
                                .suggests(suggestAdvancedRuleTypes())
                                .executes(context -> executeList(context, plugin))));
    }

    private static int executeList(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) {
            source.sendError(TextUtil.parse("<red>Tipo de regla inválido: " + ruleTypeKey));
            return 0;
        }
        AdvancedAreaRules rules = plugin.getAdvancedRulesManager().getRules(areaId);
        Set<String> blocks = rules.getBlocks(ruleType);
        Set<String> entities = rules.getEntities(ruleType);
        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        source.sendFeedback(() -> TextUtil.parse("<gold><bold>Reglas Avanzadas - <yellow>" + areaId), false);
        source.sendFeedback(() -> TextUtil.parse("<gold><bold>Tipo: <yellow>" + ruleType.getKey()), false);
        source.sendFeedback(() -> TextUtil.parse(""), false);
        if (blocks.isEmpty() && entities.isEmpty()) {
            source.sendFeedback(() -> TextUtil.parse("  <gray>No hay reglas configuradas de este tipo"), false);
        } else {
            if (!blocks.isEmpty()) {
                source.sendFeedback(() -> TextUtil.parse("  <green><bold>Bloques:"), false);
                for (String block : blocks) source.sendFeedback(() -> TextUtil.parse("    <gray>- <gold>" + block), false);
            }
            if (!entities.isEmpty()) {
                source.sendFeedback(() -> TextUtil.parse(""), false);
                source.sendFeedback(() -> TextUtil.parse("  <green><bold>Entidades:"), false);
                for (String entity : entities) source.sendFeedback(() -> TextUtil.parse("    <gray>- <gold>" + entity), false);
            }
        }
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

    private static SuggestionProvider<ServerCommandSource> suggestAdvancedRuleTypes() {
        return (context, builder) -> {
            for (String key : AdvancedRuleType.getAllKeys()) builder.suggest(key);
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestEntityTypes() {
        return (context, builder) -> {
            for (Identifier id : Registries.ENTITY_TYPE.getIds()) builder.suggest(id.toString());
            return builder.buildFuture();
        };
    }
}
