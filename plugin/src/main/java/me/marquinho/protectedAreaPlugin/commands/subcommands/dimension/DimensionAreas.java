package me.marquinho.protectedAreaPlugin.commands.subcommands.dimension;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class DimensionAreas {

    private DimensionAreas() {
    }

    public static SuggestionProvider<CommandSourceStack> suggestIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isDimension())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> suggestIdsWithSkybox(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isDimension() && e.getValue().hasSkybox())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> suggestAllRules() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) builder.suggest(key);
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> suggestAllRulesAndAdvanced() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) builder.suggest(key);
            builder.suggest("limit");
            return builder.buildFuture();
        };
    }

    public static boolean isFolder(String areaId) {
        return areaId.endsWith("/");
    }

    public static List<ProtectedArea> resolveFolder(CommandSender sender, ProtectedAreaPlugin plugin, String folderId) {
        List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(folderId, "dimension");
        if (areaList.isEmpty()) {
            sender.sendMessage("§cNo dimension areas in folder: " + folderId);
            return null;
        }
        return areaList;
    }

    public static ProtectedArea resolve(CommandSender sender, ProtectedAreaPlugin plugin, String areaId) {
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            sender.sendMessage("§cNo area exists with ID: " + areaId);
            return null;
        }
        if (!area.isDimension()) {
            sender.sendMessage("§cThe area is not of type dimension: " + areaId);
            return null;
        }
        return area;
    }
}
