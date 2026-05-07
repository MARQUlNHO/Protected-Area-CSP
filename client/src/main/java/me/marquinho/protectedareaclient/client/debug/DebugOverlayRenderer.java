package me.marquinho.protectedareaclient.client.debug;

import me.marquinho.protectedareaclient.client.ProtectedareaclientClient;
import me.marquinho.protectedareaclient.client.debug.DebugState.RuleEntry;
import me.marquinho.protectedareaclient.client.debug.DebugState.ExceptionEntry;
import me.marquinho.protectedareaclient.client.debug.DebugState.AdvancedEntry;
import me.marquinho.protectedareaclient.client.managers.AreaTracker;
import me.marquinho.protectedareaclient.client.models.ProtectedArea;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;

import java.util.ArrayList;
import java.util.List;

public class DebugOverlayRenderer {

    private static final int PANEL_W      = 210;
    private static final int MARGIN_RIGHT = 4;
    private static final int MARGIN_TOP   = 4;
    private static final int PADDING      = 5;
    private static final int LINE_H       = 11;

    private static final int COLOR_TITLE_BG  = 0x80000000;
    private static final int COLOR_TITLE_FG  = 0xFFFFD700;
    private static final int COLOR_AREA_ID   = 0xFF00CFFF;
    private static final int COLOR_VALUE     = 0xFFFFFFFF;
    private static final int COLOR_TYPE_CUBE  = 0xFF4DFF91;
    private static final int COLOR_TYPE_FLAT  = 0xFF00BFFF;
    private static final int COLOR_SIDE_POS   = 0xFF4DFF91;
    private static final int COLOR_SIDE_NEG   = 0xFFFF4D4D;
    private static final int COLOR_RULE_OK   = 0xFF4DFF91;
    private static final int COLOR_RULE_EX   = 0xFFFF4D4D;
    private static final int COLOR_NO_AREA   = 0xFFBBBBBB;
    private static final int COLOR_NAV       = 0xFFFFFFFF;
    private static final int COLOR_SEP       = 0xFFAAAAAA;
    private static final int COLOR_COORDS    = 0xFFFFE066;
    private static final int COLOR_EX_RULE   = 0xFFFF9A3C;
    private static final int COLOR_EX_PLAYER = 0xFFFFFFFF;
    private static final int COLOR_LIMIT_OK  = 0xFF4DFF91;
    private static final int COLOR_LIMIT_WARN= 0xFFFF4D4D;
    private static final int COLOR_ADV_YES   = 0xFF4DFF91;
    private static final int COLOR_ADV_NO    = 0xFFFF4D4D;

    public static void register() {
        HudRenderCallback.EVENT.register(DebugOverlayRenderer::render);
    }

    private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        DebugState state = DebugStateHolder.get();
        if (!state.isActive()) return;

        TextRenderer tr = client.textRenderer;
        int screenW = client.getWindow().getScaledWidth();

        AreaTracker tracker = ProtectedareaclientClient.getAreaTracker();
        String debugType = state.getDebugType();
        ProtectedArea currentArea = resolveCurrentArea(client, tracker, debugType);
        String currentAreaId = currentArea != null ? currentArea.getId() : "";
        String playerName = client.player.getName().getString();

        List<Line> lines = buildContentLines(client, state, currentArea, currentAreaId, playerName, tracker);

        int panelX    = screenW - PANEL_W - MARGIN_RIGHT;
        int panelY    = MARGIN_TOP;
        int titleRowH = LINE_H + PADDING;
        int contentY  = panelY + titleRowH + PADDING;
        int navY      = contentY + lines.size() * LINE_H + PADDING;

        String title = I18n.translate("debug.protectedareaclient.title");
        context.fill(panelX, panelY, panelX + PANEL_W, panelY + titleRowH, COLOR_TITLE_BG);
        int titleX = panelX + (PANEL_W - tr.getWidth("§l" + title)) / 2;
        context.drawText(tr, "§l" + title, titleX, panelY + PADDING / 2 + 1, COLOR_TITLE_FG, false);

