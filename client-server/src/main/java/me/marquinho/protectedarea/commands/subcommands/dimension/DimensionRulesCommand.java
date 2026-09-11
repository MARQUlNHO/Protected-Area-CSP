package me.marquinho.protectedarea.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.AreaRule;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Set;

public class DimensionRulesCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("rules")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(CommandManager.argument("rule", StringArgumentType.word())
                                        .suggests(DimensionAreas.suggestAllRules())
                                        .executes(context -> executeAddRule(context, plugin))
                                )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(CommandManager.argument("rule_name", StringArgumentType.word())
                                        .suggests(DimensionAreas.suggestAllRules())
                                        .executes(context -> executeRemoveRule(context, plugin))
                                )
                        )
                )
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeListRules(context, plugin))
                        )
                );
    }

    private static int executeAddRule(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "id");
        String ruleKey = StringArgumentType.getString(context, "rule");

        AreaRule rule = AreaRule.fromKey(ruleKey);
        if (rule == null) {
            Messages.error(source, "protectedarea.command.common.invalid_rule", Messages.arg("rule", ruleKey));
            Messages.send(source, "protectedarea.command.rules.available", Messages.arg("rules", String.join(", ", AreaRule.getAllKeys())));
            return 0;
        }

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().addRuleToArea(a.getId(), rule)) count++;
            final int c = count; final int t = areaList.size();
            Messages.send(source, "protectedarea.command.rules.applied.folder",
                    Messages.arg("rule", rule.getKey()), Messages.arg("count", c), Messages.arg("total", t), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;
        if (area.hasRule(rule)) { Messages.send(source, "protectedarea.command.rules.already_has", Messages.arg("area", areaId), Messages.arg("rule", rule.getKey())); return 0; }

        if (plugin.getAreaManager().addRuleToArea(areaId, rule)) {
            Messages.send(source, "protectedarea.command.rules.added.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.rules.added.rule_with_description", Messages.arg("rule", rule.getKey()), Messages.arg("description", rule.getDescription()));
            return 1;
        }
        Messages.error(source, "protectedarea.command.rules.add.failed");
        return 0;
    }

    private static int executeRemoveRule(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule_name");

        AreaRule rule = AreaRule.fromKey(ruleKey);
        if (rule == null) { Messages.error(source, "protectedarea.command.common.invalid_rule", Messages.arg("rule", ruleKey)); return 0; }

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().removeRuleFromArea(a.getId(), rule)) count++;
            final int c = count; final int t = areaList.size();
            Messages.send(source, "protectedarea.command.rules.removed.folder",
                    Messages.arg("rule", rule.getKey()), Messages.arg("count", c), Messages.arg("total", t), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;
        if (!area.hasRule(rule)) { Messages.send(source, "protectedarea.command.rules.does_not_have", Messages.arg("area", areaId), Messages.arg("rule", rule.getKey())); return 0; }

        if (plugin.getAreaManager().removeRuleFromArea(areaId, rule)) {
            Messages.send(source, "protectedarea.command.rules.removed.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.common.rule_line", Messages.arg("rule", rule.getKey()));
            return 1;
        }
        Messages.error(source, "protectedarea.command.rules.remove.failed");
        return 0;
    }

    private static int executeListRules(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "id");

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        Set<AreaRule> rules = area.getRules();

        Messages.send(source, "protectedarea.command.exception.list.divider");
        Messages.send(source, "protectedarea.command.rules.list.header", Messages.arg("area", areaId));
        Messages.send(source, "protectedarea.command.common.blank_line");

        if (rules.isEmpty()) {
            Messages.send(source, "protectedarea.command.rules.list.empty");
        } else {
            for (AreaRule rule : rules) {
                Messages.send(source, "protectedarea.command.rules.list.entry", Messages.arg("rule", rule.getKey()));
                Messages.send(source, "protectedarea.command.rules.list.description", Messages.arg("description", rule.getDescription()));
            }
        }

        Messages.send(source, "protectedarea.command.exception.list.divider");
        return 1;
    }
}
