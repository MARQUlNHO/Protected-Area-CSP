package me.marquinho.protectedAreaPlugin.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.commands.subcommands.WandCommand;
import me.marquinho.protectedAreaPlugin.commands.subcommands.cube.*;
import me.marquinho.protectedAreaPlugin.commands.subcommands.config.*;
import me.marquinho.protectedAreaPlugin.commands.subcommands.dimension.*;
import me.marquinho.protectedAreaPlugin.commands.subcommands.flat.FlatLimitCommand;
import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AreaCommand {

    public static void register(ProtectedAreaPlugin plugin, Commands commands) {
        commands.register(
                buildCommand(plugin).build(),
                "Command to manage protected areas",
                java.util.Collections.emptyList()
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("area")
                .requires(source -> source.getSender().hasPermission("protectedarea.admin"))
                .then(createSubcommand(plugin))
                .then(removeSubcommand(plugin))
                .then(reloadSubcommand(plugin))
                .then(viewSubcommand(plugin))
                .then(colorSubcommand(plugin))
                .then(DebugCommand.buildCommand(plugin))
                .then(WandCommand.buildCommand(plugin))
                .then(cubeSubcommand(plugin))
                .then(flatSubcommand(plugin))
                .then(dimensionSubcommand(plugin))
                .then(configSubcommand(plugin));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> cubeSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("cube")
                .then(rulesSubcommand(plugin))
                .then(exceptionSubcommand(plugin))
                .then(SkyboxCommand.buildCommand(plugin))
                .then(PriorityCommand.buildCommand(plugin))
                .then(TeleportCommand.buildCommand(plugin))
                .then(ExecuteCommand.buildCommand(plugin))
                .then(LimitCommand.buildCommand(plugin))
                .then(CommandCommand.buildCommand(plugin))
                .then(AdvancedRulesCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> flatSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("flat")
                .then(FlatLimitCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dimensionSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("dimension")
                .then(DimensionRulesCommand.buildCommand(plugin))
                .then(DimensionExceptionCommand.buildCommand(plugin))
                .then(DimensionAdvancedRulesCommand.buildCommand(plugin))
                .then(DimensionSkyboxCommand.buildCommand(plugin))
                .then(DimensionPriorityCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("config")
                .then(ModRequiredCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> exceptionSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("exception")
                .then(Commands.literal("add")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(Commands.argument("target", ArgumentTypes.players())
                                        .then(Commands.literal("all")
                                                .executes(context -> executeAddExceptionAll(context, plugin))
                                        )
                                        .then(Commands.argument("rule", StringArgumentType.word())
                                                .suggests(suggestAllRulesAndAdvanced())
                                                .executes(context -> executeAddException(context, plugin))
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(Commands.argument("target", ArgumentTypes.players())
                                        .then(Commands.literal("all")
                                                .executes(context -> executeRemoveExceptionAll(context, plugin))
                                        )
                                        .then(Commands.argument("rule", StringArgumentType.word())
                                                .suggests(suggestAllRulesAndAdvanced())
                                                .executes(context -> executeRemoveException(context, plugin))
                                        )
                                )
                        )
                )
                .then(Commands.literal("list")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .executes(context -> executeListExceptions(context, plugin))
                        )
                );
    }

    private static int executeAddExceptionAll(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) throws CommandSyntaxException {
        CommandSender sender = context.getSource().getSender();

        String areaId = StringArgumentType.getString(context, "area_id");
        PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(context.getSource());

        if (targets.isEmpty()) {
            sender.sendMessage("§cNo players found matching that selector");
            return 0;
        }

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            for (ProtectedArea a : list) {
                for (Player target : targets) a.addException("all", target.getName());
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            sender.sendMessage("§aException §6all §aapplied to §6" + list.size() + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

        int count = 0;
        for (Player target : targets) {
            if (area.addException("all", target.getName())) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        if (count > 0) {
            sender.sendMessage("§aException added successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6all");
            sender.sendMessage("§ePlayers: §6" + count);
        } else {
            sender.sendMessage("§eThese exceptions already exist");
        }

        return 1;
    }

    private static int executeAddException(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) throws CommandSyntaxException {
        CommandSender sender = context.getSource().getSender();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule");
        PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(context.getSource());

        if (targets.isEmpty()) {
            sender.sendMessage("§cNo players found matching that selector");
            return 0;
        }

        if (!isValidRule(ruleKey)) {
            sender.sendMessage("§cInvalid rule: " + ruleKey);
            return 0;
        }

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            for (ProtectedArea a : list) {
                for (Player target : targets) a.addException(ruleKey, target.getName());
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            sender.sendMessage("§aException §6" + ruleKey + " §aapplied to §6" + list.size() + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

        int count = 0;
        for (Player target : targets) {
            if (area.addException(ruleKey, target.getName())) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        if (count > 0) {
            sender.sendMessage("§aException added successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6" + ruleKey);
            sender.sendMessage("§ePlayers: §6" + count);
        } else {
            sender.sendMessage("§eThese exceptions already exist");
        }

        return 1;
    }

    private static int executeRemoveExceptionAll(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) throws CommandSyntaxException {
        CommandSender sender = context.getSource().getSender();

        String areaId = StringArgumentType.getString(context, "area_id");
        PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(context.getSource());

        if (targets.isEmpty()) {
            sender.sendMessage("§cNo players found matching that selector");
            return 0;
        }

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            for (ProtectedArea a : list) {
                for (Player target : targets) a.removeException("all", target.getName());
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            sender.sendMessage("§aException §6all §aremoved from §6" + list.size() + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

        int count = 0;
        for (Player target : targets) {
            if (area.removeException("all", target.getName())) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        if (count > 0) {
            sender.sendMessage("§aException removed successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6all");
            sender.sendMessage("§ePlayers: §6" + count);
        } else {
            sender.sendMessage("§eThese exceptions did not exist");
        }

        return 1;
    }

    private static int executeRemoveException(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) throws CommandSyntaxException {
        CommandSender sender = context.getSource().getSender();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule");
        PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(context.getSource());

        if (targets.isEmpty()) {
            sender.sendMessage("§cNo players found matching that selector");
            return 0;
        }

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            for (ProtectedArea a : list) {
                for (Player target : targets) a.removeException(ruleKey, target.getName());
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            sender.sendMessage("§aException §6" + ruleKey + " §aremoved from §6" + list.size() + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

        int count = 0;
        for (Player target : targets) {
            if (area.removeException(ruleKey, target.getName())) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        if (count > 0) {
            sender.sendMessage("§aException removed successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eRule: §6" + ruleKey);
            sender.sendMessage("§ePlayers: §6" + count);
        } else {
            sender.sendMessage("§eThese exceptions did not exist");
        }

        return 1;
    }

    private static int executeListExceptions(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

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

    private static SuggestionProvider<CommandSourceStack> suggestAllRulesAndAdvanced() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) builder.suggest(key);
            builder.suggest("no_break");
            builder.suggest("no_place");
            builder.suggest("no_interact");
            builder.suggest("limit");
            return builder.buildFuture();
        };
    }

    public static boolean isValidRule(String ruleKey) {
        if (AreaRule.fromKey(ruleKey) != null) return true;
        return ruleKey.equalsIgnoreCase("limit");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rulesSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("rules")
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(Commands.argument("rule", StringArgumentType.word())
                                        .suggests(suggestAllRules())
                                        .executes(context -> executeAddRule(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(Commands.argument("rule_name", StringArgumentType.word())
                                        .suggests(suggestAllRules())
                                        .executes(context -> executeRemoveRule(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("list")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
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

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            int count = 0;
            for (ProtectedArea a : list) if (plugin.getAreaManager().addRuleToArea(a.getId(), rule)) count++;
            sender.sendMessage("§aRule §6" + rule.getKey() + " §aapplied to §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

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

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            int count = 0;
            for (ProtectedArea a : list) if (plugin.getAreaManager().removeRuleFromArea(a.getId(), rule)) count++;
            sender.sendMessage("§aRule §6" + rule.getKey() + " §aremoved from §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

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

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

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

    private static SuggestionProvider<CommandSourceStack> suggestAllRules() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) builder.suggest(key);
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestTargetBlockX() {
        return (context, builder) -> {
            if (!(context.getSource().getSender() instanceof Player p)) return builder.buildFuture();
            Block target = p.getTargetBlockExact(100);
            if (target == null) target = p.getLocation().getBlock();
            builder.suggest(String.valueOf(target.getX()));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestTargetBlockY() {
        return (context, builder) -> {
            if (!(context.getSource().getSender() instanceof Player p)) return builder.buildFuture();
            Block target = p.getTargetBlockExact(100);
            if (target == null) target = p.getLocation().getBlock();
            builder.suggest(String.valueOf(target.getY()));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestTargetBlockZ() {
        return (context, builder) -> {
            if (!(context.getSource().getSender() instanceof Player p)) return builder.buildFuture();
            Block target = p.getTargetBlockExact(100);
            if (target == null) target = p.getLocation().getBlock();
            builder.suggest(String.valueOf(target.getZ()));
            return builder.buildFuture();
        };
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("create")
                .then(Commands.literal("cube")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("x1", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                        .then(Commands.argument("y1", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                .then(Commands.argument("z1", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                        .then(Commands.argument("x2", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                                .then(Commands.argument("y2", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                                        .then(Commands.argument("z2", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                                .executes(context -> {
                                                                                    CommandSender sender = context.getSource().getSender();
                                                                                    String id = StringArgumentType.getString(context, "id");
                                                                                    int x1 = IntegerArgumentType.getInteger(context, "x1");
                                                                                    int y1 = IntegerArgumentType.getInteger(context, "y1");
                                                                                    int z1 = IntegerArgumentType.getInteger(context, "z1");
                                                                                    int x2 = IntegerArgumentType.getInteger(context, "x2");
                                                                                    int y2 = IntegerArgumentType.getInteger(context, "y2");
                                                                                    int z2 = IntegerArgumentType.getInteger(context, "z2");

                                                                                    Player player = (sender instanceof Player) ? (Player) sender : null;
                                                                                    org.bukkit.World world = (player != null) ? player.getWorld() : plugin.getServer().getWorlds().get(0);
                                                                                    String dimension = world.getKey().toString();

                                                                                    if (plugin.getAreaManager().createArea(id, dimension, x1, y1, z1, x2, y2, z2)) {
                                                                                        sender.sendMessage("§aArea '" + id + "' created successfully!");
                                                                                        sender.sendMessage("§eDimension: §6" + dimension);
                                                                                        ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
                                                                                        plugin.getAreaManager().broadcastNewArea(area);
                                                                                        return 1;
                                                                                    } else {
                                                                                        sender.sendMessage("§cAn area with ID already exists: " + id);
                                                                                        return 0;
                                                                                    }
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("flat")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("flatPosition", IntegerArgumentType.integer())
                                        .then(Commands.argument("x1", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                .then(Commands.argument("y1", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                        .then(Commands.argument("z1", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                .then(Commands.argument("x2", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                                        .then(Commands.argument("y2", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                                                .then(Commands.argument("z2", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                                        .executes(context -> {
                                                                                            CommandSender sender = context.getSource().getSender();
                                                                                            String id = StringArgumentType.getString(context, "id");
                                                                                            int flatPosition = IntegerArgumentType.getInteger(context, "flatPosition");
                                                                                            int x1 = IntegerArgumentType.getInteger(context, "x1");
                                                                                            int y1 = IntegerArgumentType.getInteger(context, "y1");
                                                                                            int z1 = IntegerArgumentType.getInteger(context, "z1");
                                                                                            int x2 = IntegerArgumentType.getInteger(context, "x2");
                                                                                            int y2 = IntegerArgumentType.getInteger(context, "y2");
                                                                                            int z2 = IntegerArgumentType.getInteger(context, "z2");

                                                                                            Player player = (sender instanceof Player) ? (Player) sender : null;
                                                                                            World world = (player != null) ? player.getWorld() : plugin.getServer().getWorlds().get(0);
                                                                                            String dimension = world.getKey().toString();

                                                                                            if (plugin.getAreaManager().createArea(id, dimension, x1, y1, z1, x2, y2, z2, "flat", flatPosition)) {
                                                                                                sender.sendMessage("§aFlat area '" + id + "' created successfully!");
                                                                                                sender.sendMessage("§eDimension: §6" + dimension);
                                                                                                sender.sendMessage("§ePosition: §6" + flatPosition);
                                                                                                ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
                                                                                                plugin.getAreaManager().broadcastNewArea(area);
                                                                                                return 1;
                                                                                            } else {
                                                                                                sender.sendMessage("§cAn area with ID already exists: " + id);
                                                                                                return 0;
                                                                                            }
                                                                                        })
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

    private static LiteralArgumentBuilder<CommandSourceStack> removeSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("remove")
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(suggestAreaIds(plugin))
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            String id = StringArgumentType.getString(context, "id");

                            if (id.endsWith("/")) {
                                List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(id);
                                if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + id); return 0; }
                                int count = 0;
                                for (ProtectedArea a : list) {
                                    if (plugin.getAreaManager().removeArea(a.getId())) {
                                        plugin.getAreaManager().broadcastRemoveArea(a.getId());
                                        count++;
                                    }
                                }
                                java.io.File areasRoot = new java.io.File(plugin.getDataFolder(), "Areas");
                                String folderPath = id.substring(0, id.length() - 1).replace("/", java.io.File.separator);
                                deleteFolder(new java.io.File(areasRoot, folderPath));
                                deleteFolder(new java.io.File(new java.io.File(areasRoot, ".flat"), folderPath));
                                sender.sendMessage("§a" + count + " area(s) removed from folder: §6" + id);
                                return 1;
                            }

                            if (plugin.getAreaManager().removeArea(id)) {
                                sender.sendMessage("§aArea '" + id + "' removed successfully!");
                                plugin.getAreaManager().broadcastRemoveArea(id);
                                return 1;
                            } else {
                                sender.sendMessage("§cNo area exists with ID: " + id);
                                return 0;
                            }
                        })
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> reloadSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("reload")
                .then(Commands.literal("notifications")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            sender.sendMessage("§eReloading notification configurations...");
                            plugin.getNotificationManager().reloadConfigs();
                            sender.sendMessage("§aNotifications reloaded successfully!");
                            return 1;
                        }))
                .then(Commands.literal("config")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            sender.sendMessage("§eReloading plugin configuration...");
                            plugin.getConfigManager().reloadConfig();
                            sender.sendMessage("§aConfiguration reloaded successfully!");

                            boolean modRequired = plugin.getConfigManager().isModRequired();
                            String status = modRequired ? "§a§lENABLED" : "§c§lDISABLED";
                            sender.sendMessage("§eMandatory mod status: " + status);

                            return 1;
                        }))
                .then(Commands.literal("cube")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .executes(context -> executeReloadArea(context, plugin, "cube"))
                        ))
                .then(Commands.literal("flat")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(suggestFlatAreaIds(plugin))
                                .executes(context -> executeReloadArea(context, plugin, "flat"))
                        ))
                .then(Commands.literal("dimension")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeReloadArea(context, plugin, "dimension"))
                        ))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    sender.sendMessage("§eReloading areas...");
                    plugin.getAreaManager().reloadAreas();
                    sender.sendMessage("§aAreas reloaded successfully! Total: " +
                            plugin.getAreaManager().getAreas().size());
                    return 1;
                });
    }

    private static int executeReloadArea(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin, String type) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        if (!plugin.getAreaManager().reloadArea(areaId, type)) {
            sender.sendMessage("§cArea file not found " + type + ": " + areaId);
            return 0;
        }

        sender.sendMessage("§aArea reloaded! §e" + type + " §6" + areaId);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> viewSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("view")
                .then(Commands.argument("target", ArgumentTypes.players())
                        .then(Commands.argument("enable", BoolArgumentType.bool())
                                .executes(context -> {
                                    CommandSender sender = context.getSource().getSender();
                                    boolean enable = BoolArgumentType.getBool(context, "enable");

                                    PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
                                    List<Player> targets = resolver.resolve(context.getSource());

                                    if (targets.isEmpty()) {
                                        sender.sendMessage("§cNo players found matching that selector");
                                        return 0;
                                    }

                                    int count = 0;
                                    for (Player t : targets) {
                                        plugin.getAreaManager().sendViewToggle(t, enable);
                                        count++;
                                    }

                                    String status = enable ? "§aenabled" : "§cdisabled";
                                    Player p = (sender instanceof Player) ? (Player) sender : null;
                                    if (count == 1) {
                                        Player target = targets.get(0);
                                        if (p != null && target.equals(p)) {
                                            sender.sendMessage("§eArea visibility " + status);
                                        } else {
                                            sender.sendMessage("§eArea visibility " + status + " for §6" + target.getName());
                                        }
                                    } else {
                                        sender.sendMessage("§eArea visibility " + status + " for §6" + count + " player(s)");
                                    }

                                    return 1;
                                })
                        )
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> colorSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("color")
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(suggestAreaIds(plugin))
                        .then(Commands.argument("hexcolor", StringArgumentType.word())
                                .executes(context -> executeColorCommand(context, plugin, ""))
                                .then(Commands.argument("alias", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String alias = StringArgumentType.getString(context, "alias");
                                            return executeColorCommand(context, plugin, alias);
                                        })
                                )
                        )
                );
    }

    private static int executeColorCommand(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin, String alias) {
        CommandSender sender = context.getSource().getSender();

        String providedId = StringArgumentType.getString(context, "id");
        String rawColor = StringArgumentType.getString(context, "hexcolor");

        if (!rawColor.startsWith("#")) rawColor = "#" + rawColor;

        if (!rawColor.matches("^#[0-9A-Fa-f]{6}$")) {
            sender.sendMessage("§cThe color must be in hex format (#RRGGBB) - e.g: #FF0000 or FF0000");
            return 0;
        }

        String normalizedColor = rawColor.toUpperCase();

        if (providedId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(providedId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + providedId); return 0; }
            int count = 0;
            for (ProtectedArea a : list) {
                if (plugin.getAreaManager().setAreaColor(a.getId(), normalizedColor, alias)) {
                    plugin.getAreaManager().broadcastUpdateArea(plugin.getAreaManager().getAreas().get(a.getId()));
                    count++;
                }
            }
            sender.sendMessage("§aColor §6" + normalizedColor + " §aapplied to §6" + count + " §aarea(s) in §6" + providedId);
            return 1;
        }

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
            sender.sendMessage("§cNo area exists with ID: " + providedId);
            return 0;
        }

        if (plugin.getAreaManager().setAreaColor(matchedId, normalizedColor, alias)) {
            sender.sendMessage("§aColor of area '" + matchedId + "' updated to " + normalizedColor);
            if (alias != null && !alias.isEmpty()) sender.sendMessage("§aAlias: " + alias);
            ProtectedArea updated = plugin.getAreaManager().getAreas().get(matchedId);
            if (updated != null) plugin.getAreaManager().broadcastUpdateArea(updated);
            return 1;
        } else {
            sender.sendMessage("§cCould not update the color of area: " + matchedId);
            return 0;
        }
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isDimension())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestCubeAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isCube())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestFlatAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isFlat())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }

    private static void deleteFolder(java.io.File folder) {
        if (!folder.exists() || !folder.isDirectory()) return;
        java.io.File[] files = folder.listFiles();
        if (files != null) for (java.io.File f : files) deleteFolder(f);
        folder.delete();
    }
}