        int y = contentY;
        for (Line line : lines) {
            context.drawText(tr, line.text, panelX + PADDING, y, line.color, true);
            y += LINE_H;
        }

        String nav = buildNavString(state);
        int navX = panelX + (PANEL_W - tr.getWidth(nav)) / 2;
        context.drawText(tr, nav, navX, navY, COLOR_NAV, true);
    }

    private static ProtectedArea resolveCurrentArea(MinecraftClient client, AreaTracker tracker, String debugType) {
        if (tracker == null || client.player == null || client.world == null) return null;
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        String worldName = getWorldName(client.world.getRegistryKey());
        String dimension = getDimensionKey(client.world.getRegistryKey());
        for (ProtectedArea area : tracker.getAreas().values()) {
            if ("flat".equals(debugType)) {
                if (area.isFlat() && area.isWithinFlatBounds(x, y, z, worldName, dimension)) return area;
            } else {
                if (!area.isFlat() && area.isInside(x, y, z, worldName, dimension)) return area;
            }
        }
        return null;
    }

    private static List<Line> buildContentLines(MinecraftClient client, DebugState state, ProtectedArea area,
                                                String currentAreaId, String playerName, AreaTracker tracker) {
        List<Line> lines = new ArrayList<>();
        String debugType = state.getDebugType();
        boolean isFlat = "flat".equals(debugType);

        if (currentAreaId.isEmpty()) {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.area_none"), COLOR_NO_AREA));
        } else {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.area_prefix") + currentAreaId, COLOR_AREA_ID));
        }
        lines.add(new Line("Type: " + debugType, isFlat ? COLOR_TYPE_FLAT : COLOR_TYPE_CUBE));
        lines.add(new Line("──────────────────────────", COLOR_SEP));

        if (isFlat) {
            buildOverview(lines, client, tracker, true, area);
        } else {
            switch (state.getCurrentPage()) {
                case DebugState.PAGE_OVERVIEW   -> buildOverview(lines, client, tracker, false, null);
                case DebugState.PAGE_RULES      -> buildRules(lines, state);
                case DebugState.PAGE_EXCEPTIONS -> buildExceptions(lines, state);
                case DebugState.PAGE_LIMIT      -> buildLimit(lines, state);
                case DebugState.PAGE_ADVANCED   -> buildAdvanced(lines, state);
            }
        }

        return lines;
    }

    private static void buildOverview(List<Line> lines, MinecraftClient client, AreaTracker tracker, boolean flatMode, ProtectedArea currentArea) {
        int total = 0;
        if (tracker != null) {
            for (ProtectedArea a : tracker.getAreas().values()) {
                if (flatMode ? a.isFlat() : !a.isFlat()) total++;
            }
        }
        lines.add(new Line(I18n.translate("debug.protectedareaclient.overview.total_areas") + total, COLOR_VALUE));

        if (client.player != null) {
            int x = (int) client.player.getX();
            int y = (int) client.player.getY();
            int z = (int) client.player.getZ();
            lines.add(new Line(I18n.translate("debug.protectedareaclient.overview.position") + x + ", " + y + ", " + z, COLOR_COORDS));

            if (flatMode && currentArea != null) {
                boolean positive = currentArea.getFlatSide(client.player.getX(), client.player.getY(), client.player.getZ());
                String axisName = switch (currentArea.getFlatAxis()) { case 0 -> "X"; case 1 -> "Y"; default -> "Z"; };
                String sideLabel = positive ? "Positivo (+)" : "Negativo (-)";
                lines.add(new Line("Lado " + axisName + ": " + sideLabel, positive ? COLOR_SIDE_POS : COLOR_SIDE_NEG));
                lines.add(new Line("Plano: " + String.format("%.2f", currentArea.getFlatPlaneCoord()), COLOR_VALUE));
            }
        }
    }

    private static void buildRules(List<Line> lines, DebugState state) {
        List<RuleEntry> rules = state.getRules();
        if (rules.isEmpty()) {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.rules.no_rules"), COLOR_NO_AREA));
            return;
        }
        for (RuleEntry rule : rules) {
            if (rule.hasException()) {
                lines.add(new Line("✔ " + rule.key() + I18n.translate("debug.protectedareaclient.rules.exception_suffix"), COLOR_RULE_EX));
            } else {
                lines.add(new Line("✔ " + rule.key(), COLOR_RULE_OK));
            }
        }
    }

    private static void buildExceptions(List<Line> lines, DebugState state) {
        List<ExceptionEntry> exceptions = state.getExceptions();
        if (exceptions.isEmpty()) {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.exceptions.none"), COLOR_NO_AREA));
            return;
        }
        String lastRule = null;
        for (ExceptionEntry ex : exceptions) {
            if (!ex.ruleKey().equals(lastRule)) {
                if (lastRule != null) lines.add(new Line(" ", COLOR_SEP));
                lines.add(new Line("[" + ex.ruleKey() + "]", COLOR_EX_RULE));
                lastRule = ex.ruleKey();
            }
            lines.add(new Line("  " + ex.playerName(), COLOR_EX_PLAYER));
        }
    }

    private static void buildLimit(List<Line> lines, DebugState state) {
        if (!state.isHasLimit()) {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.limit.none"), COLOR_NO_AREA));
            return;
        }

        int current = state.getLimitCurrent();
        int max     = state.getLimitMax();
        boolean full = current >= max;

        lines.add(new Line(I18n.translate("debug.protectedareaclient.limit.players") + current + " / " + max, full ? COLOR_LIMIT_WARN : COLOR_LIMIT_OK));

        if (state.isLimitBlocked()) {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.limit.status_blocked"), COLOR_LIMIT_WARN));
        } else {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.limit.status_open"), COLOR_LIMIT_OK));
        }

        if (state.isLimitException()) {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.limit.exception"), COLOR_RULE_EX));
        }
    }

    private static void buildAdvanced(List<Line> lines, DebugState state) {
        List<AdvancedEntry> entries = state.getAdvancedRules();
        if (entries.isEmpty()) {
            lines.add(new Line(I18n.translate("debug.protectedareaclient.advanced.none"), COLOR_NO_AREA));
            return;
        }
        String lastType = null;
        for (AdvancedEntry entry : entries) {
            if (!entry.ruleType().equals(lastType)) {
                if (lastType != null) lines.add(new Line(" ", COLOR_SEP));
                boolean isYes = entry.ruleType().startsWith("yes_");
                lines.add(new Line("[" + entry.ruleType() + "]", isYes ? COLOR_ADV_YES : COLOR_ADV_NO));
                lastType = entry.ruleType();
            }
            lines.add(new Line("  " + entry.targetType() + ": " + entry.targetId(), COLOR_VALUE));
        }
    }

    private static String buildNavString(DebugState state) {
        if ("flat".equals(state.getDebugType())) {
            return I18n.translate("debug.protectedareaclient.page.overview") + " (1/1) [↑ cube]";
        }
        int page  = state.getCurrentPage();
        int total = state.getTotalPages();
        String[] keys = {
                "debug.protectedareaclient.page.overview",
                "debug.protectedareaclient.page.rules",
                "debug.protectedareaclient.page.exceptions",
                "debug.protectedareaclient.page.limit",
                "debug.protectedareaclient.page.advanced"
        };
        String name = (page < keys.length) ? I18n.translate(keys[page]) : "Page " + (page + 1);
        return name + " (" + (page + 1) + "/" + total + ") [↑ flat]";
    }

    private static String getWorldName(net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey) {
        String path = worldKey.getValue().getPath();
        if (worldKey == net.minecraft.world.World.OVERWORLD || path.equals("overworld")) return "world";
        else if (worldKey == net.minecraft.world.World.NETHER || path.equals("the_nether")) return "world_nether";
        else if (worldKey == net.minecraft.world.World.END    || path.equals("the_end"))    return "world_the_end";
        else return path;
    }

    private static String getDimensionKey(net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimension) {
        if (dimension == net.minecraft.world.World.NETHER) return "minecraft:the_nether";
        else if (dimension == net.minecraft.world.World.END) return "minecraft:the_end";
        else return "minecraft:overworld";
    }

    private record Line(String text, int color) {}
}