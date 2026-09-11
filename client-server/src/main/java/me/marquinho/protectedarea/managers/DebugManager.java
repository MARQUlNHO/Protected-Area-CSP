package me.marquinho.protectedarea.managers;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.AdvancedAreaRules;
import me.marquinho.protectedarea.models.AdvancedRuleType;
import me.marquinho.protectedarea.models.AreaRule;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.network.ProtectedAreaPayload;
import me.marquinho.protectedarea.util.SchedulerUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DebugManager {

    private final ProtectedAreaInit plugin;
    private final Map<UUID, Session> activeSessions = new HashMap<>();

    public static final int PAGE_OVERVIEW   = 0;
    public static final int PAGE_RULES      = 1;
    public static final int PAGE_EXCEPTIONS = 2;
    public static final int PAGE_LIMIT      = 3;
    public static final int PAGE_ADVANCED   = 4;
    public static final int TOTAL_PAGES     = 5;

    public static final int DIM_PAGE_OVERVIEW   = 0;
    public static final int DIM_PAGE_RULES      = 1;
    public static final int DIM_PAGE_EXCEPTIONS = 2;
    public static final int DIM_PAGE_ADVANCED   = 3;
    public static final int DIMENSION_TOTAL_PAGES = 4;

    public static final String SCOPE_CUBE      = "cube";
    public static final String SCOPE_DIMENSION = "dimension";

    private record Session(int page, String scope) {}

    public DebugManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        SchedulerUtil.runTimer(this::refreshActiveSessions, 20, 20);
    }

    public void enableDebug(ServerPlayerEntity target) {
        activeSessions.put(target.getUuid(), new Session(PAGE_OVERVIEW, SCOPE_CUBE));
        sendPage(target, PAGE_OVERVIEW, SCOPE_CUBE);
    }

    public void disableDebug(ServerPlayerEntity target) {
        activeSessions.remove(target.getUuid());
        sendClose(target);
    }

    public boolean isActive(UUID uuid) { return activeSessions.containsKey(uuid); }
    public void removeSession(UUID uuid) { activeSessions.remove(uuid); }

    public void sendPage(ServerPlayerEntity player, int page) {
        sendPage(player, page, SCOPE_CUBE);
    }

    public void sendPage(ServerPlayerEntity player, int page, String scope) {
        if (!activeSessions.containsKey(player.getUuid())) return;
        String resolvedScope = SCOPE_DIMENSION.equals(scope) ? SCOPE_DIMENSION : SCOPE_CUBE;
        activeSessions.put(player.getUuid(), new Session(page, resolvedScope));

        String dim = player.getServerWorld().getRegistryKey().getValue().toString();

        if (SCOPE_DIMENSION.equals(resolvedScope)) {
            ProtectedArea area = plugin.getAreaManager().getDimensionArea(dim);
            String areaId = area != null ? area.getId() : "";
            switch (page) {
                case DIM_PAGE_RULES      -> sendRulesPage(player, area, areaId, resolvedScope, DIMENSION_TOTAL_PAGES, DIM_PAGE_RULES);
                case DIM_PAGE_EXCEPTIONS -> sendExceptionsPage(player, area, areaId, resolvedScope, DIMENSION_TOTAL_PAGES, DIM_PAGE_EXCEPTIONS);
                case DIM_PAGE_ADVANCED   -> sendAdvancedPage(player, area, areaId, resolvedScope, DIMENSION_TOTAL_PAGES, DIM_PAGE_ADVANCED);
                default                  -> sendDimensionOverviewPage(player, area, areaId, dim);
            }
            return;
        }

        ProtectedArea currentArea = plugin.getAreaManager().getCubeAreaAt(dim, player.getX(), player.getY(), player.getZ());
        String currentAreaId = currentArea != null ? currentArea.getId() : "";

        switch (page) {
            case PAGE_RULES      -> sendRulesPage(player, currentArea, currentAreaId, resolvedScope, TOTAL_PAGES, PAGE_RULES);
            case PAGE_EXCEPTIONS -> sendExceptionsPage(player, currentArea, currentAreaId, resolvedScope, TOTAL_PAGES, PAGE_EXCEPTIONS);
            case PAGE_LIMIT      -> sendLimitPage(player, currentArea, currentAreaId, resolvedScope);
            case PAGE_ADVANCED   -> sendAdvancedPage(player, currentArea, currentAreaId, resolvedScope, TOTAL_PAGES, PAGE_ADVANCED);
            default              -> sendOverviewPage(player, currentAreaId, resolvedScope);
        }
    }

    public void refreshActiveSessions() {
        for (Map.Entry<UUID, Session> entry : activeSessions.entrySet()) {
            ServerPlayerEntity player = plugin.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) sendPage(player, entry.getValue().page(), entry.getValue().scope());
        }
    }

    private DataOutputStream header(ByteArrayOutputStream bos, int page, int totalPages,
                                    String areaId, String scope) throws IOException {
        DataOutputStream out = new DataOutputStream(bos);
        out.writeUTF("DEBUG_DATA");
        out.writeInt(page);
        out.writeInt(totalPages);
        out.writeUTF(areaId);
        out.writeUTF(scope);
        return out;
    }

    private void sendOverviewPage(ServerPlayerEntity player, String currentAreaId, String scope) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, PAGE_OVERVIEW, TOTAL_PAGES, currentAreaId, scope);
            out.writeInt(plugin.getAreaManager().getAreas().size());
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendDimensionOverviewPage(ServerPlayerEntity player, ProtectedArea area,
                                           String areaId, String dimensionKey) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, DIM_PAGE_OVERVIEW, DIMENSION_TOTAL_PAGES, areaId, SCOPE_DIMENSION);
            out.writeInt(plugin.getAreaManager().countDimensionAreas());
            out.writeUTF(dimensionKey);
            out.writeInt(area != null ? area.getPriority() : 0);
            out.writeUTF(area != null && area.hasSkybox() ? area.getSkybox() : "");
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendRulesPage(ServerPlayerEntity player, ProtectedArea area, String currentAreaId,
                               String scope, int totalPages, int page) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, page, totalPages, currentAreaId, scope);
            if (area == null) {
                out.writeInt(0);
            } else {
                String playerName = player.getGameProfile().getName();
                Set<AreaRule> rules = area.getRules();
                out.writeInt(rules.size());
                for (AreaRule rule : rules) {
                    out.writeUTF(rule.getKey());
                    out.writeBoolean(area.hasException(playerName, rule.getKey()));
                }
            }
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendExceptionsPage(ServerPlayerEntity player, ProtectedArea area, String currentAreaId,
                                    String scope, int totalPages, int page) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, page, totalPages, currentAreaId, scope);
            if (area == null) {
                out.writeInt(0);
            } else {
                Map<String, Set<String>> allEx = area.getAllExceptions();
                int total = allEx.values().stream().mapToInt(Set::size).sum();
                out.writeInt(total);
                for (Map.Entry<String, Set<String>> entry : allEx.entrySet()) {
                    for (String name : entry.getValue()) {
                        out.writeUTF(entry.getKey());
                        out.writeUTF(name);
                    }
                }
            }
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendLimitPage(ServerPlayerEntity player, ProtectedArea area, String currentAreaId, String scope) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, PAGE_LIMIT, TOTAL_PAGES, currentAreaId, scope);
            boolean hasLimit = area != null && area.hasPlayerLimit();
            out.writeBoolean(hasLimit);
            if (hasLimit) {
                int current = plugin.getAreaManager().getPlayersInArea(area.getId());
                out.writeInt(area.getPlayerLimit());
                out.writeInt(current);
                out.writeBoolean(area.isLimitBlocked());
                out.writeBoolean(area.hasException(player.getGameProfile().getName(), "limit"));
            }
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendAdvancedPage(ServerPlayerEntity player, ProtectedArea area, String currentAreaId,
                                  String scope, int totalPages, int page) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, page, totalPages, currentAreaId, scope);
            if (area == null) {
                out.writeInt(0);
            } else {
                AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getStorageKey());
                List<String[]> entries = new ArrayList<>();
                for (AdvancedRuleType rt : AdvancedRuleType.values()) {
                    for (String blockId : adv.getBlocks(rt)) entries.add(new String[]{rt.getKey(), "block", blockId});
                    for (String entityId : adv.getEntities(rt)) entries.add(new String[]{rt.getKey(), "entity", entityId});
                }
                out.writeInt(entries.size());
                for (String[] e : entries) { out.writeUTF(e[0]); out.writeUTF(e[1]); out.writeUTF(e[2]); }
            }
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendClose(ServerPlayerEntity player) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_CLOSE");
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }
}
