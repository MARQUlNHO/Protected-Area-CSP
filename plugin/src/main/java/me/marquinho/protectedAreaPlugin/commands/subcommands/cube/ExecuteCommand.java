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
                .then(Commands.argument("area_id", StringArgumentType.word())
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

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo existe un área con el ID: " + areaId);
            return 0;
        }

        List<Player> playersInArea = getPlayersInArea(plugin, area);

        if (playersInArea.isEmpty()) {
            sender.sendMessage("§eNo hay jugadores dentro del área §6" + areaId);
            return 0;
        }

        int successCount = 0;
        for (Player player : playersInArea) {
            String finalCommand = command.replace("{player}", player.getName());
            try {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                successCount++;
            } catch (Exception e) {
                sender.sendMessage("§cError al ejecutar comando para §6" + player.getName() + "§c: " + e.getMessage());
            }
        }

        sender.sendMessage("§aComando ejecutado en §6" + successCount + "§a/§6" + playersInArea.size() + " §ajugador(es) del área §6" + areaId);
        return 1;
    }

    private static List<Player> getPlayersInArea(ProtectedAreaPlugin plugin, ProtectedArea area) {
        List<Player> playersInArea = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ProtectedArea playerArea = plugin.getAreaManager().getAreaAt(player.getLocation());
            if (playerArea != null && playerArea.getId().equals(area.getId())) playersInArea.add(player);
        }
        return playersInArea;
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
