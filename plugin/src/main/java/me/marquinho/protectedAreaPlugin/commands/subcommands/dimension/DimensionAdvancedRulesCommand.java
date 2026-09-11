package me.marquinho.protectedAreaPlugin.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.AdvancedAreaRules;
import me.marquinho.protectedAreaPlugin.models.AdvancedRuleType;
import me.marquinho.protectedAreaPlugin.models.AdvancedRuleType.Target;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.block.BlockType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemType;

import java.util.List;
import java.util.Set;

public class DimensionAdvancedRulesCommand {

    private static final String ARG_AREA = "area_id";
    private static final String ARG_BLOCK = "block_id";
    private static final String ARG_ENTITY = "entity_id";
    private static final String ARG_ITEM = "item_id";

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("advanced")
                .then(Commands.literal("rules")
                        .then(addSubcommand(plugin))
                        .then(removeSubcommand(plugin))
                        .then(listSubcommand(plugin)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> addSubcommand(ProtectedAreaPlugin plugin) {
        RequiredArgumentBuilder<CommandSourceStack, String> areaArg = areaArgument(plugin);

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            LiteralArgumentBuilder<CommandSourceStack> ruleNode = Commands.literal(ruleType.getKey());

            for (Target target : ruleType.getTargets()) {
                ruleNode.then(Commands.literal(target.getKey())
                        .then(targetArgument(target)
                                .executes(context -> executeAdd(context, plugin, ruleType, target))));
            }

            areaArg.then(ruleNode);
        }

        return Commands.literal("add").then(areaArg);
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin,
                                  AdvancedRuleType ruleType, Target target) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, ARG_AREA);

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            String folderTargetId = resolveTargetId(context, target);
            int count = 0;
            for (ProtectedArea a : areaList) {
                String folderKey = a.getStorageKey();
                boolean applied = target == Target.ENTITY
                        ? plugin.getAdvancedRulesManager().addEntityRule(folderKey, ruleType, folderTargetId)
                        : plugin.getAdvancedRulesManager().addBlockRule(folderKey, ruleType, folderTargetId);
                if (applied) count++;
            }
            sender.sendMessage("§aAdvanced rule §6" + ruleType.getKey() + " §aapplied to §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        String targetId = resolveTargetId(context, target);

        String key = area.getStorageKey();
        boolean added = target == Target.ENTITY
                ? plugin.getAdvancedRulesManager().addEntityRule(key, ruleType, targetId)
                : plugin.getAdvancedRulesManager().addBlockRule(key, ruleType, targetId);

        if (added) {
            sender.sendMessage("§aAdvanced rule added successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6" + ruleType.getKey());
            sender.sendMessage("§eType: §6" + target.getDisplayName());
            sender.sendMessage("§eID: §6" + targetId);
        } else {
            sender.sendMessage("§eThis rule already exists for " + target.getDisplayName().toLowerCase() + ": " + targetId);
        }
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> removeSubcommand(ProtectedAreaPlugin plugin) {
        RequiredArgumentBuilder<CommandSourceStack, String> areaArg = areaArgument(plugin);

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            LiteralArgumentBuilder<CommandSourceStack> ruleNode = Commands.literal(ruleType.getKey())
                    .then(Commands.literal("all")
                            .executes(context -> executeRemoveAll(context, plugin, ruleType)));

            for (Target target : ruleType.getTargets()) {
                ruleNode.then(Commands.literal(target.getKey())
                        .then(targetArgument(target)
                                .executes(context -> executeRemove(context, plugin, ruleType, target))));
            }

            areaArg.then(ruleNode);
        }

        return Commands.literal("remove").then(areaArg);
    }

