package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LimitCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("limit")
                .then(Commands.literal("add")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                                        .executes(context -> executeAdd(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("block")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(Commands.argument("blocked", BoolArgumentType.bool())
                                        .executes(context -> executeBlock(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .executes(context -> executeRemove(context, plugin))
                        )
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .executes(context -> executeInfo(context, plugin))
                        )
                );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        int limit = IntegerArgumentType.getInteger(context, "limit");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        if (plugin.getAreaManager().setAreaLimit(areaId, limit)) {
            sender.sendMessage("§a¡Límite de jugadores actualizado!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eLímite: §6" + limit + " jugador(es)");
            int currentPlayers = plugin.getAreaManager().getPlayersInArea(areaId);
            sender.sendMessage("§eJugadores actuales: §6" + currentPlayers + "§e/§6" + limit);
            return 1;
        } else {
            sender.sendMessage("§cError al establecer el límite");
            return 0;
        }
    }

    private static int executeBlock(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        boolean blocked = BoolArgumentType.getBool(context, "blocked");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        if (!area.hasPlayerLimit()) {
            sender.sendMessage("§cEl área no tiene un límite de jugadores configurado");
            sender.sendMessage("§eUsa §6/area cube limit add " + areaId + " <número> §eprimero");
            return 0;
        }

        if (plugin.getAreaManager().setAreaLimitBlocked(areaId, blocked)) {
            if (blocked) {
                sender.sendMessage("§c¡Área bloqueada!");
                sender.sendMessage("§eÁrea: §6" + areaId);
                int trapped = plugin.getAreaManager().getPlayersInArea(areaId);
                sender.sendMessage("§eJugadores dentro: §6" + trapped);
            } else {
                sender.sendMessage("§a¡Área desbloqueada!");
                sender.sendMessage("§eÁrea: §6" + areaId);
            }
            return 1;
        } else {
            sender.sendMessage("§cError al cambiar el estado de bloqueo");
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }
        if (!area.hasPlayerLimit()) { sender.sendMessage("§eEl área no tiene un límite de jugadores configurado"); return 0; }

        if (plugin.getAreaManager().removeAreaLimit(areaId)) {
            sender.sendMessage("§a¡Límite de jugadores removido!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            return 1;
        } else {
            sender.sendMessage("§cError al remover el límite");
            return 0;
        }
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lInformación de Límite - §e" + areaId);
        sender.sendMessage("");

        if (area.hasPlayerLimit()) {
            int limit = area.getPlayerLimit();
            int current = plugin.getAreaManager().getPlayersInArea(areaId);
            sender.sendMessage("  §aLímite: §6" + limit + " jugador(es)");
            sender.sendMessage("  §aActuales: §6" + current + "§e/§6" + limit);
            int available = limit - current;
            if (available > 0) sender.sendMessage("  §aEspacios disponibles: §2" + available);
            else sender.sendMessage("  §cÁrea llena");
            sender.sendMessage("");
            if (area.isLimitBlocked()) {
                sender.sendMessage("  §cEstado: §4§lBLOQUEADA");
                sender.sendMessage("  §7Nadie puede entrar o salir");
            } else {
                sender.sendMessage("  §aEstado: §2§lACTIVA");
                sender.sendMessage("  §7Se respeta el límite de jugadores");
            }
        } else {
            sender.sendMessage("  §7No hay límite configurado");
        }

        sender.sendMessage("");
        sender.sendMessage("§e§m                                          ");
        return 1;
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(java.util.Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
