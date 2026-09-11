package me.marquinho.protectedarea.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.commands.subcommands.DebugCommand;
import me.marquinho.protectedarea.commands.subcommands.WandCommand;
import me.marquinho.protectedarea.commands.subcommands.config.ModRequiredCommand;
import me.marquinho.protectedarea.commands.subcommands.cube.*;
import me.marquinho.protectedarea.commands.subcommands.dimension.*;
import me.marquinho.protectedarea.commands.subcommands.flat.FlatLimitCommand;
import me.marquinho.protectedarea.models.AreaRule;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.WorldDimensionUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AreaCommand {

    public static void register(ProtectedAreaInit plugin, CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess) {
        dispatcher.register(buildCommand(plugin, registryAccess));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin,
                                                                            CommandRegistryAccess registryAccess) {
        return CommandManager.literal("area")
                .requires(source -> source.hasPermissionLevel(4))
                .then(createSubcommand(plugin))
                .then(removeSubcommand(plugin))
                .then(reloadSubcommand(plugin))
                .then(viewSubcommand(plugin))
                .then(colorSubcommand(plugin))
                .then(DebugCommand.buildCommand(plugin))
                .then(WandCommand.buildCommand(plugin))
                .then(cubeSubcommand(plugin, registryAccess))
                .then(flatSubcommand(plugin))
                .then(dimensionSubcommand(plugin, registryAccess))
                .then(configSubcommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> cubeSubcommand(ProtectedAreaInit plugin,
                                                                               CommandRegistryAccess registryAccess) {
        return CommandManager.literal("cube")
                .then(rulesSubcommand(plugin))
                .then(AdvancedRulesCommand.buildCommand(plugin, registryAccess))
                .then(exceptionSubcommand(plugin))
                .then(SkyboxCommand.buildCommand(plugin))
                .then(PriorityCommand.buildCommand(plugin))
                .then(TeleportCommand.buildCommand(plugin))
                .then(ExecuteCommand.buildCommand(plugin))
                .then(LimitCommand.buildCommand(plugin))
                .then(CommandCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> flatSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("flat")
                .then(FlatLimitCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> dimensionSubcommand(ProtectedAreaInit plugin,
                                                                                    CommandRegistryAccess registryAccess) {
        return CommandManager.literal("dimension")
                .then(DimensionRulesCommand.buildCommand(plugin))
                .then(DimensionExceptionCommand.buildCommand(plugin))
                .then(DimensionAdvancedRulesCommand.buildCommand(plugin, registryAccess))
                .then(DimensionSkyboxCommand.buildCommand(plugin))
                .then(DimensionPriorityCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> configSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("config")
                .then(ModRequiredCommand.buildCommand(plugin));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> exceptionSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("exception")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.literal("all")
                                                .executes(context -> executeAddExceptionAll(context, plugin))
                                        )
                                        .then(CommandManager.argument("rule", StringArgumentType.word())
                                                .suggests(suggestAllRulesAndAdvanced())
                                                .executes(context -> executeAddException(context, plugin))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.literal("all")
                                                .executes(context -> executeRemoveExceptionAll(context, plugin))
                                        )
                                        .then(CommandManager.argument("rule", StringArgumentType.word())
                                                .suggests(suggestAllRulesAndAdvanced())
                                                .executes(context -> executeRemoveException(context, plugin))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .executes(context -> executeListExceptions(context, plugin))
                        )
                );
    }

    private static int executeAddExceptionAll(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_players_found"); return 0; }

        if (areaId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(areaId);
            if (areaList.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            for (ProtectedArea a : areaList) {
                for (ServerPlayerEntity t : targets) a.addException("all", t.getGameProfile().getName());
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            Messages.send(source, "protectedarea.command.exception.all.applied",
                    Messages.arg("count", areaList.size()),
                    Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }

        int count = 0;
        for (ServerPlayerEntity target : targets) if (area.addException("all", target.getGameProfile().getName())) count++;
        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        final int finalCount = count;
        if (count > 0) {
            Messages.send(source, "protectedarea.command.exception.added.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.common.rule_line", Messages.arg("rule", "all"));
            Messages.send(source, "protectedarea.command.common.players_line", Messages.arg("count", finalCount));
        } else {
            Messages.send(source, "protectedarea.command.exception.already_exists");
        }
        return 1;
    }

    private static int executeAddException(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_players_found"); return 0; }
        if (!isValidRule(ruleKey)) { Messages.error(source, "protectedarea.command.common.invalid_rule", Messages.arg("rule", ruleKey)); return 0; }

        if (areaId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(areaId);
            if (areaList.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            for (ProtectedArea a : areaList) {
                for (ServerPlayerEntity t : targets) a.addException(ruleKey, t.getGameProfile().getName());
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            final String fRuleKey = ruleKey;
            Messages.send(source, "protectedarea.command.exception.rule.applied.folder",
                    Messages.arg("rule", fRuleKey),
                    Messages.arg("count", areaList.size()),
                    Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }

        int count = 0;
        for (ServerPlayerEntity target : targets) if (area.addException(ruleKey, target.getGameProfile().getName())) count++;
        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        final int finalCount = count;
        final String fRuleKey = ruleKey;
        if (count > 0) {
            Messages.send(source, "protectedarea.command.exception.added.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.common.rule_line", Messages.arg("rule", fRuleKey));
            Messages.send(source, "protectedarea.command.common.players_line", Messages.arg("count", finalCount));
        } else {
            Messages.send(source, "protectedarea.command.exception.already_exists");
        }
        return 1;
    }

    private static int executeRemoveExceptionAll(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_players_found"); return 0; }

        if (areaId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(areaId);
            if (areaList.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            for (ProtectedArea a : areaList) {
                for (ServerPlayerEntity t : targets) a.removeException("all", t.getGameProfile().getName());
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            Messages.send(source, "protectedarea.command.exception.all.removed.folder",
                    Messages.arg("count", areaList.size()),
                    Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }

        int count = 0;
        for (ServerPlayerEntity target : targets) if (area.removeException("all", target.getGameProfile().getName())) count++;
        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        final int finalCount = count;
        if (count > 0) {
            Messages.send(source, "protectedarea.command.exception.removed.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.common.rule_line", Messages.arg("rule", "all"));
            Messages.send(source, "protectedarea.command.common.players_line", Messages.arg("count", finalCount));
        } else {
            Messages.send(source, "protectedarea.command.exception.did_not_exist");
        }
        return 1;
    }

    private static int executeRemoveException(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");
        String ruleKey = StringArgumentType.getString(context, "rule");
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        if (targets.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_players_found"); return 0; }

        if (areaId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(areaId);
            if (areaList.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            for (ProtectedArea a : areaList) {
                for (ServerPlayerEntity t : targets) a.removeException(ruleKey, t.getGameProfile().getName());
                plugin.getAreaManager().saveAreaManually(a);
                plugin.getAreaManager().broadcastUpdateArea(a);
            }
            final String fRuleKey = ruleKey;
            Messages.send(source, "protectedarea.command.exception.rule.removed.folder",
                    Messages.arg("rule", fRuleKey),
                    Messages.arg("count", areaList.size()),
                    Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }

        int count = 0;
        for (ServerPlayerEntity target : targets) if (area.removeException(ruleKey, target.getGameProfile().getName())) count++;
        plugin.getAreaManager().saveAreaManually(area);
        plugin.getAreaManager().broadcastUpdateArea(area);

        final int finalCount = count;
        final String fRuleKey = ruleKey;
        if (count > 0) {
            Messages.send(source, "protectedarea.command.exception.removed.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.common.rule_line", Messages.arg("rule", fRuleKey));
            Messages.send(source, "protectedarea.command.common.players_line", Messages.arg("count", finalCount));
        } else {
            Messages.send(source, "protectedarea.command.exception.did_not_exist");
        }
        return 1;
    }

    private static int executeListExceptions(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();

        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId));
            return 0;
        }

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

    private static SuggestionProvider<ServerCommandSource> suggestAllRulesAndAdvanced() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) {
                builder.suggest(key);
            }
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

    private static LiteralArgumentBuilder<ServerCommandSource> rulesSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("rules")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(CommandManager.argument("rule", StringArgumentType.word())
                                        .suggests(suggestAllRules())
                                        .executes(context -> executeAddRule(context, plugin))
                                )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .then(CommandManager.argument("rule_name", StringArgumentType.word())
                                        .suggests(suggestAllRules())
                                        .executes(context -> executeRemoveRule(context, plugin))
                                )
                        )
                )
                .then(CommandManager.literal("list")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
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

        if (areaId.endsWith("/")) {
            List<ProtectedArea> targets = plugin.getAreaManager().getAreasByFolder(areaId);
            if (targets.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            int count = 0;
            for (ProtectedArea a : targets) if (plugin.getAreaManager().addRuleToArea(a.getId(), rule)) count++;
            final int c = count; final int t = targets.size();
            Messages.send(source, "protectedarea.command.rules.applied.folder",
                    Messages.arg("rule", rule.getKey()),
                    Messages.arg("count", c),
                    Messages.arg("total", t),
                    Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
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

        if (areaId.endsWith("/")) {
            List<ProtectedArea> targets = plugin.getAreaManager().getAreasByFolder(areaId);
            if (targets.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", areaId)); return 0; }
            int count = 0;
            for (ProtectedArea a : targets) if (plugin.getAreaManager().removeRuleFromArea(a.getId(), rule)) count++;
            final int c = count; final int t = targets.size();
            Messages.send(source, "protectedarea.command.rules.removed.folder",
                    Messages.arg("rule", rule.getKey()),
                    Messages.arg("count", c),
                    Messages.arg("total", t),
                    Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
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

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId));
            return 0;
        }

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

    private static SuggestionProvider<ServerCommandSource> suggestAllRules() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) {
                builder.suggest(key);
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestTargetBlockX() {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getEntity() instanceof ServerPlayerEntity p ? p : null;
            if (player == null) return builder.buildFuture();
            HitResult hit = player.raycast(100, 0, false);
            BlockPos pos = (hit instanceof BlockHitResult bhr) ? bhr.getBlockPos() : player.getBlockPos();
            builder.suggest(String.valueOf(pos.getX()));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestTargetBlockY() {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getEntity() instanceof ServerPlayerEntity p ? p : null;
            if (player == null) return builder.buildFuture();
            HitResult hit = player.raycast(100, 0, false);
            BlockPos pos = (hit instanceof BlockHitResult bhr) ? bhr.getBlockPos() : player.getBlockPos();
            builder.suggest(String.valueOf(pos.getY()));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestTargetBlockZ() {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getEntity() instanceof ServerPlayerEntity p ? p : null;
            if (player == null) return builder.buildFuture();
            HitResult hit = player.raycast(100, 0, false);
            BlockPos pos = (hit instanceof BlockHitResult bhr) ? bhr.getBlockPos() : player.getBlockPos();
            builder.suggest(String.valueOf(pos.getZ()));
            return builder.buildFuture();
        };
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("create")
                .then(CommandManager.literal("cube")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .then(CommandManager.argument("x1", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                        .then(CommandManager.argument("y1", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                .then(CommandManager.argument("z1", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                        .then(CommandManager.argument("x2", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                                .then(CommandManager.argument("y2", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                                        .then(CommandManager.argument("z2", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                                .executes(context -> executeCreateCube(context, plugin))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("flat")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .then(CommandManager.argument("flatPosition", IntegerArgumentType.integer(0, 16))
                                        .then(CommandManager.argument("x1", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                .then(CommandManager.argument("y1", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                        .then(CommandManager.argument("z1", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                .then(CommandManager.argument("x2", IntegerArgumentType.integer()).suggests(suggestTargetBlockX())
                                                                        .then(CommandManager.argument("y2", IntegerArgumentType.integer()).suggests(suggestTargetBlockY())
                                                                                .then(CommandManager.argument("z2", IntegerArgumentType.integer()).suggests(suggestTargetBlockZ())
                                                                                        .executes(context -> executeCreateFlat(context, plugin))
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

    private static int executeCreateCube(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity p ? p : null;

        String id = StringArgumentType.getString(context, "id");
        int x1 = IntegerArgumentType.getInteger(context, "x1");
        int y1 = IntegerArgumentType.getInteger(context, "y1");
        int z1 = IntegerArgumentType.getInteger(context, "z1");
        int x2 = IntegerArgumentType.getInteger(context, "x2");
        int y2 = IntegerArgumentType.getInteger(context, "y2");
        int z2 = IntegerArgumentType.getInteger(context, "z2");

        String dimension = (player != null)
                ? WorldDimensionUtil.getDimensionKey(player.getServerWorld().getRegistryKey())
                : "minecraft:overworld";

        if (plugin.getAreaManager().createArea(id, dimension, x1, y1, z1, x2, y2, z2, "cube", 0)) {
            Messages.send(source, "protectedarea.command.create.success", Messages.arg("id", id));
            Messages.send(source, "protectedarea.command.create.type_line");
            Messages.send(source, "protectedarea.command.create.dimension_line", Messages.arg("dimension", dimension));
            ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
            plugin.getAreaManager().broadcastNewArea(area);
            return 1;
        } else {
            Messages.error(source, "protectedarea.command.create.already_exists", Messages.arg("id", id));
            return 0;
        }
    }

    private static int executeCreateFlat(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity p ? p : null;

        String id = StringArgumentType.getString(context, "id");
        int flatPosition = IntegerArgumentType.getInteger(context, "flatPosition");
        int x1 = IntegerArgumentType.getInteger(context, "x1");
        int y1 = IntegerArgumentType.getInteger(context, "y1");
        int z1 = IntegerArgumentType.getInteger(context, "z1");
        int x2 = IntegerArgumentType.getInteger(context, "x2");
        int y2 = IntegerArgumentType.getInteger(context, "y2");
        int z2 = IntegerArgumentType.getInteger(context, "z2");

        String dimension = (player != null)
                ? WorldDimensionUtil.getDimensionKey(player.getServerWorld().getRegistryKey())
                : "minecraft:overworld";

        if (plugin.getAreaManager().createArea(id, dimension, x1, y1, z1, x2, y2, z2, "flat", flatPosition)) {
            Messages.send(source, "protectedarea.command.create.success", Messages.arg("id", id));
            Messages.send(source, "protectedarea.command.create.type_flat_line", Messages.arg("position", flatPosition));
            Messages.send(source, "protectedarea.command.create.dimension_line", Messages.arg("dimension", dimension));
            ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
            plugin.getAreaManager().broadcastNewArea(area);
            return 1;
        } else {
            Messages.error(source, "protectedarea.command.create.already_exists", Messages.arg("id", id));
            return 0;
        }
    }

    private static LiteralArgumentBuilder<ServerCommandSource> removeSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("remove")
                .then(CommandManager.argument("id", StringArgumentType.string())
                        .suggests(suggestAreaIds(plugin))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            String id = StringArgumentType.getString(context, "id");

                            if (id.endsWith("/")) {
                                List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(id);
                                if (list.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", id)); return 0; }
                                int count = 0;
                                for (ProtectedArea a : list) {
                                    if (plugin.getAreaManager().removeArea(a.getId())) {
                                        plugin.getAreaManager().broadcastRemoveArea(a.getId());
                                        count++;
                                    }
                                }
                                java.io.File areasRoot = new java.io.File(plugin.getDataPath(), "Areas");
                                String folderPath = id.substring(0, id.length() - 1).replace("/", java.io.File.separator);
                                deleteFolder(new java.io.File(areasRoot, folderPath));
                                deleteFolder(new java.io.File(new java.io.File(areasRoot, ".flat"), folderPath));
                                final int finalCount = count;
                                Messages.send(source, "protectedarea.command.remove.folder.success", Messages.arg("count", finalCount), Messages.arg("folder", id));
                                return 1;
                            }

                            if (plugin.getAreaManager().removeArea(id)) {
                                Messages.send(source, "protectedarea.command.remove.success", Messages.arg("id", id));
                                plugin.getAreaManager().broadcastRemoveArea(id);
                                return 1;
                            } else {
                                Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", id));
                                return 0;
                            }
                        })
                );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> reloadSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("reload")
                .then(CommandManager.literal("notifications")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            Messages.send(source, "protectedarea.command.reload.notifications.progress");
                            plugin.getNotificationManager().reloadConfigs();
                            Messages.send(source, "protectedarea.command.reload.notifications.success");
                            return 1;
                        }))
                .then(CommandManager.literal("config")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            Messages.send(source, "protectedarea.command.reload.config.progress");
                            plugin.getConfigManager().reloadConfig();
                            Messages.send(source, "protectedarea.command.reload.config.success");

                            boolean modRequired = plugin.getConfigManager().isModRequired();
                            String status = modRequired ? "<green><bold>ENABLED" : "<red><bold>DISABLED";
                            Messages.send(source, "protectedarea.command.reload.mod_status",
                                    Messages.markup("status", status));

                            return 1;
                        }))
                .then(CommandManager.literal("cube")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestCubeAreaIds(plugin))
                                .executes(context -> executeReloadArea(context, plugin, "cube"))
                        ))
                .then(CommandManager.literal("flat")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestFlatAreaIds(plugin))
                                .executes(context -> executeReloadArea(context, plugin, "flat"))
                        ))
                .then(CommandManager.literal("dimension")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeReloadArea(context, plugin, "dimension"))
                        ))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    Messages.send(source, "protectedarea.command.reload.areas.progress");
                    plugin.getAreaManager().reloadAreas();
                    Messages.send(source, "protectedarea.command.reload.areas.success",
                            Messages.arg("count", plugin.getAreaManager().getAreas().size()));
                    return 1;
                });
    }

    private static int executeReloadArea(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin, String type) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");

        if (!plugin.getAreaManager().reloadArea(areaId, type)) {
            Messages.error(source, "protectedarea.command.reload.area_not_found", Messages.arg("type", type), Messages.arg("area", areaId));
            return 0;
        }

        Messages.send(source, "protectedarea.command.reload.area_success", Messages.arg("type", type), Messages.arg("area", areaId));
        return 1;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> viewSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("view")
                .then(CommandManager.argument("target", EntityArgumentType.players())
                        .then(CommandManager.argument("enable", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerPlayerEntity sourcePlayer = source.getEntity() instanceof ServerPlayerEntity p ? p : null;

                                    boolean enable = BoolArgumentType.getBool(context, "enable");

                                    Collection<ServerPlayerEntity> targets;
                                    try {
                                        targets = EntityArgumentType.getPlayers(context, "target");
                                    } catch (CommandSyntaxException e) {
                                        Messages.error(source, "protectedarea.command.common.no_players_found");
                                        return 0;
                                    }

                                    if (targets.isEmpty()) {
                                        Messages.error(source, "protectedarea.command.common.no_players_found");
                                        return 0;
                                    }

                                    int count = 0;
                                    for (ServerPlayerEntity t : targets) {
                                        plugin.getAreaManager().sendViewToggle(t, enable);
                                        count++;
                                    }

                                    String statusStr = enable ? "<green>enabled" : "<red>disabled";
                                    final int finalCount = count;
                                    List<ServerPlayerEntity> targetList = List.copyOf(targets);
                                    if (count == 1) {
                                        ServerPlayerEntity target = targetList.get(0);
                                        if (sourcePlayer != null && target.getUuid().equals(sourcePlayer.getUuid())) {
                                            Messages.send(source, "protectedarea.command.view.self",
                                                    Messages.markup("status", statusStr));
                                        } else {
                                            Messages.send(source, "protectedarea.command.view.other",
                                                    Messages.markup("status", statusStr),
                                                    Messages.arg("player", target.getGameProfile().getName()));
                                        }
                                    } else {
                                        Messages.send(source, "protectedarea.command.view.multiple",
                                                Messages.markup("status", statusStr),
                                                Messages.arg("count", finalCount));
                                    }

                                    return 1;
                                })
                        )
                );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> colorSubcommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("color")
                .then(CommandManager.argument("id", StringArgumentType.string())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("hexcolor", StringArgumentType.word())
                                .executes(context -> executeColorCommand(context, plugin, ""))
                                .then(CommandManager.argument("alias", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            String alias = StringArgumentType.getString(context, "alias");
                                            return executeColorCommand(context, plugin, alias);
                                        })
                                )
                        )
                );
    }

    private static int executeColorCommand(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin, String alias) {
        ServerCommandSource source = context.getSource();

        String providedId = StringArgumentType.getString(context, "id");
        String rawColor = StringArgumentType.getString(context, "hexcolor");

        if (!rawColor.startsWith("#")) rawColor = "#" + rawColor;
        if (!rawColor.matches("^#[0-9A-Fa-f]{6}$")) {
            Messages.error(source, "protectedarea.command.color.invalid_format");
            return 0;
        }

        final String normalizedColor = rawColor.toUpperCase();

        if (providedId.endsWith("/")) {
            List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(providedId);
            if (areaList.isEmpty()) { Messages.error(source, "protectedarea.command.common.no_areas_in_folder", Messages.arg("folder", providedId)); return 0; }
            int count = 0;
            for (ProtectedArea a : areaList) {
                if (plugin.getAreaManager().setAreaColor(a.getId(), normalizedColor, alias)) {
                    plugin.getAreaManager().broadcastUpdateArea(plugin.getAreaManager().getAreas().get(a.getId()));
                    count++;
                }
            }
            final int c = count;
            Messages.send(source, "protectedarea.command.color.applied.folder",
                    Messages.arg("color", normalizedColor), Messages.arg("count", c), Messages.arg("area", providedId));
            return 1;
        }

        Map<String, ProtectedArea> areas = plugin.getAreaManager().getAreas();
        ProtectedArea area = areas.get(providedId);
        String matchedId = providedId;
        if (area == null) {
            for (String key : areas.keySet()) {
                if (key.equalsIgnoreCase(providedId)) { matchedId = key; area = areas.get(key); break; }
            }
        }
        if (area == null) { final String pid = providedId; Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", pid)); return 0; }

        final String fMatchedId = matchedId;
        if (plugin.getAreaManager().setAreaColor(matchedId, normalizedColor, alias)) {
            Messages.send(source, "protectedarea.command.color.updated", Messages.arg("area", fMatchedId), Messages.arg("color", normalizedColor));
            if (alias != null && !alias.isEmpty()) Messages.send(source, "protectedarea.command.color.alias", Messages.arg("alias", alias));
            ProtectedArea updated = plugin.getAreaManager().getAreas().get(matchedId);
            if (updated != null) plugin.getAreaManager().broadcastUpdateArea(updated);
            return 1;
        }
        Messages.error(source, "protectedarea.command.color.update_failed", Messages.arg("area", fMatchedId));
        return 0;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAreaIds(ProtectedAreaInit plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isDimension())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestCubeAreaIds(ProtectedAreaInit plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isCube())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<ServerCommandSource> suggestFlatAreaIds(ProtectedAreaInit plugin) {
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
