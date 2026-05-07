package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.AdvancedAreaRules;
import me.marquinho.protectedAreaPlugin.models.AdvancedRuleType;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.block.BlockType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.Set;

public class AdvancedRulesCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("advanced")
                .then(Commands.literal("rules")
                        .then(addSubcommand(plugin))
                        .then(removeSubcommand(plugin))
                        .then(listSubcommand(plugin)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> addSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("add")
                .then(Commands.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(Commands.argument("rule_type", StringArgumentType.word())
                                .suggests(suggestAdvancedRuleTypes())
                                .then(Commands.argument("block_id", ArgumentTypes.resource(RegistryKey.BLOCK))
                                        .executes(context -> executeAddBlock(context, plugin)))
                                .then(Commands.argument("entity_id", ArgumentTypes.resource(RegistryKey.ENTITY_TYPE))
                                        .executes(context -> executeAddEntity(context, plugin)))));
    }

    private static int executeAddBlock(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        BlockType blockType = context.getArgument("block_id", BlockType.class);
        String blockId = blockType.getKey().asString();

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) { sender.sendMessage("§cTipo de regla inválido: " + ruleTypeKey); return 0; }

        boolean added = plugin.getAdvancedRulesManager().addBlockRule(areaId, ruleType, blockId);
        if (added) {
            sender.sendMessage("§a¡Regla avanzada agregada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6" + ruleType.getKey());
            sender.sendMessage("§eTipo: §6Bloque");
            sender.sendMessage("§eID: §6" + blockId);
        } else {
            sender.sendMessage("§eEsta regla ya existe para ese bloque");
        }
        return 1;
    }

    private static int executeAddEntity(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        EntityType entityType = context.getArgument("entity_id", EntityType.class);
        String entityId = entityType.getKey().asString();

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) { sender.sendMessage("§cTipo de regla inválido: " + ruleTypeKey); return 0; }

        boolean added = plugin.getAdvancedRulesManager().addEntityRule(areaId, ruleType, entityId);
        if (added) {
            sender.sendMessage("§a¡Regla avanzada agregada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6" + ruleType.getKey());
            sender.sendMessage("§eTipo: §6Entidad");
            sender.sendMessage("§eID: §6" + entityId);
        } else {
            sender.sendMessage("§eEsta regla ya existe para esa entidad");
        }
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> removeSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("remove")
                .then(Commands.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(Commands.argument("rule_type", StringArgumentType.word())
                                .suggests(suggestAdvancedRuleTypes())
                                .then(Commands.literal("all")
                                        .executes(context -> executeRemoveAll(context, plugin)))
                                .then(Commands.argument("block_id", ArgumentTypes.resource(RegistryKey.BLOCK))
                                        .executes(context -> executeRemoveBlock(context, plugin)))
                                .then(Commands.argument("entity_id", ArgumentTypes.resource(RegistryKey.ENTITY_TYPE))
                                        .executes(context -> executeRemoveEntity(context, plugin)))));
    }

    private static int executeRemoveAll(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) { sender.sendMessage("§cTipo de regla inválido: " + ruleTypeKey); return 0; }
        plugin.getAdvancedRulesManager().clearRule(areaId, ruleType);
        sender.sendMessage("§a¡Todas las reglas de tipo §6" + ruleType.getKey() + " §ahan sido eliminadas!");
        return 1;
    }

    private static int executeRemoveBlock(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        BlockType blockType = context.getArgument("block_id", BlockType.class);
        String blockId = blockType.getKey().asString();
        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) { sender.sendMessage("§cTipo de regla inválido: " + ruleTypeKey); return 0; }
        boolean removed = plugin.getAdvancedRulesManager().removeBlockRule(areaId, ruleType, blockId);
        if (removed) {
            sender.sendMessage("§a¡Regla avanzada eliminada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6" + ruleType.getKey());
            sender.sendMessage("§eBloque: §6" + blockId);
        } else {
            sender.sendMessage("§cNo se encontró esa regla");
        }
        return 1;
    }

    private static int executeRemoveEntity(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        EntityType entityType = context.getArgument("entity_id", EntityType.class);
        String entityId = entityType.getKey().asString();
        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) { sender.sendMessage("§cTipo de regla inválido: " + ruleTypeKey); return 0; }
        boolean removed = plugin.getAdvancedRulesManager().removeEntityRule(areaId, ruleType, entityId);
        if (removed) {
            sender.sendMessage("§a¡Regla avanzada eliminada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6" + ruleType.getKey());
            sender.sendMessage("§eEntidad: §6" + entityId);
        } else {
            sender.sendMessage("§cNo se encontró esa regla");
        }
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("list")
                .then(Commands.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(Commands.argument("rule_type", StringArgumentType.word())
                                .suggests(suggestAdvancedRuleTypes())
                                .executes(context -> executeList(context, plugin))));
    }

    private static int executeList(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleTypeKey = StringArgumentType.getString(context, "rule_type");
        AdvancedRuleType ruleType = AdvancedRuleType.fromKey(ruleTypeKey);
        if (ruleType == null) { sender.sendMessage("§cTipo de regla inválido: " + ruleTypeKey); return 0; }

        AdvancedAreaRules rules = plugin.getAdvancedRulesManager().getRules(areaId);
        Set<String> blocks = rules.getBlocks(ruleType);
        Set<String> entities = rules.getEntities(ruleType);

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lReglas Avanzadas - §e" + areaId);
        sender.sendMessage("§6§lTipo: §e" + ruleType.getKey());
        sender.sendMessage("");
        if (blocks.isEmpty() && entities.isEmpty()) {
            sender.sendMessage("  §7No hay reglas configuradas de este tipo");
        } else {
            if (!blocks.isEmpty()) {
                sender.sendMessage("  §a§lBloques:");
                for (String block : blocks) sender.sendMessage("    §7- §6" + block);
            }
            if (!entities.isEmpty()) {
                sender.sendMessage("");
                sender.sendMessage("  §a§lEntidades:");
                for (String entity : entities) sender.sendMessage("    §7- §6" + entity);
            }
        }
        sender.sendMessage("§e§m                                          ");
        return 1;
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(java.util.Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestAdvancedRuleTypes() {
        return (context, builder) -> {
            for (String key : AdvancedRuleType.getAllKeys()) builder.suggest(key);
            return builder.buildFuture();
        };
    }
}
