package me.marquinho.protectedarea.commands.subcommands.dimension;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.AdvancedRuleType;
import me.marquinho.protectedarea.models.AreaRule;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.Messages;
import me.marquinho.protectedarea.util.TextUtil;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Map;

public final class DimensionAreas {

    private DimensionAreas() {
    }

    public static SuggestionProvider<ServerCommandSource> suggestIds(ProtectedAreaInit plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> e.getValue().isDimension())
                    .filter(e -> !plugin.getConfigManager().isIgnoredInCommand(e.getValue()))
                    .map(Map.Entry::getKey)
                    .forEach(id -> builder.suggest(id.contains("/") ? "\"" + id + "\"" : id));
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<ServerCommandSource> suggestAllRules() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) builder.suggest(key);
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<ServerCommandSource> suggestAllRulesAndAdvanced() {
        return (context, builder) -> {
            for (String key : AreaRule.getAllKeys()) builder.suggest(key);
            for (String key : AdvancedRuleType.getAllKeys()) builder.suggest(key);
            builder.suggest("limit");
            return builder.buildFuture();
        };
    }

    public static boolean isFolder(String areaId) {
        return areaId.endsWith("/");
    }

    public static List<ProtectedArea> resolveFolder(ServerCommandSource source, ProtectedAreaInit plugin, String folderId) {
        List<ProtectedArea> areaList = plugin.getAreaManager().getAreasByFolder(folderId, "dimension");
        if (areaList.isEmpty()) {
            Messages.error(source, "protectedarea.command.dimension.no_areas_in_folder", Messages.arg("folder", folderId));
            return null;
        }
        return areaList;
    }

    public static ProtectedArea resolve(ServerCommandSource source, ProtectedAreaInit plugin, String areaId) {
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) {
            Messages.error(source, "protectedarea.command.common.area_not_found", Messages.arg("area", areaId));
            return null;
        }
        if (!area.isDimension()) {
            Messages.error(source, "protectedarea.command.dimension.not_dimension_type", Messages.arg("area", areaId));
            return null;
        }
        return area;
    }
}
