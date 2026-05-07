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
    private final Map<UUID, Integer> activeSessions = new HashMap<>();

    public static final int PAGE_OVERVIEW   = 0;
    public static final int PAGE_RULES      = 1;
    public static final int PAGE_EXCEPTIONS = 2;
    public static final int PAGE_LIMIT      = 3;
    public static final int PAGE_ADVANCED   = 4;
    public static final int TOTAL_PAGES     = 5;

    public DebugManager(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshActiveSessions, 20L, 20L);
    }

    public void enableDebug(Player target) {
        activeSessions.put(target.getUniqueId(), PAGE_OVERVIEW);
        sendPage(target, PAGE_OVERVIEW);
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
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        activeSessions.put(player.getUniqueId(), page);

        ProtectedArea currentArea = plugin.getAreaManager().getAreaAt(player.getLocation());
        String currentAreaId = currentArea != null ? currentArea.getId() : "";

        switch (page) {
            case PAGE_OVERVIEW   -> sendOverviewPage(player, currentAreaId);
            case PAGE_RULES      -> sendRulesPage(player, currentArea, currentAreaId);
            case PAGE_EXCEPTIONS -> sendExceptionsPage(player, currentArea, currentAreaId);
            case PAGE_LIMIT      -> sendLimitPage(player, currentArea, currentAreaId);
            case PAGE_ADVANCED   -> sendAdvancedPage(player, currentArea, currentAreaId);
            default              -> sendOverviewPage(player, currentAreaId);
        }
    }

    public void refreshActiveSessions() {
        for (Map.Entry<UUID, Integer> entry : activeSessions.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                sendPage(player, entry.getValue());
            }
        }
    }

    private void sendOverviewPage(Player player, String currentAreaId) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_DATA");
            out.writeInt(PAGE_OVERVIEW);
            out.writeInt(TOTAL_PAGES);
            out.writeUTF(currentAreaId);
            out.writeInt(plugin.getAreaManager().getAreas().size());
            player.sendPluginMessage(plugin, "protectedarea:main", bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRulesPage(Player player, ProtectedArea area, String currentAreaId) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_DATA");
            out.writeInt(PAGE_RULES);
            out.writeInt(TOTAL_PAGES);
            out.writeUTF(currentAreaId);

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

            player.sendPluginMessage(plugin, "protectedarea:main", bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendExceptionsPage(Player player, ProtectedArea area, String currentAreaId) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_DATA");
            out.writeInt(PAGE_EXCEPTIONS);
            out.writeInt(TOTAL_PAGES);
            out.writeUTF(currentAreaId);

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

            player.sendPluginMessage(plugin, "protectedarea:main", bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLimitPage(Player player, ProtectedArea area, String currentAreaId) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_DATA");
            out.writeInt(PAGE_LIMIT);
            out.writeInt(TOTAL_PAGES);
            out.writeUTF(currentAreaId);

            boolean hasLimit = area != null && area.hasPlayerLimit();
            out.writeBoolean(hasLimit);

            if (hasLimit) {
                int current = plugin.getAreaManager().getPlayersInArea(area.getId());
                out.writeInt(area.getPlayerLimit());
                out.writeInt(current);
                out.writeBoolean(area.isLimitBlocked());
                out.writeBoolean(area.hasException(player.getName(), "limit"));
            }

            player.sendPluginMessage(plugin, "protectedarea:main", bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAdvancedPage(Player player, ProtectedArea area, String currentAreaId) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_DATA");
            out.writeInt(PAGE_ADVANCED);
            out.writeInt(TOTAL_PAGES);
            out.writeUTF(currentAreaId);

            if (area == null) {
                out.writeInt(0);
            } else {
                AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());
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

            player.sendPluginMessage(plugin, "protectedarea:main", bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendClose(Player player) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_CLOSE");
            player.sendPluginMessage(plugin, "protectedarea:main", bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}