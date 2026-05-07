package me.marquinho.protectedAreaPlugin.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.commands.subcommands.cube.*;
import me.marquinho.protectedAreaPlugin.commands.subcommands.config.*;
import me.marquinho.protectedAreaPlugin.commands.subcommands.flat.FlatLimitCommand;
import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
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
                "Comando para gestionar áreas protegidas",
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
                .then(cubeSubcommand(plugin))
                .then(flatSubcommand(plugin))
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

    private static LiteralArgumentBuilder<CommandSourceStack> configSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("config")
                .then(ModRequiredCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> exceptionSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("exception")
                .then(Commands.literal("add")
                        .then(Commands.argument("area_id", StringArgumentType.word())
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
                        .then(Commands.argument("area_id", StringArgumentType.word())
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
                        .then(Commands.argument("area_id", StringArgumentType.word())
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
            sender.sendMessage("§cNo se encontraron jugadores con ese selector");
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        int count = 0;
        for (Player target : targets) {
            if (area.addException("all", target.getName())) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        if (count > 0) {
            sender.sendMessage("§a¡Excepción agregada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6all");
            sender.sendMessage("§eJugadores: §6" + count);
        } else {
            sender.sendMessage("§eEstas excepciones ya existen");
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
            sender.sendMessage("§cNo se encontraron jugadores con ese selector");
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        if (!isValidRule(ruleKey)) {
            sender.sendMessage("§cRegla inválida: " + ruleKey);
            return 0;
        }

        int count = 0;
        for (Player target : targets) {
            if (area.addException(ruleKey, target.getName())) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        if (count > 0) {
            sender.sendMessage("§a¡Excepción agregada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6" + ruleKey);
            sender.sendMessage("§eJugadores: §6" + count);
        } else {
            sender.sendMessage("§eEstas excepciones ya existen");
        }

        return 1;
    }

    private static int executeRemoveExceptionAll(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) throws CommandSyntaxException {
        CommandSender sender = context.getSource().getSender();

        String areaId = StringArgumentType.getString(context, "area_id");
        PlayerSelectorArgumentResolver resolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        List<Player> targets = resolver.resolve(context.getSource());

        if (targets.isEmpty()) {
            sender.sendMessage("§cNo se encontraron jugadores con ese selector");
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        int count = 0;
        for (Player target : targets) {
            if (area.removeException("all", target.getName())) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        if (count > 0) {
            sender.sendMessage("§a¡Excepción eliminada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6all");
            sender.sendMessage("§eJugadores: §6" + count);
        } else {
            sender.sendMessage("§eEstas excepciones no existían");
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
            sender.sendMessage("§cNo se encontraron jugadores con ese selector");
            return 0;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        int count = 0;
        for (Player target : targets) {
            if (area.removeException(ruleKey, target.getName())) count++;
        }

        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        if (count > 0) {
            sender.sendMessage("§a¡Excepción eliminada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6" + ruleKey);
            sender.sendMessage("§eJugadores: §6" + count);
        } else {
            sender.sendMessage("§eEstas excepciones no existían");
        }

        return 1;
    }

    private static int executeListExceptions(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        Map<String, Set<String>> exceptions = area.getAllExceptions();

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lExcepciones del Área: §e" + areaId);
        sender.sendMessage("");

        if (exceptions.isEmpty()) {
            sender.sendMessage("  §7Esta área no tiene excepciones configuradas");
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

    private static boolean isValidRule(String ruleKey) {
        if (AreaRule.fromKey(ruleKey) != null) return true;
        return ruleKey.equalsIgnoreCase("no_break") ||
                ruleKey.equalsIgnoreCase("no_place") ||
                ruleKey.equalsIgnoreCase("no_interact") ||
                ruleKey.equalsIgnoreCase("limit");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rulesSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("rules")
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(Commands.argument("rule", StringArgumentType.word())
                                        .suggests(suggestAllRules())
                                        .executes(context -> executeAddRule(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(Commands.argument("rule_name", StringArgumentType.word())
                                        .suggests(suggestAllRules())
                                        .executes(context -> executeRemoveRule(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("list")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(suggestCubeAreaIds(plugin))
                                .executes(context -> executeListRules(context, plugin))
                        )
                );
    }

    private static int executeAddRule(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "id");
        String ruleKey = StringArgumentType.getString(context, "rule");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        AreaRule rule = AreaRule.fromKey(ruleKey);
        if (rule == null) {
            sender.sendMessage("§cRegla inválida: " + ruleKey);
            sender.sendMessage("§eReglas disponibles: " + String.join(", ", AreaRule.getAllKeys()));
            return 0;
        }

        if (area.hasRule(rule)) {
            sender.sendMessage("§eEl área '" + areaId + "' ya tiene la regla: §6" + rule.getKey());
            return 0;
        }

        if (plugin.getAreaManager().addRuleToArea(areaId, rule)) {
            sender.sendMessage("§a¡Regla agregada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6" + rule.getKey() + " §7- " + rule.getDescription());
            return 1;
        } else {
            sender.sendMessage("§cError al agregar la regla");
            return 0;
        }
    }

    private static int executeRemoveRule(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule_name");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        AreaRule rule = AreaRule.fromKey(ruleKey);
        if (rule == null) {
            sender.sendMessage("§cRegla inválida: " + ruleKey);
            return 0;
        }

        if (!area.hasRule(rule)) {
            sender.sendMessage("§eEl área '" + areaId + "' no tiene la regla: §6" + rule.getKey());
            return 0;
        }

        if (plugin.getAreaManager().removeRuleFromArea(areaId, rule)) {
            sender.sendMessage("§a¡Regla eliminada exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eRegla: §6" + rule.getKey());
            return 1;
        } else {
            sender.sendMessage("§cError al eliminar la regla");
            return 0;
        }
    }

    private static int executeListRules(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        Set<AreaRule> rules = area.getRules();

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lReglas del Área: §e" + areaId);
        sender.sendMessage("");

        if (rules.isEmpty()) {
            sender.sendMessage("  §7Esta área no tiene reglas configuradas");
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
                        .then(Commands.argument("id", StringArgumentType.word())
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
                                                                                    String worldName = (player != null) ? player.getWorld().getName() : plugin.getServer().getWorlds().get(0).getName();
                                                                                    String dimension = getDimensionKey(worldName);

                                                                                    if (plugin.getAreaManager().createArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2)) {
                                                                                        sender.sendMessage("§a¡Área '" + id + "' creada exitosamente!");
                                                                                        sender.sendMessage("§eMundo: §6" + worldName);
                                                                                        sender.sendMessage("§eDimensión: §6" + dimension);
                                                                                        ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
                                                                                        plugin.getAreaManager().broadcastNewArea(area);
                                                                                        return 1;
                                                                                    } else {
                                                                                        sender.sendMessage("§cYa existe un área con el ID: " + id);
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
                        .then(Commands.argument("id", StringArgumentType.word())
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
                                                                                            String worldName = (player != null) ? player.getWorld().getName() : plugin.getServer().getWorlds().get(0).getName();
                                                                                            String dimension = getDimensionKey(worldName);

                                                                                            if (plugin.getAreaManager().createArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2, "flat", flatPosition)) {
                                                                                                sender.sendMessage("§a¡Área plana '" + id + "' creada exitosamente!");
                                                                                                sender.sendMessage("§eMundo: §6" + worldName);
                                                                                                sender.sendMessage("§ePosición: §6" + flatPosition);
                                                                                                ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
                                                                                                plugin.getAreaManager().broadcastNewArea(area);
                                                                                                return 1;
                                                                                            } else {
                                                                                                sender.sendMessage("§cYa existe un área con el ID: " + id);
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
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            String id = StringArgumentType.getString(context, "id");

                            if (plugin.getAreaManager().removeArea(id)) {
                                sender.sendMessage("§a¡Área '" + id + "' eliminada exitosamente!");
                                plugin.getAreaManager().broadcastRemoveArea(id);
                                return 1;
                            } else {
                                sender.sendMessage("§cNo existe un área con el ID: " + id);
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
                            sender.sendMessage("§eRecargando configuraciones de notificaciones...");
                            plugin.getNotificationManager().reloadConfigs();
                            sender.sendMessage("§a¡Notificaciones recargadas exitosamente!");
                            return 1;
                        }))
                .then(Commands.literal("config")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            sender.sendMessage("§eRecargando configuración del plugin...");
                            plugin.getConfigManager().reloadConfig();
                            sender.sendMessage("§a¡Configuración recargada exitosamente!");

                            boolean modRequired = plugin.getConfigManager().isModRequired();
                            String status = modRequired ? "§a§lACTIVADO" : "§c§lDESACTIVADO";
                            sender.sendMessage("§eEstado del mod obligatorio: " + status);

                            return 1;
                        }))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    sender.sendMessage("§eRecargando áreas...");
                    plugin.getAreaManager().reloadAreas();
                    sender.sendMessage("§a¡Áreas recargadas exitosamente! Total: " +
                            plugin.getAreaManager().getAreas().size());
                    return 1;
                });
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
                                        sender.sendMessage("§cNo se encontraron jugadores con ese selector");
                                        return 0;
                                    }

                                    int count = 0;
                                    for (Player t : targets) {
                                        plugin.getAreaManager().sendViewToggle(t, enable);
                                        count++;
                                    }

                                    String status = enable ? "§aactivada" : "§cdesactivada";
                                    Player p = (sender instanceof Player) ? (Player) sender : null;
                                    if (count == 1) {
                                        Player target = targets.get(0);
                                        if (p != null && target.equals(p)) {
                                            sender.sendMessage("§eVisualización de áreas " + status);
                                        } else {
                                            sender.sendMessage("§eVisualización de áreas " + status + " para §6" + target.getName());
                                        }
                                    } else {
                                        sender.sendMessage("§eVisualización de áreas " + status + " para §6" + count + " jugador(es)");
                                    }

                                    return 1;
                                })
                        )
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> colorSubcommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("color")
                .then(Commands.argument("id", StringArgumentType.word())
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
            sender.sendMessage("§cEl color debe estar en formato hexadecimal (#RRGGBB) – ej: #FF0000 o FF0000");
            return 0;
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
            sender.sendMessage("§cNo existe un área con el ID: " + providedId);
            return 0;
        }

        String normalizedColor = rawColor.toUpperCase();
        if (plugin.getAreaManager().setAreaColor(matchedId, normalizedColor, alias)) {
            sender.sendMessage("§aColor del área '" + matchedId + "' actualizado a " + normalizedColor);
            if (alias != null && !alias.isEmpty()) sender.sendMessage("§aAlias: " + alias);
            ProtectedArea updated = plugin.getAreaManager().getAreas().get(matchedId);
            if (updated != null) plugin.getAreaManager().broadcastUpdateArea(updated);
            return 1;
        } else {
            sender.sendMessage("§cNo se pudo actualizar el color del área: " + matchedId);
            return 0;
        }
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().keySet().forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> suggestCubeAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static String getDimensionKey(String worldName) {
        if (worldName.contains("nether")) return "minecraft:the_nether";
        if (worldName.contains("end")) return "minecraft:the_end";
        return "minecraft:overworld";
    }
}
