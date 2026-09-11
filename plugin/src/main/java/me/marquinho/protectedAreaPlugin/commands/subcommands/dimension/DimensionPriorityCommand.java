package me.marquinho.protectedAreaPlugin.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;

import java.util.List;

public class DimensionPriorityCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("priority")
                .then(Commands.argument("area_id", StringArgumentType.string())
                        .suggests(DimensionAreas.suggestIds(plugin))
                        .then(Commands.argument("value", IntegerArgumentType.integer(Integer.MIN_VALUE + 1))
                                .executes(context -> executePriority(context, plugin))
                        )
                );
    }

    private static int executePriority(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin) {
        CommandSender sender = context.getSource().getSender();
        String areaId = StringArgumentType.getString(context, "area_id");
        int priority = IntegerArgumentType.getInteger(context, "value");

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(sender, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().setAreaPriority(a.getId(), priority)) count++;
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            sender.sendMessage("§aPriority §6" + priority + " §aapplied to §6" + count + " §aarea(s) in §6" + areaId);
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(sender, plugin, areaId);
        if (area == null) return 0;

        if (plugin.getAreaManager().setAreaPriority(areaId, priority)) {
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            sender.sendMessage("§aPriority updated successfully!");
            sender.sendMessage("§eArea: §6" + areaId);
            sender.sendMessage("§ePriority: §6" + priority);
            sender.sendMessage("");
            sender.sendMessage("§7Note: Higher number = Higher priority");
            return 1;
        } else {
            sender.sendMessage("§cError updating the priority");
            return 0;
        }
    }
}
