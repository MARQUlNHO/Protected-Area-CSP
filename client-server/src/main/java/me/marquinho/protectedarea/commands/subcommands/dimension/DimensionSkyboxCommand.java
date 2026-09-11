package me.marquinho.protectedarea.commands.subcommands.dimension;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public class DimensionSkyboxCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("skybox")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .then(CommandManager.argument("skybox_name", StringArgumentType.word())
                                        .executes(context -> executeAdd(context, plugin)))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeRemove(context, plugin))))
                .then(CommandManager.literal("info")
                        .then(CommandManager.argument("area_id", StringArgumentType.string())
                                .suggests(DimensionAreas.suggestIds(plugin))
                                .executes(context -> executeInfo(context, plugin))));
    }

    private static int executeAdd(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");
        String skyboxName = StringArgumentType.getString(context, "skybox_name");

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (plugin.getAreaManager().setSkyboxForArea(a.getId(), skyboxName)) count++;
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            final int c = count;
            Messages.send(source, "protectedarea.command.skybox.applied.folder",
                    Messages.arg("skybox", skyboxName), Messages.arg("count", c), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        if (plugin.getAreaManager().setSkyboxForArea(areaId, skyboxName)) {
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            Messages.send(source, "protectedarea.command.skybox.added.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.skybox.name_line", Messages.arg("skybox", skyboxName));
            Messages.send(source, "protectedarea.command.skybox.expected_file", Messages.arg("skybox", skyboxName));
            return 1;
        }
        Messages.error(source, "protectedarea.command.skybox.set_failed");
        return 0;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");

        if (DimensionAreas.isFolder(areaId)) {
            List<ProtectedArea> areaList = DimensionAreas.resolveFolder(source, plugin, areaId);
            if (areaList == null) return 0;
            int count = 0;
            for (ProtectedArea a : areaList) if (a.hasSkybox() && plugin.getAreaManager().setSkyboxForArea(a.getId(), "")) count++;
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            final int c = count;
            Messages.send(source, "protectedarea.command.skybox.removed.folder", Messages.arg("count", c), Messages.arg("area", areaId));
            return 1;
        }

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;
        if (!area.hasSkybox()) { Messages.send(source, "protectedarea.command.skybox.none_assigned"); return 0; }

        String previousSkybox = area.getSkybox();
        if (plugin.getAreaManager().setSkyboxForArea(areaId, "")) {
            plugin.getAreaManager().broadcastDimensionSkyboxes();
            Messages.send(source, "protectedarea.command.skybox.removed.success");
            Messages.send(source, "protectedarea.command.common.area_line", Messages.arg("area", areaId));
            Messages.send(source, "protectedarea.command.skybox.previous", Messages.arg("skybox", previousSkybox));
            return 1;
        }
        Messages.error(source, "protectedarea.command.skybox.remove_failed");
        return 0;
    }

    private static int executeInfo(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin) {
        ServerCommandSource source = context.getSource();
        String areaId = StringArgumentType.getString(context, "area_id");

        ProtectedArea area = DimensionAreas.resolve(source, plugin, areaId);
        if (area == null) return 0;

        Messages.send(source, "protectedarea.command.exception.list.divider");
        Messages.send(source, "protectedarea.command.skybox.info.header", Messages.arg("area", areaId));
        Messages.send(source, "protectedarea.command.common.blank_line");
        if (area.hasSkybox()) {
            Messages.send(source, "protectedarea.command.skybox.info.name_line", Messages.arg("skybox", area.getSkybox()));
            Messages.send(source, "protectedarea.command.skybox.info.file_line", Messages.arg("skybox", area.getSkybox()));
        } else {
            Messages.send(source, "protectedarea.command.skybox.info.none");
            Messages.send(source, "protectedarea.command.skybox.info.uses_dimension_sky");
        }
        Messages.send(source, "protectedarea.command.common.blank_line");
        Messages.send(source, "protectedarea.command.exception.list.divider");
        return 1;
    }
}
