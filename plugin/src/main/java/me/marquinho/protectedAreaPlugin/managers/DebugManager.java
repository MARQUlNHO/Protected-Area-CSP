package me.marquinho.protectedAreaPlugin.managers;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.AdvancedAreaRules;
import me.marquinho.protectedAreaPlugin.models.AdvancedRuleType;
import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class DebugManager {

    private final ProtectedAreaPlugin plugin;
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

    public DebugManager(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshActiveSessions, 20L, 20L);
    }

    public void enableDebug(Player target) {
        activeSessions.put(target.getUniqueId(), new Session(PAGE_OVERVIEW, SCOPE_CUBE));
        sendPage(target, PAGE_OVERVIEW, SCOPE_CUBE);
    }

    public void disableDebug(Player target) {
        activeSessions.remove(target.getUniqueId());
        sendClose(target);
    }

    public boolean isActive(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
    }

    public void sendPage(Player player, int page) {
        sendPage(player, page, SCOPE_CUBE);
    }

    public void sendPage(Player player, int page, String scope) {
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        String resolvedScope = SCOPE_DIMENSION.equals(scope) ? SCOPE_DIMENSION : SCOPE_CUBE;
        activeSessions.put(player.getUniqueId(), new Session(page, resolvedScope));

        if (SCOPE_DIMENSION.equals(resolvedScope)) {
            String dimensionKey = player.getWorld().getKey().toString();
            ProtectedArea area = plugin.getAreaManager().getDimensionArea(dimensionKey);
            String areaId = area != null ? area.getId() : "";
            switch (page) {
                case DIM_PAGE_RULES      -> sendRulesPage(player, area, areaId, resolvedScope, DIMENSION_TOTAL_PAGES, DIM_PAGE_RULES);
                case DIM_PAGE_EXCEPTIONS -> sendExceptionsPage(player, area, areaId, resolvedScope, DIMENSION_TOTAL_PAGES, DIM_PAGE_EXCEPTIONS);
                case DIM_PAGE_ADVANCED   -> sendAdvancedPage(player, area, areaId, resolvedScope, DIMENSION_TOTAL_PAGES, DIM_PAGE_ADVANCED);
                default                  -> sendDimensionOverviewPage(player, area, areaId, dimensionKey);
            }
            return;
        }

        ProtectedArea currentArea = plugin.getAreaManager().getCubeAreaAt(player.getLocation());
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
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                sendPage(player, entry.getValue().page(), entry.getValue().scope());
            }
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

    private void send(Player player, ByteArrayOutputStream bos) {
        player.sendPluginMessage(plugin, "protectedarea:main", bos.toByteArray());
    }

    private void sendOverviewPage(Player player, String currentAreaId, String scope) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, PAGE_OVERVIEW, TOTAL_PAGES, currentAreaId, scope);
            out.writeInt(plugin.getAreaManager().getAreas().size());
            send(player, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDimensionOverviewPage(Player player, ProtectedArea area, String areaId, String dimensionKey) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, DIM_PAGE_OVERVIEW, DIMENSION_TOTAL_PAGES, areaId, SCOPE_DIMENSION);
            out.writeInt(plugin.getAreaManager().countDimensionAreas());
            out.writeUTF(dimensionKey);
            out.writeInt(area != null ? area.getPriority() : 0);
            out.writeUTF(area != null && area.hasSkybox() ? area.getSkybox() : "");
            send(player, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRulesPage(Player player, ProtectedArea area, String currentAreaId,
                               String scope, int totalPages, int page) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, page, totalPages, currentAreaId, scope);

            if (area == null) {
                out.writeInt(0);
            } else {
                Set<AreaRule> rules = area.getRules();
                out.writeInt(rules.size());
                for (AreaRule rule : rules) {
                    out.writeUTF(rule.getKey());
                    out.writeBoolean(area.hasException(player.getName(), rule.getKey()));
                }
            }

            send(player, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendExceptionsPage(Player player, ProtectedArea area, String currentAreaId,
                                    String scope, int totalPages, int page) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, page, totalPages, currentAreaId, scope);

            if (area == null) {
                out.writeInt(0);
            } else {
                Map<String, Set<String>> allExceptions = area.getAllExceptions();
                int totalEntries = allExceptions.values().stream().mapToInt(Set::size).sum();
                out.writeInt(totalEntries);
                for (Map.Entry<String, Set<String>> entry : allExceptions.entrySet()) {
                    for (String playerName : entry.getValue()) {
                        out.writeUTF(entry.getKey());
                        out.writeUTF(playerName);
                    }
                }
            }

            send(player, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLimitPage(Player player, ProtectedArea area, String currentAreaId, String scope) {
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
                out.writeBoolean(area.hasException(player.getName(), "limit"));
            }

            send(player, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAdvancedPage(Player player, ProtectedArea area, String currentAreaId,
                                  String scope, int totalPages, int page) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = header(bos, page, totalPages, currentAreaId, scope);

            if (area == null) {
                out.writeInt(0);
            } else {
                AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getStorageKey());
                List<String[]> entries = new ArrayList<>();

                for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
                    for (String blockId : advancedRules.getBlocks(ruleType)) {
                        entries.add(new String[]{ruleType.getKey(), "block", blockId});
                    }
                    for (String entityId : advancedRules.getEntities(ruleType)) {
                        entries.add(new String[]{ruleType.getKey(), "entity", entityId});
                    }
                }

                out.writeInt(entries.size());
                for (String[] entry : entries) {
                    out.writeUTF(entry[0]);
                    out.writeUTF(entry[1]);
                    out.writeUTF(entry[2]);
                }
            }

            send(player, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendClose(Player player) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_CLOSE");
            send(player, bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
