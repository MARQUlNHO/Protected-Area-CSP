package me.marquinho.protectedarea.commands.subcommands.cube;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.AreaCommandEntry;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.CommandSuggestions;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;

public class CommandCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("command")
                .then(CommandManager.literal("entry")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                        .then(CommandManager.argument("uses", IntegerArgumentType.integer(-1))
                                                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                        .suggests(CommandSuggestions.nestedCommand())
                                                        .executes(ctx -> executeAdd(ctx, plugin, true))
                                                )
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("exit")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                        .then(CommandManager.argument("uses", IntegerArgumentType.integer(-1))
                                                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                        .suggests(CommandSuggestions.nestedCommand())
                                                        .executes(ctx -> executeAdd(ctx, plugin, false))
                                                )
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.literal("entry")
                                .then(CommandManager.argument("area_id", StringArgumentType.string())
                                        .suggests(suggestAreaIds(plugin))
                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeRemove(ctx, plugin, true))
                                        )
                                )
                        )
                        .then(CommandManager.literal("exit")
                                .then(CommandManager.argument("area_id", StringArgumentType.string())
                                        .suggests(suggestAreaIds(plugin))
                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeRemove(ctx, plugin, false))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("list")
                        .then(CommandManager.literal("entry")
                                .then(CommandManager.argument("area_id", StringArgumentType.string())
                                        .suggests(suggestAreaIds(plugin))
                                        .executes(ctx -> executeList(ctx, plugin, true))
                                )
                        )
                        .then(CommandManager.literal("exit")
                                .then(CommandManager.argument("area_id", StringArgumentType.string())
                                        .suggests(suggestAreaIds(plugin))
                                        .executes(ctx -> executeList(ctx, plugin, false))
                                )
                        )
                )
                .then(CommandManager.literal("uses")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(CommandManager.literal("entry")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> executeUsesAdd(ctx, plugin, true))
                                                                )
                                                        )
                                                )
                                                .then(CommandManager.literal("exit")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> executeUsesAdd(ctx, plugin, false))
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(CommandManager.literal("all")
                                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(CommandManager.literal("entry")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> executeUsesAll(ctx, plugin, true, false))
                                                                )
                                                        )
                                                )
                                                .then(CommandManager.literal("exit")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> executeUsesAll(ctx, plugin, false, false))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(CommandManager.literal("entry")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> executeUsesSet(ctx, plugin, true))
                                                                )
                                                        )
                                                )
                                                .then(CommandManager.literal("exit")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> executeUsesSet(ctx, plugin, false))
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(CommandManager.literal("all")
                                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(CommandManager.literal("entry")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> executeUsesAll(ctx, plugin, true, true))
                                                                )
                                                        )
                                                )
                                                .then(CommandManager.literal("exit")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> executeUsesAll(ctx, plugin, false, true))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(CommandManager.literal("info")
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(CommandManager.literal("entry")
                                                        .executes(ctx -> executeUsesInfo(ctx, plugin, true))
                                                )
                                                .then(CommandManager.literal("exit")
                                                        .executes(ctx -> executeUsesInfo(ctx, plugin, false))
                                                )
                                        )
                                )
                        )
                );
    }

    private static int executeAdd(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) {
        ServerCommandSource source = ctx.getSource();
        String areaId  = StringArgumentType.getString(ctx, "area_id");
        int delay      = IntegerArgumentType.getInteger(ctx, "delay");
        int uses       = IntegerArgumentType.getInteger(ctx, "uses");
        String command = StringArgumentType.getString(ctx, "command");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        AreaCommandEntry entry = new AreaCommandEntry(uses, delay, command);
        plugin.getAreaManager().addAreaCommand(areaId, entry, isEntry);
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        String type = isEntry ? "entry" : "exit";
        int idx = list.size();
        Messages.send(source, "protectedarea.command.cmdentry.added", Messages.arg("type", type));
        Messages.send(source, "protectedarea.command.cmdentry.area_index", Messages.arg("area", areaId), Messages.arg("index", idx));
        Messages.send(source, "protectedarea.command.cmdentry.delay_uses", Messages.arg("delay", delay), Messages.arg("uses", uses));
        Messages.send(source, "protectedarea.command.cmdentry.command_line", Messages.arg("command", command));
        return 1;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) {
        ServerCommandSource source = ctx.getSource();
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) {
            final String fType = isEntry ? "entry" : "exit";
            Messages.error(source, "protectedarea.command.cmdentry.invalid_index_count", Messages.arg("count", list.size()), Messages.arg("type", fType));
            return 0;
        }
        AreaCommandEntry removed = list.get(index - 1);
        plugin.getAreaManager().removeAreaCommand(areaId, index - 1, isEntry);
        Messages.send(source, "protectedarea.command.cmdentry.removed");
        Messages.send(source, "protectedarea.command.cmdentry.area_command", Messages.arg("area", areaId), Messages.arg("command", removed.getCommand()));
        return 1;
    }

    private static int executeList(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) {
        ServerCommandSource source = ctx.getSource();
        String areaId = StringArgumentType.getString(ctx, "area_id");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        String type = isEntry ? "Entry" : "Exit";
        Messages.send(source, "protectedarea.command.exception.list.divider");
        Messages.send(source, "protectedarea.command.cmdentry.list.header", Messages.arg("type", type), Messages.arg("area", areaId));
        Messages.send(source, "protectedarea.command.common.blank_line");
        if (list.isEmpty()) {
            Messages.send(source, "protectedarea.command.cmdentry.list.empty");
        } else {
            for (int i = 0; i < list.size(); i++) {
                AreaCommandEntry e = list.get(i);
                final int idx = i;
                Messages.send(source, "protectedarea.command.cmdentry.list.entry", Messages.arg("index", idx + 1), Messages.arg("command", e.getCommand()));
                Messages.send(source, "protectedarea.command.cmdentry.list.entry_detail", Messages.arg("delay", e.getDelayTicks()), Messages.arg("maxuses", e.getMaxUses()));
            }
        }
        Messages.send(source, "protectedarea.command.common.blank_line");
        Messages.send(source, "protectedarea.command.exception.list.divider");
        return 1;
    }

    private static int executeUsesAdd(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        int amount    = IntegerArgumentType.getInteger(ctx, "amount");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.cmdentry.uses.area_not_found", Messages.arg("area", areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { Messages.error(source, "protectedarea.command.cmdentry.uses.invalid_index"); return 0; }
        String type = isEntry ? "entry" : "exit";
        AreaCommandEntry entry = list.get(index - 1);
        for (ServerPlayerEntity target : targets) {
            int current = plugin.getAreaCommandManager().getUses(target, areaId, type, index - 1);
            if (current == -1) current = entry.getMaxUses();
            int newVal = current + amount;
            plugin.getAreaCommandManager().setUses(target, areaId, type, index - 1, newVal);
            final int fv = newVal;
            Messages.send(source, "protectedarea.command.cmdentry.uses.add_result",
                    Messages.arg("amount", amount), Messages.arg("player", target.getGameProfile().getName()), Messages.arg("newvalue", fv));
        }
        return 1;
    }

    private static int executeUsesSet(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        int amount    = IntegerArgumentType.getInteger(ctx, "amount");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.cmdentry.uses.area_not_found", Messages.arg("area", areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { Messages.error(source, "protectedarea.command.cmdentry.uses.invalid_index"); return 0; }
        String type = isEntry ? "entry" : "exit";
        for (ServerPlayerEntity target : targets) {
            plugin.getAreaCommandManager().setUses(target, areaId, type, index - 1, amount);
            Messages.send(source, "protectedarea.command.cmdentry.uses.set_result",
                    Messages.arg("player", target.getGameProfile().getName()), Messages.arg("amount", amount));
        }
        return 1;
    }

    private static int executeUsesAll(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin,
                                      boolean isEntry, boolean set) {
        ServerCommandSource source = ctx.getSource();
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        int amount    = IntegerArgumentType.getInteger(ctx, "amount");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.cmdentry.uses.area_not_found", Messages.arg("area", areaId)); return 0; }

        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { Messages.error(source, "protectedarea.command.cmdentry.uses.invalid_index"); return 0; }

        String type = isEntry ? "entry" : "exit";
        AreaCommandEntry entry = list.get(index - 1);
        int maxUses = entry.getMaxUses();

        int online = 0;
        for (ServerPlayerEntity target : plugin.getServer().getPlayerManager().getPlayerList()) {
            int newValue;
            if (set) {
                newValue = amount;
            } else {
                int current = plugin.getAreaCommandManager().getUses(target, areaId, type, index - 1);
                if (current == -1) current = maxUses;
                newValue = current + amount;
            }
            plugin.getAreaCommandManager().setUses(target, areaId, type, index - 1, newValue);
            online++;
        }

        int offline = plugin.getAreaCommandManager()
                .applyUsesToOfflineCaches(areaId, type, index - 1, amount, set, maxUses);

        final int onlineCount = online;
        final int offlineCount = offline;
        String action = set ? "set to" : "added";
        Messages.send(source, "protectedarea.command.cmdentry.uses.all_result",
                Messages.arg("action", action), Messages.arg("amount", amount), Messages.arg("area", areaId),
                Messages.arg("type", type), Messages.arg("index", index));
        Messages.send(source, "protectedarea.command.cmdentry.uses.all_summary",
                Messages.arg("online", onlineCount), Messages.arg("offline", offlineCount));
        return 1;
    }

    private static int executeUsesInfo(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        String areaId = StringArgumentType.getString(ctx, "area_id");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { Messages.error(source, "protectedarea.command.cmdentry.uses.area_not_found", Messages.arg("area", areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        String type = isEntry ? "entry" : "exit";

        for (ServerPlayerEntity target : targets) {
            Messages.send(source, "protectedarea.command.exception.list.divider");
            Messages.send(source, "protectedarea.command.cmdentry.uses.info_header", Messages.arg("player", target.getGameProfile().getName()));
            Messages.send(source, "protectedarea.command.cmdentry.uses.info_area_type", Messages.arg("area", areaId), Messages.arg("type", type));
            Messages.send(source, "protectedarea.command.common.blank_line");

            if (list.isEmpty()) {
                Messages.send(source, "protectedarea.command.cmdentry.list.empty");
            } else {
                for (int i = 0; i < list.size(); i++) {
                    AreaCommandEntry entry = list.get(i);
                    final int idx = i;
                    Messages.send(source, "protectedarea.command.cmdentry.list.entry", Messages.arg("index", idx + 1), Messages.arg("command", entry.getCommand()));

                    if (entry.getMaxUses() == -1) {
                        Messages.send(source, "protectedarea.command.cmdentry.uses.unlimited");
                    } else {
                        int current = plugin.getAreaCommandManager().getUses(target, areaId, type, idx);
                        int remaining = (current == -1) ? entry.getMaxUses() : current;
                        Messages.send(source, "protectedarea.command.cmdentry.uses.remaining",
                                Messages.arg("remaining", remaining), Messages.arg("max", entry.getMaxUses()));
                    }
                }
            }

            Messages.send(source, "protectedarea.command.exception.list.divider");
        }
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAreaIds(ProtectedAreaInit plugin) {
        return (ctx, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isCube())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(java.util.Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }
}