    private static int executeRemoveAll(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin,
                                        AdvancedRuleType ruleType) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, ARG_AREA);

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            for (ProtectedArea a : areaList) plugin.getAdvancedRulesManager().clearRule(a.getStorageKey(), ruleType);
            sender.sendMessage("§aAdvanced rules of type §6" + ruleType.getKey() + " §aremoved from §6" + areaList.size() + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        plugin.getAdvancedRulesManager().clearRule(area.getStorageKey(), ruleType);
        sender.sendMessage("§aAll rules of type §6" + ruleType.getKey() + " §ahave been removed!");
        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin,
                                     AdvancedRuleType ruleType, Target target) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, ARG_AREA);

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            String folderTargetId = resolveTargetId(context, target);
            int count = 0;
            for (ProtectedArea a : areaList) {
                String folderKey = a.getStorageKey();
                boolean cleared = target == Target.ENTITY
                        ? plugin.getAdvancedRulesManager().removeEntityRule(folderKey, ruleType, folderTargetId)
                        : plugin.getAdvancedRulesManager().removeBlockRule(folderKey, ruleType, folderTargetId);
                if (cleared) count++;
            }
            sender.sendMessage("§aAdvanced rule §6" + ruleType.getKey() + " §aremoved from §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        String targetId = resolveTargetId(context, target);

        String key = area.getStorageKey();
        boolean removed = target == Target.ENTITY
                ? plugin.getAdvancedRulesManager().removeEntityRule(key, ruleType, targetId)
                : plugin.getAdvancedRulesManager().removeBlockRule(key, ruleType, targetId);

        if (removed) {
            sender.sendMessage("§aAdvanced rule removed successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6" + ruleType.getKey());
            sender.sendMessage("§e" + target.getDisplayName() + ": §6" + targetId);
        } else {
            sender.sendMessage("§cThat rule was not found");
        }
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listSubcommand(ProtectedAreaPlugin plugin) {
        RequiredArgumentBuilder<CommandSourceStack, String> areaArg = areaArgument(plugin);

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            areaArg.then(Commands.literal(ruleType.getKey())
                    .executes(context -> executeList(context, plugin, ruleType)));
        }

        return Commands.literal("list").then(areaArg);
    }

    private static int executeList(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin,
                                   AdvancedRuleType ruleType) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, ARG_AREA);

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        AdvancedAreaRules rules = plugin.getAdvancedRulesManager().getRules(area.getStorageKey());
        Set<String> blocks = rules.getBlocks(ruleType);
        Set<String> entities = rules.getEntities(ruleType);

        String blocksLabel = ruleType.supports(Target.ITEM) ? "Items:" : "Blocks:";

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lAdvanced Rules - §e" + areaId);
        sender.sendMessage("§6§lType: §e" + ruleType.getKey());
        sender.sendMessage("");
        if (blocks.isEmpty() && entities.isEmpty()) {
            sender.sendMessage("  §7No rules configured of this type");
        } else {
            if (!blocks.isEmpty()) {
                sender.sendMessage("  §a§l" + blocksLabel);
                for (String block : blocks) sender.sendMessage("    §7- §6" + block);
            }
            if (!entities.isEmpty()) {
                sender.sendMessage("");
                sender.sendMessage("  §a§lEntities:");
                for (String entity : entities) sender.sendMessage("    §7- §6" + entity);
            }
        }
        sender.sendMessage("§e§m                                          ");
        return 1;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> areaArgument(ProtectedAreaPlugin plugin) {
        return Commands.argument(ARG_AREA, StringArgumentType.string()).suggests(DimensionAreas.suggestIds(plugin));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> targetArgument(Target target) {
        return switch (target) {
            case BLOCK -> Commands.argument(ARG_BLOCK, ArgumentTypes.resource(RegistryKey.BLOCK));
            case ITEM -> Commands.argument(ARG_ITEM, ArgumentTypes.resource(RegistryKey.ITEM));
            case ENTITY -> Commands.argument(ARG_ENTITY, ArgumentTypes.resource(RegistryKey.ENTITY_TYPE));
        };
    }

    private static String resolveTargetId(CommandContext<CommandSourceStack> context, Target target) {
        return switch (target) {
            case BLOCK -> context.getArgument(ARG_BLOCK, BlockType.class).getKey().asString();
            case ITEM -> context.getArgument(ARG_ITEM, ItemType.class).getKey().asString();
            case ENTITY -> context.getArgument(ARG_ENTITY, EntityType.class).getKey().asString();
        };
    }
}
