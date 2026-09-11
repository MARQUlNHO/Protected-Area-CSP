package me.marquinho.protectedAreaPlugin.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;

public class DimensionRulesCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("rules")
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(Commands.argument("rule", StringArgumentType.word())
                                        .suggests(DimensionAreas.suggestAllRules())
                                        .executes(context -> executeAddRule(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(Commands.argument("rule_name", StringArgumentType.word())
                                        .suggests(DimensionAreas.suggestAllRules())
                                        .executes(context -> executeRemoveRule(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("list")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeListRules(context, plugin))
                        )
                );
    }

    private static int executeAddRule(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "id");
        String ruleKey = StringArgumentType.getString(context, "rule");

        AreaRule rule = AreaRule.fromKey(ruleKey);
        if (rule == null) {
            sender.sendMessage("§cInvalid rule: " + ruleKey);
            sender.sendMessage("§eAvailable rules: " + String.join(", ", AreaRule.getAllKeys()));
            return 0;
        }

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().addRuleToArea(a.getId(), rule)) count++;
            sender.sendMessage("§aRule §6" + rule.getKey() + " §aapplied to §6" + count + "§a/§6" + areaList.size() + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        if (area.hasRule(rule)) {
            sender.sendMessage("§eArea '" + areaId + "' already has the rule: §6" + rule.getKey());
            return 0;
        }

        if (plugin.getAreaManager().addRuleToArea(areaId, rule)) {
            sender.sendMessage("§aRule added successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6" + rule.getKey() + " §7- " + rule.getDescription());
            return 1;
        } else {
            sender.sendMessage("§cError adding the rule");
            return 0;
        }
    }

    private static int executeRemoveRule(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule_name");

        AreaRule rule = AreaRule.fromKey(ruleKey);
        if (rule == null) {
            sender.sendMessage("§cInvalid rule: " + ruleKey);
            return 0;
        }

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().removeRuleFromArea(a.getId(), rule)) count++;
            sender.sendMessage("§aRule §6" + rule.getKey() + " §aremoved from §6" + count + "§a/§6" + areaList.size() + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        if (!area.hasRule(rule)) {
            sender.sendMessage("§eArea '" + areaId + "' does not have the rule: §6" + rule.getKey());
            return 0;
        }

        if (plugin.getAreaManager().removeRuleFromArea(areaId, rule)) {
            sender.sendMessage("§aRule removed successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6" + rule.getKey());
            return 1;
        } else {
            sender.sendMessage("§cError removing the rule");
            return 0;
        }
    }

    private static int executeListRules(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "id");

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        Set<AreaRule> rules = area.getRules();

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lArea Rules: §e" + areaId);
        sender.sendMessage("");

        if (rules.isEmpty()) {
            sender.sendMessage("  §7This area has no rules configured");
        } else {
            for (AreaRule rule : rules) {
                sender.sendMessage("  §a● §6" + rule.getKey());
                sender.sendMessage("    §7" + rule.getDescription());
            }
        }

        sender.sendMessage("§e§m                                          ");
        return 1;
    }
}
