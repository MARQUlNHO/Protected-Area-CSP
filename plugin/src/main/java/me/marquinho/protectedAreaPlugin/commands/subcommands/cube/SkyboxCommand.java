package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;

public class SkyboxCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("skybox")
                .then(Commands.literal("add")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIds(plugin))
                                .then(Commands.argument("skybox_name", StringArgumentType.word())
                                        .executes(context -> executeAdd(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.word())
                                .suggests(suggestAreaIdsWithSkybox(plugin))
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
        String skyboxName = StringArgumentType.getString(context, "skybox_name");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        if (plugin.getAreaManager().setSkyboxForArea(areaId, skyboxName)) {
            sender.sendMessage("§a¡Skybox asignado exitosamente!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§eSkybox: §6" + skyboxName);
            sender.sendMessage("§7Coloca el archivo en: §fconfig/ProtectedArea/assets/skybox/" + skyboxName + ".png");
            return 1;
        } else {
            sender.sendMessage("§cError al asignar el skybox");
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }
        if (!area.hasSkybox()) { sender.sendMessage("§eEl área §6" + areaId + " §eno tiene un skybox asignado"); return 0; }

        String previous = area.getSkybox();
        if (plugin.getAreaManager().setSkyboxForArea(areaId, "")) {
            sender.sendMessage("§a¡Skybox removido!");
            sender.sendMessage("§eÁrea: §6" + areaId);
            sender.sendMessage("§7Skybox anterior: §f" + previous);
            return 1;
        } else {
            sender.sendMessage("§cError al remover el skybox");
            return 0;
        }
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lInformación de Skybox - §e" + areaId);
        sender.sendMessage("");
        if (area.hasSkybox()) {
            sender.sendMessage("  §aSkybox: §6" + area.getSkybox());
            sender.sendMessage("  §7Archivo esperado: §fconfig/ProtectedArea/assets/skybox/" + area.getSkybox() + ".png");
        } else {
            sender.sendMessage("  §7No hay skybox asignado");
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

    private static SuggestionProvider<CommandSourceStack> suggestAreaIdsWithSkybox(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().values().stream()
                    .filter(a -> !a.isFlat() && a.hasSkybox())
                    .map(ProtectedArea::getId)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
