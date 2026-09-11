package me.marquinho.protectedAreaPlugin.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.commands.AreaCommand;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DimensionExceptionCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("exception")
                .then(Commands.literal("add")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(Commands.argument("target", ArgumentTypes.players())
                                        .then(Commands.literal("all")
                                                .executes(context -> executeException(context, plugin, "all", true))
                                        )
                                        .then(Commands.argument("rule", StringArgumentType.word())
                                                .suggests(DimensionAreas.suggestAllRulesAndAdvanced())
                                                .executes(context -> executeException(context, plugin, null, true))
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(Commands.argument("target", ArgumentTypes.players())
                                        .then(Commands.literal("all")
                                                .executes(context -> executeException(context, plugin, "all", false))
                                        )
                                        .then(Commands.argument("rule", StringArgumentType.word())
                                                .suggests(DimensionAreas.suggestAllRulesAndAdvanced())
                                                .executes(context -> executeException(context, plugin, null, false))
                                        )
                                )
                        )
                )
                .then(Commands.literal("list")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeListExceptions(context, plugin))
                        )
                );
    }

    private static int executeException(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin,
                                        String fixedRule, boolean add) throws CommandSyntaxException {
        CommandSender sender = context.getSource().getSender();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = fixedRule != null ? fixedRule : StringArgumentType.getString(context, "rule");
        PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(context.getSource());

        if (targets.isEmpty()) {
            sender.sendMessage("§cNo players found matching that selector");
            return 0;
        }

        if (fixedRule == null && !AreaCommand.isValidRule(ruleKey)) {
            sender.sendMessage("§cInvalid rule: " + ruleKey);
            return 0;
        }

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            for (ProtectedArea a : areaList) {
                for (Player t : targets) {
                    if (add) a.addException(ruleKey, t.getName()); else a.removeException(ruleKey, t.getName());
                }
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            sender.sendMessage(add
                    ? "§aException §6" + ruleKey + " §aapplied to §6" + areaList.size() + " §aarea(s) in §6" + areaId
                    : "§aException §6" + ruleKey + " §aremoved from §6" + areaList.size() + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        int count = 0;
        for (Player target : targets) {
            boolean changed = add
                    ? area.addException(ruleKey, target.getName())
                    : area.removeException(ruleKey, target.getName());
            if (changed) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);

        if (count > 0) {
            sender.sendMessage(add ? "§aException added successfully!" : "§aException removed successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6" + ruleKey);
            sender.sendMessage("§ePlayers: §6" + count);
        } else {
            sender.sendMessage(add ? "§eThese exceptions already exist" : "§eThese exceptions did not exist");
        }

        return 1;
    }

    private static int executeListExceptions(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        Map<String, Set<String>> exceptions = area.getAllExceptions();

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lArea Exceptions: §e" + areaId);
        sender.sendMessage("");

        if (exceptions.isEmpty()) {
            sender.sendMessage("  §7This area has no exceptions configured");
        } else {
            for (Map.Entry<String, Set<String>> entry : exceptions.entrySet()) {
                sender.sendMessage("  §a● §6" + entry.getKey().toUpperCase());
                for (String playerName : entry.getValue()) {
                    sender.sendMessage("    §7- §e" + playerName);
                }
            }
        }

        sender.sendMessage("§e§m                                          ");
        return 1;
    }
}
