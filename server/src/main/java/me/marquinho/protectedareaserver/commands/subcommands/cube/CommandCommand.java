package me.marquinho.protectedareaserver.commands.subcommands.cube;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.models.AreaCommandEntry;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.util.TextUtil;
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
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                        .then(CommandManager.argument("uses", IntegerArgumentType.integer(-1))
                                                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeAdd(ctx, plugin, true))
                                                )
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("exit")
                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                        .then(CommandManager.argument("uses", IntegerArgumentType.integer(-1))
                                                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeAdd(ctx, plugin, false))
                                                )
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("remove")
                        .then(CommandManager.literal("entry")
                                .then(CommandManager.argument("area_id", StringArgumentType.word())
                                        .suggests(suggestAreaIds(plugin))
                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeRemove(ctx, plugin, true))
                                        )
                                )
                        )
                        .then(CommandManager.literal("exit")
                                .then(CommandManager.argument("area_id", StringArgumentType.word())
                                        .suggests(suggestAreaIds(plugin))
                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeRemove(ctx, plugin, false))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("list")
                        .then(CommandManager.literal("entry")
                                .then(CommandManager.argument("area_id", StringArgumentType.word())
                                        .suggests(suggestAreaIds(plugin))
                                        .executes(ctx -> executeList(ctx, plugin, true))
                                )
                        )
                        .then(CommandManager.literal("exit")
                                .then(CommandManager.argument("area_id", StringArgumentType.word())
                                        .suggests(suggestAreaIds(plugin))
                                        .executes(ctx -> executeList(ctx, plugin, false))
                                )
                        )
                )
                .then(CommandManager.literal("uses")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.argument("area_id", StringArgumentType.word())
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
                        )
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.argument("area_id", StringArgumentType.word())
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
                        )
                        .then(CommandManager.literal("info")
                                .then(CommandManager.argument("target", EntityArgumentType.players())
                                        .then(CommandManager.argument("area_id", StringArgumentType.word())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(CommandManager.literal("entry")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> executeUsesInfo(ctx, plugin, true))
                                                        )
                                                )
                                                .then(CommandManager.literal("exit")
                                                        .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> executeUsesInfo(ctx, plugin, false))
                                                        )
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
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        AreaCommandEntry entry = new AreaCommandEntry(uses, delay, command);
        plugin.getAreaManager().addAreaCommand(areaId, entry, isEntry);
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        String type = isEntry ? "entry" : "exit";
        int idx = list.size();
        source.sendFeedback(() -> TextUtil.parse("<green>¡Comando de " + type + " agregado!"), false);
        source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId + "  <yellow>Índice: <gold>" + idx), false);
        source.sendFeedback(() -> TextUtil.parse("<yellow>Delay: <gold>" + delay + "t  <yellow>Usos: <gold>" + uses), false);
        source.sendFeedback(() -> TextUtil.parse("<yellow>Comando: <gold>" + command), false);
        return 1;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) {
        ServerCommandSource source = ctx.getSource();
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) {
            source.sendError(TextUtil.parse("<red>Índice inválido. El área tiene " + list.size() + " comando(s) de " + (isEntry ? "entry" : "exit") + "."));
            return 0;
        }
        AreaCommandEntry removed = list.get(index - 1);
        plugin.getAreaManager().removeAreaCommand(areaId, index - 1, isEntry);
        source.sendFeedback(() -> TextUtil.parse("<green>¡Comando eliminado!"), false);
        source.sendFeedback(() -> TextUtil.parse("<yellow>Área: <gold>" + areaId + "  <yellow>Comando: <gold>" + removed.getCommand()), false);
        return 1;
    }

    private static int executeList(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) {
        ServerCommandSource source = ctx.getSource();
        String areaId = StringArgumentType.getString(ctx, "area_id");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        String type = isEntry ? "Entry" : "Exit";
        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        source.sendFeedback(() -> TextUtil.parse("<gold><bold>Comandos de " + type + " - <yellow>" + areaId), false);
        source.sendFeedback(() -> TextUtil.parse(""), false);
        if (list.isEmpty()) {
            source.sendFeedback(() -> TextUtil.parse("  <gray>No hay comandos configurados."), false);
        } else {
            for (int i = 0; i < list.size(); i++) {
                AreaCommandEntry e = list.get(i);
                final int idx = i;
                source.sendFeedback(() -> TextUtil.parse("  <yellow>[" + (idx + 1) + "] <white>" + e.getCommand()), false);
                source.sendFeedback(() -> TextUtil.parse("      <gray>Delay: <white>" + e.getDelayTicks() + "t  <gray>Usos máx: <white>" + e.getMaxUses()), false);
            }
        }
        source.sendFeedback(() -> TextUtil.parse(""), false);
        source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        return 1;
    }

    private static int executeUsesAdd(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        int amount    = IntegerArgumentType.getInteger(ctx, "amount");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>Área no encontrada: " + areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { source.sendError(TextUtil.parse("<red>Índice inválido.")); return 0; }
        String type = isEntry ? "entry" : "exit";
        AreaCommandEntry entry = list.get(index - 1);
        for (ServerPlayerEntity target : targets) {
            int current = plugin.getAreaCommandManager().getUses(target, areaId, type, index - 1);
            if (current == -1) current = entry.getMaxUses();
            int newVal = current + amount;
            plugin.getAreaCommandManager().setUses(target, areaId, type, index - 1, newVal);
            final int fv = newVal;
            source.sendFeedback(() -> TextUtil.parse("<green>+" + amount + " usos para <gold>" + target.getGameProfile().getName() + "<green> → <gold>" + fv), false);
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
        if (area == null) { source.sendError(TextUtil.parse("<red>Área no encontrada: " + areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { source.sendError(TextUtil.parse("<red>Índice inválido.")); return 0; }
        String type = isEntry ? "entry" : "exit";
        for (ServerPlayerEntity target : targets) {
            plugin.getAreaCommandManager().setUses(target, areaId, type, index - 1, amount);
            source.sendFeedback(() -> TextUtil.parse("<green>Usos de <gold>" + target.getGameProfile().getName() + "<green> seteados a <gold>" + amount), false);
        }
        return 1;
    }

    private static int executeUsesInfo(CommandContext<ServerCommandSource> ctx, ProtectedAreaInit plugin, boolean isEntry) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { source.sendError(TextUtil.parse("<red>Área no encontrada: " + areaId)); return 0; }
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { source.sendError(TextUtil.parse("<red>Índice inválido.")); return 0; }
        String type = isEntry ? "entry" : "exit";
        AreaCommandEntry entry = list.get(index - 1);
        for (ServerPlayerEntity target : targets) {
            int current = plugin.getAreaCommandManager().getUses(target, areaId, type, index - 1);
            int display = (current == -1) ? entry.getMaxUses() : current;
            source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
            source.sendFeedback(() -> TextUtil.parse("<gold><bold>Usos - <yellow>" + target.getGameProfile().getName()), false);
            source.sendFeedback(() -> TextUtil.parse("  <yellow>Área: <gold>" + areaId + "  <yellow>Tipo: <gold>" + type + "  <yellow>Índice: <gold>" + index), false);
            source.sendFeedback(() -> TextUtil.parse("  <yellow>Comando: <gold>" + entry.getCommand()), false);
            source.sendFeedback(() -> TextUtil.parse("  <yellow>Usos restantes: <gold>" + display + " <yellow>de <gold>" + entry.getMaxUses()), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow><st>                                          </st>"), false);
        }
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAreaIds(ProtectedAreaInit plugin) {
        return (ctx, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(java.util.Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
