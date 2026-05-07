package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.AreaCommandEntry;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("command")
                .then(Commands.literal("entry")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(Commands.argument("delay", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("uses", IntegerArgumentType.integer(-1))
                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeAdd(ctx, plugin, true))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("exit")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(Commands.argument("delay", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("uses", IntegerArgumentType.integer(-1))
                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeAdd(ctx, plugin, false))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.literal("entry")
                                .then(Commands.argument("area_id", StringArgumentType.word())
                                        .suggests(suggestAreaIds(plugin))
                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeRemove(ctx, plugin, true))
                                        )
                                )
                        )
                        .then(Commands.literal("exit")
                                .then(Commands.argument("area_id", StringArgumentType.word())
                                        .suggests(suggestAreaIds(plugin))
                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeRemove(ctx, plugin, false))
                                        )
                                )
                        )
                )
                .then(Commands.literal("list")
                        .then(Commands.literal("entry")
                                .then(Commands.argument("area_id", StringArgumentType.word())
                                        .suggests(suggestAreaIds(plugin))
                                        .executes(ctx -> executeList(ctx, plugin, true))
                                )
                        )
                        .then(Commands.literal("exit")
                                .then(Commands.argument("area_id", StringArgumentType.word())
                                        .suggests(suggestAreaIds(plugin))
                                        .executes(ctx -> executeList(ctx, plugin, false))
                                )
                        )
                )
                .then(Commands.literal("uses")
                        .then(Commands.literal("add")
                                .then(Commands.argument("target", ArgumentTypes.players())
                                        .then(Commands.argument("area_id", StringArgumentType.word())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(Commands.literal("entry")
                                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> executeUsesAdd(ctx, plugin, true))
                                                                )
                                                        )
                                                )
                                                .then(Commands.literal("exit")
                                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> executeUsesAdd(ctx, plugin, false))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", ArgumentTypes.players())
                                        .then(Commands.argument("area_id", StringArgumentType.word())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(Commands.literal("entry")
                                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> executeUsesSet(ctx, plugin, true))
                                                                )
                                                        )
                                                )
                                                .then(Commands.literal("exit")
                                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> executeUsesSet(ctx, plugin, false))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("info")
                                .then(Commands.argument("target", ArgumentTypes.players())
                                        .then(Commands.argument("area_id", StringArgumentType.word())
                                                .suggests(suggestAreaIds(plugin))
                                                .then(Commands.literal("entry")
                                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> executeUsesInfo(ctx, plugin, true))
                                                        )
                                                )
                                                .then(Commands.literal("exit")
                                                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> executeUsesInfo(ctx, plugin, false))
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin, boolean isEntry) {
        CommandSender sender = ctx.getSource().getSender();
        String areaId  = StringArgumentType.getString(ctx, "area_id");
        int delay      = IntegerArgumentType.getInteger(ctx, "delay");
        int uses       = IntegerArgumentType.getInteger(ctx, "uses");
        String command = StringArgumentType.getString(ctx, "command");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        AreaCommandEntry entry = new AreaCommandEntry(uses, delay, command);
        plugin.getAreaManager().addAreaCommand(areaId, entry, isEntry);

        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        String type = isEntry ? "entry" : "exit";
        sender.sendMessage("§a¡Comando de " + type + " agregado!");
        sender.sendMessage("§eÁrea: §6" + areaId + "  §eÍndice: §6" + list.size());
        sender.sendMessage("§eDelay: §6" + delay + "t  §eUsos: §6" + uses);
        sender.sendMessage("§eComando: §6" + command);
        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin, boolean isEntry) {
        CommandSender sender = ctx.getSource().getSender();
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) {
            sender.sendMessage("§cÍndice inválido. El área tiene " + list.size() + " comando(s) de " + (isEntry ? "entry" : "exit") + ".");
            return 0;
        }

        AreaCommandEntry removed = list.get(index - 1);
        plugin.getAreaManager().removeAreaCommand(areaId, index - 1, isEntry);
        sender.sendMessage("§a¡Comando eliminado!");
        sender.sendMessage("§eÁrea: §6" + areaId + "  §eComando: §6" + removed.getCommand());
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin, boolean isEntry) {
        CommandSender sender = ctx.getSource().getSender();
        String areaId = StringArgumentType.getString(ctx, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        String type = isEntry ? "Entry" : "Exit";
        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lComandos de " + type + " - §e" + areaId);
        sender.sendMessage("");
        if (list.isEmpty()) {
            sender.sendMessage("  §7No hay comandos configurados.");
        } else {
            for (int i = 0; i < list.size(); i++) {
                AreaCommandEntry e = list.get(i);
                sender.sendMessage("  §e[" + (i + 1) + "] §f" + e.getCommand());
                sender.sendMessage("      §7Delay: §f" + e.getDelayTicks() + "t  §7Usos máx: §f" + e.getMaxUses());
            }
        }
        sender.sendMessage("");
        sender.sendMessage("§e§m                                          ");
        return 1;
    }

    private static int executeUsesAdd(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin, boolean isEntry) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        List<Player> targets = resolveTargets(ctx);
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        int amount    = IntegerArgumentType.getInteger(ctx, "amount");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cÁrea no encontrada: " + areaId); return 0; }

        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { sender.sendMessage("§cÍndice inválido. El área tiene " + list.size() + " comando(s)."); return 0; }

        String type = isEntry ? "entry" : "exit";
        AreaCommandEntry entry = list.get(index - 1);
        for (Player target : targets) {
            int current = plugin.getAreaCommandManager().getUses(target, areaId, type, index - 1);
            if (current == -1) current = entry.getMaxUses();
            int newVal = current + amount;
            plugin.getAreaCommandManager().setUses(target, areaId, type, index - 1, newVal);
            sender.sendMessage("§a+" + amount + " usos para §6" + target.getName() + "§a en [§6" + areaId + "/" + type + "/" + index + "§a] → §6" + newVal);
        }
        return 1;
    }

    private static int executeUsesSet(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin, boolean isEntry) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        List<Player> targets = resolveTargets(ctx);
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");
        int amount    = IntegerArgumentType.getInteger(ctx, "amount");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cÁrea no encontrada: " + areaId); return 0; }

        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { sender.sendMessage("§cÍndice inválido. El área tiene " + list.size() + " comando(s)."); return 0; }

        String type = isEntry ? "entry" : "exit";
        for (Player target : targets) {
            plugin.getAreaCommandManager().setUses(target, areaId, type, index - 1, amount);
            sender.sendMessage("§aUsos de §6" + target.getName() + "§a seteados a §6" + amount + "§a en [§6" + areaId + "/" + type + "/" + index + "§a]");
        }
        return 1;
    }

    private static int executeUsesInfo(CommandContext<CommandSourceStack> ctx, ProtectedAreaPlugin plugin, boolean isEntry) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        List<Player> targets = resolveTargets(ctx);
        String areaId = StringArgumentType.getString(ctx, "area_id");
        int index     = IntegerArgumentType.getInteger(ctx, "index");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cÁrea no encontrada: " + areaId); return 0; }

        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index < 1 || index > list.size()) { sender.sendMessage("§cÍndice inválido. El área tiene " + list.size() + " comando(s)."); return 0; }

        String type = isEntry ? "entry" : "exit";
        AreaCommandEntry entry = list.get(index - 1);
        for (Player target : targets) {
            int current = plugin.getAreaCommandManager().getUses(target, areaId, type, index - 1);
            int display = (current == -1) ? entry.getMaxUses() : current;
            sender.sendMessage("§e§m                                          ");
            sender.sendMessage("§6§lUsos - §e" + target.getName());
            sender.sendMessage("  §eÁrea: §6" + areaId + "  §eTipo: §6" + type + "  §eÍndice: §6" + index);
            sender.sendMessage("  §eComando: §6" + entry.getCommand());
            sender.sendMessage("  §eUsos restantes: §6" + display + " §ede §6" + entry.getMaxUses());
            sender.sendMessage("§e§m                                          ");
        }
        return 1;
    }

    private static List<Player> resolveTargets(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerSelectorArgumentResolver resolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
        return resolver.resolve(ctx.getSource());
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
        return (ctx, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(java.util.Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
