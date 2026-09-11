package me.marquinho.protectedarea.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.AdvancedAreaRules;
import me.marquinho.protectedarea.models.AdvancedRuleType;
import me.marquinho.protectedarea.models.AdvancedRuleType.Target;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Set;

public class DimensionAdvancedRulesCommand {

    private static final String ARG_AREA = "area_id";
    private static final String ARG_BLOCK = "block_id";
    private static final String ARG_ENTITY = "entity_id";
    private static final String ARG_ITEM = "item_id";

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
        RequiredArgumentBuilder<ServerCommandSource, String> areaArg = areaArgument(plugin);

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            LiteralArgumentBuilder<ServerCommandSource> ruleNode = CommandManager.literal(ruleType.getKey());

            for (Target target : ruleType.getTargets()) {
                ruleNode.then(CommandManager.literal(target.getKey())
                        .then(targetArgument(target, registryAccess)
                                .executes(context -> executeAdd(context, plugin, ruleType, target))));
            }

            areaArg.then(ruleNode);
        }

        return CommandManager.literal("add").then(areaArg);
    }

    private static int executeAdd(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin,
                                  AdvancedRuleType ruleType, Target target) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, ARG_AREA);

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            String folderTargetId = resolveTargetId(context, target);
            if (folderTargetId == null) {
                Messages.error(source, "protectedarea.command.advrules.invalid_entity_type",
                        Messages.arg("id", String.valueOf(IdentifierArgumentType.getIdentifier(context, ARG_ENTITY))));
                return 0;
            }
            int count = 0;
            for (ProtectedArea a : areaList) {
                String folderKey = a.getStorageKey();
                boolean applied = target == Target.ENTITY
                        ? plugin.getAdvancedRulesManager().addEntityRule(folderKey, ruleType, folderTargetId)
                        : plugin.getAdvancedRulesManager().addBlockRule(folderKey, ruleType, folderTargetId);
                if (applied) count++;
            }
            final int c = count;
            Messages.send(source, "protectedarea.command.advrules.applied.folder",
                    Messages.arg("rule", ruleType.getKey()), Messages.arg("count", c), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        String targetId = resolveTargetId(context, target);
        if (targetId == null) {
            Messages.error(source, "protectedarea.command.advrules.invalid_entity_type",
                    Messages.arg("id", String.valueOf(IdentifierArgumentType.getIdentifier(context, ARG_ENTITY))));
            return 0;
        }

        String key = area.getStorageKey();
        boolean added = target == Target.ENTITY
                ? plugin.getAdvancedRulesManager().addEntityRule(key, ruleType, targetId)
                : plugin.getAdvancedRulesManager().addBlockRule(key, ruleType, targetId);

        if (added) {
            Messages.send(source, "protectedarea.command.advrules.added.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.common.rule_line", Messages.arg("rule", ruleType.getKey()));
            Messages.send(source, "protectedarea.command.advrules.type_line", Messages.arg("type", target.getDisplayName()));
            Messages.send(source, "protectedarea.command.advrules.id_line", Messages.arg("id", targetId));
        } else {
            Messages.send(source, "protectedarea.command.advrules.already_exists",
                    Messages.arg("type", target.getDisplayName().toLowerCase()), Messages.arg("id", targetId));
        }

        return 1;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> removeSubcommand(ProtectedAreaInit plugin,
                                                                                CommandRegistryAccess registryAccess) {
        RequiredArgumentBuilder<ServerCommandSource, String> areaArg = areaArgument(plugin);

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            LiteralArgumentBuilder<ServerCommandSource> ruleNode = CommandManager.literal(ruleType.getKey())
                    .then(CommandManager.literal("all")
                            .executes(context -> executeRemoveAll(context, plugin, ruleType)));

            for (Target target : ruleType.getTargets()) {
                ruleNode.then(CommandManager.literal(target.getKey())
                        .then(targetArgument(target, registryAccess)
                                .executes(context -> executeRemove(context, plugin, ruleType, target))));
            }

            areaArg.then(ruleNode);
        }

        return CommandManager.literal("remove").then(areaArg);
    }

    private static int executeRemoveAll(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin,
                                        AdvancedRuleType ruleType) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, ARG_AREA);

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            for (ProtectedArea a : areaList) plugin.getAdvancedRulesManager().clearRule(a.getStorageKey(), ruleType);
            final int t = areaList.size();
            Messages.send(source, "protectedarea.command.advrules.cleared.folder",
                    Messages.arg("rule", ruleType.getKey()), Messages.arg("count", t), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        plugin.getAdvancedRulesManager().clearRule(area.getStorageKey(), ruleType);
        Messages.send(source, "protectedarea.command.advrules.cleared", Messages.arg("rule", ruleType.getKey()));
        return 1;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin,
                                     AdvancedRuleType ruleType, Target target) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, ARG_AREA);

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            String folderTargetId = resolveTargetId(context, target);
            if (folderTargetId == null) {
                Messages.error(source, "protectedarea.command.advrules.invalid_entity_type",
                        Messages.arg("id", String.valueOf(IdentifierArgumentType.getIdentifier(context, ARG_ENTITY))));
                return 0;
            }
            int count = 0;
            for (ProtectedArea a : areaList) {
                String folderKey = a.getStorageKey();
                boolean cleared = target == Target.ENTITY
                        ? plugin.getAdvancedRulesManager().removeEntityRule(folderKey, ruleType, folderTargetId)
                        : plugin.getAdvancedRulesManager().removeBlockRule(folderKey, ruleType, folderTargetId);
                if (cleared) count++;
            }
            final int c = count;
            Messages.send(source, "protectedarea.command.advrules.removed.folder",
                    Messages.arg("rule", ruleType.getKey()), Messages.arg("count", c), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        String targetId = resolveTargetId(context, target);
        if (targetId == null) {
            Messages.error(source, "protectedarea.command.advrules.invalid_entity_type",
                    Messages.arg("id", String.valueOf(IdentifierArgumentType.getIdentifier(context, ARG_ENTITY))));
            return 0;
        }

        String key = area.getStorageKey();
        boolean removed = target == Target.ENTITY
                ? plugin.getAdvancedRulesManager().removeEntityRule(key, ruleType, targetId)
                : plugin.getAdvancedRulesManager().removeBlockRule(key, ruleType, targetId);

        if (removed) {
            Messages.send(source, "protectedarea.command.advrules.removed.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.common.rule_line", Messages.arg("rule", ruleType.getKey()));
            Messages.send(source, "protectedarea.command.advrules.target_line", Messages.arg("target", target.getDisplayName()), Messages.arg("id", targetId));
        } else {
            Messages.error(source, "protectedarea.command.advrules.not_found");
        }
        return 1;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> listSubcommand(ProtectedAreaInit plugin) {
        RequiredArgumentBuilder<ServerCommandSource, String> areaArg = areaArgument(plugin);

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            areaArg.then(CommandManager.literal(ruleType.getKey())
                    .executes(context -> executeList(context, plugin, ruleType)));
        }

        return CommandManager.literal("list").then(areaArg);
    }

    private static int executeList(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin,
                                   AdvancedRuleType ruleType) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, ARG_AREA);

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        AdvancedAreaRules rules = plugin.getAdvancedRulesManager().getRules(area.getStorageKey());
        Set<String> blocks = rules.getBlocks(ruleType);
        Set<String> entities = rules.getEntities(ruleType);

        String blocksLabel = ruleType.supports(Target.ITEM) ? "Items:" : "Blocks:";

        Messages.send(source, "protectedarea.command.exception.list.divider");
        Messages.send(source, "protectedarea.command.advrules.list.header", Messages.arg("area", areaId));
        Messages.send(source, "protectedarea.command.advrules.list.type_header", Messages.arg("type", ruleType.getKey()));
        Messages.send(source, "protectedarea.command.common.blank_line");
        if (blocks.isEmpty() && entities.isEmpty()) {
            Messages.send(source, "protectedarea.command.advrules.list.empty");
        } else {
            if (!blocks.isEmpty()) {
                Messages.send(source, "protectedarea.command.advrules.list.section_label", Messages.arg("label", blocksLabel));
                for (String block : blocks) Messages.send(source, "protectedarea.command.advrules.list.item", Messages.arg("value", block));
            }
            if (!entities.isEmpty()) {
                Messages.send(source, "protectedarea.command.common.blank_line");
                Messages.send(source, "protectedarea.command.advrules.list.entities_label");
                for (String entity : entities) Messages.send(source, "protectedarea.command.advrules.list.item", Messages.arg("value", entity));
            }
        }
        Messages.send(source, "protectedarea.command.exception.list.divider");
        return 1;
    }

    private static RequiredArgumentBuilder<ServerCommandSource, String> areaArgument(ProtectedAreaInit plugin) {
        return CommandManager.argument(ARG_AREA, StringArgumentType.string()).suggests(DimensionAreas.suggestIds(plugin));
    }

    private static RequiredArgumentBuilder<ServerCommandSource, ?> targetArgument(Target target,
                                                                                  CommandRegistryAccess registryAccess) {
        return switch (target) {
            case BLOCK -> CommandManager.argument(ARG_BLOCK, BlockStateArgumentType.blockState(registryAccess));
            case ITEM -> CommandManager.argument(ARG_ITEM, ItemStackArgumentType.itemStack(registryAccess));
            case ENTITY -> CommandManager.argument(ARG_ENTITY, IdentifierArgumentType.identifier())
                    .suggests(suggestEntityTypes());
        };
    }

    private static String resolveTargetId(CommandContext<ServerCommandSource> context, Target target) {
        return switch (target) {
            case BLOCK -> Registries.BLOCK.getId(
                    BlockStateArgumentType.getBlockState(context, ARG_BLOCK).getBlockState().getBlock()).toString();
            case ITEM -> Registries.ITEM.getId(
                    ItemStackArgumentType.getItemStackArgument(context, ARG_ITEM).getItem()).toString();
            case ENTITY -> {
                Identifier id = IdentifierArgumentType.getIdentifier(context, ARG_ENTITY);
                yield Registries.ENTITY_TYPE.containsId(id) ? id.toString() : null;
            }
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestEntityTypes() {
        return (context, builder) -> {
            String remaining = builder.getRemaining().toLowerCase();
            for (Identifier id : Registries.ENTITY_TYPE.getIds()) {
                String full = id.toString();
                if (full.startsWith(remaining) || id.getPath().startsWith(remaining)) {
                    builder.suggest(full);
                }
            }
            return builder.buildFuture();
        };
    }
}
