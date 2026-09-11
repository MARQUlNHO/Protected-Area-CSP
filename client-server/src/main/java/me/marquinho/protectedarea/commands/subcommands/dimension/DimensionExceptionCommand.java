package me.marquinho.protectedarea.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.commands.AreaCommand;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DimensionExceptionCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("exception")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.literal("all")
                                                .executes(context -> executeException(context, plugin, "all", true))
                                        )
                                        .then(CommandManager.argument("rule", StringArgumentType.word())
                                                .suggests(DimensionAreas.suggestAllRulesAndAdvanced())
                                                .executes(context -> executeException(context, plugin, null, true))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.literal("all")
                                                .executes(context -> executeException(context, plugin, "all", false))
                                        )
                                        .then(CommandManager.argument("rule", StringArgumentType.word())
                                                .suggests(DimensionAreas.suggestAllRulesAndAdvanced())
                                                .executes(context -> executeException(context, plugin, null, false))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeListExceptions(context, plugin))
                        )
                );
    }

    private static int executeException(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin,
                                        String fixedRule, boolean add) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = fixedRule != null ? fixedRule : StringArgumentType.getString(context, "rule");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_players_found"); return 0; }
        if (fixedRule == null && !AreaCommand.isValidRule(ruleKey)) { Messages.error(source, "protectedarea.command.common.invalid_rule", Messages.arg("rule", ruleKey)); return 0; }

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            for (ProtectedArea a : areaList) {
                for (ServerPlayerEntity t : targets) {
                    String name = t.getGameProfile().getName();
                    if (add) a.addException(ruleKey, name); else a.removeException(ruleKey, name);
                }
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            final int total = areaList.size();
            final String fRuleKey = ruleKey;
            Messages.send(source, add
                    ? "protectedarea.command.exception.rule.applied.folder"
                    : "protectedarea.command.exception.rule.removed.folder",
                    Messages.arg("rule", fRuleKey), Messages.arg("count", total), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        int count = 0;
        for (ServerPlayerEntity target : targets) {
            String name = target.getGameProfile().getName();
            boolean changed = add ? area.addException(ruleKey, name) : area.removeException(ruleKey, name);
            if (changed) count++;
        }
        plugin.getAreaManager().saveAreaManually(area);

        final int finalCount = count;
        final String fRuleKey = ruleKey;
        if (count > 0) {
            Messages.send(source, add
                    ? "protectedarea.command.exception.added.success"
                    : "protectedarea.command.exception.removed.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.common.rule_line", Messages.arg("rule", fRuleKey));
            Messages.send(source, "protectedarea.command.common.players_line", Messages.arg("count", finalCount));
        } else {
            Messages.send(source, add
                    ? "protectedarea.command.exception.already_exists"
                    : "protectedarea.command.exception.did_not_exist");
        }
        return 1;
    }

    private static int executeListExceptions(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        Map<String, Set<String>> exceptions = area.getAllExceptions();

        Messages.send(source, "protectedarea.command.exception.list.divider");
        Messages.send(source, "protectedarea.command.exception.list.header", Messages.arg("area", areaId));
        Messages.send(source, "protectedarea.command.common.blank_line");

        if (exceptions.isEmpty()) {
            Messages.send(source, "protectedarea.command.exception.list.empty");
        } else {
            for (Map.Entry<String, Set<String>> entry : exceptions.entrySet()) {
                Messages.send(source, "protectedarea.command.exception.list.category", Messages.arg("category", entry.getKey().toUpperCase()));
                for (String playerName : entry.getValue()) {
                    Messages.send(source, "protectedarea.command.exception.list.player", Messages.arg("player", playerName));
                }
            }
        }

        Messages.send(source, "protectedarea.command.exception.list.divider");
        return 1;
    }
}
