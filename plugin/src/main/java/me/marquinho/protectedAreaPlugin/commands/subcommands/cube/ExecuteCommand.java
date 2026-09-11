package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ExecuteCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("execute")
                .then(Commands.argument("area_id", StringArgumentType.string())
                        .suggests(suggestAreaIds(plugin))
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(context -> executeCommand(context, plugin))
                        )
                );
    }

    private static int executeCommand(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String command = StringArgumentType.getString(context, "command");

        if (areaId.endsWith("/")) {
            List<ProtectedArea> list = plugin.getAreaManager().getAreasByFolder(areaId);
            if (list.isEmpty()) { sender.sendMessage("§cNo areas in folder: " + areaId); return 0; }
            int total = 0, success = 0;
            for (ProtectedArea a : list) {
                for (Player player : getPlayersInArea(plugin, a)) {
                    total++;
                    try {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
                        success++;
                    } catch (Exception e) {
                        sender.sendMessage("§cError for §6" + player.getName() + "§c: " + e.getMessage());
                    }
                }
            }
            sender.sendMessage("§aCommand executed on §6" + success + "§a/§6" + total + " §aplayer(s) in folder §6" + areaId);
            return 1;
        }

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return 0;
        }

        List<Player> playersInArea = getPlayersInArea(plugin, area);

        if (playersInArea.isEmpty()) {
            sender.sendMessage("§eNo players inside area §6" + areaId);
            return 0;
        }

        int successCount = 0;
        for (Player player : playersInArea) {
            String finalCommand = command.replace("{player}", player.getName());
            try {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                successCount++;
            } catch (Exception e) {
                sender.sendMessage("§cError executing command for §6" + player.getName() + "§c: " + e.getMessage());
            }
        }

        sender.sendMessage("§aCommand executed on §6" + successCount + "§a/§6" + playersInArea.size() + " §aplayer(s) in area §6" + areaId);
        return 1;
    }

    private static List<Player> getPlayersInArea(ProtectedAreaPlugin plugin, ProtectedArea area) {
        return plugin.getAreaManager().getPlayersInsideArea(area.getId());
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isCube())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(java.util.Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }
}
