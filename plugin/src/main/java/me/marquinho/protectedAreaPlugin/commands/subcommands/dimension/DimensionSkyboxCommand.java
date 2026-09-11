package me.marquinho.protectedAreaPlugin.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;

import java.util.List;

public class DimensionSkyboxCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("skybox")
                .then(Commands.literal("add")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(Commands.argument("skybox_name", StringArgumentType.word())
                                        .executes(context -> executeAdd(context, plugin))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIdsWithSkybox(plugin))
                                .executes(context -> executeRemove(context, plugin))
                        )
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeInfo(context, plugin))
                        )
                );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        String skyboxName = StringArgumentType.getString(context, "skybox_name");

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().setSkyboxForArea(a.getId(), skyboxName)) count++;
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            sender.sendMessage("§aSkybox §6" + skyboxName + " §aapplied to §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        if (plugin.getAreaManager().setSkyboxForArea(areaId, skyboxName)) {
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            sender.sendMessage("§aSkybox assigned successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§eSkybox: §6" + skyboxName);
            sender.sendMessage("§7Expected file on the client: §fconfig/ProtectedArea/assets/skybox/" + skyboxName + ".png");
            return 1;
        } else {
            sender.sendMessage("§cError assigning the skybox");
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (a.hasSkybox() && plugin.getAreaManager().setSkyboxForArea(a.getId(), "")) count++;
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            sender.sendMessage("§aSkybox removed from §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        if (!area.hasSkybox()) {
            sender.sendMessage("§eThe area §6" + areaId + " §ehas no skybox assigned");
            return 0;
        }

        String previous = area.getSkybox();
        if (plugin.getAreaManager().setSkyboxForArea(areaId, "")) {
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            sender.sendMessage("§aSkybox removed from the area!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§7Previous skybox: §f" + previous);
            return 1;
        } else {
            sender.sendMessage("§cError removing the skybox");
            return 0;
        }
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        sender.sendMessage("§e§m                                          ");
        sender.sendMessage("§6§lArea Skybox: §e" + areaId);
        sender.sendMessage("");
        if (area.hasSkybox()) {
            sender.sendMessage("  §aSkybox: §6" + area.getSkybox());
            sender.sendMessage("  §7File: §fconfig/ProtectedArea/assets/skybox/" + area.getSkybox() + ".png");
        } else {
            sender.sendMessage("  §7No skybox assigned");
        }
        sender.sendMessage("");
        sender.sendMessage("§e§m                                          ");
        return 1;
    }
}
