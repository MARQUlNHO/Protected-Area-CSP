package me.marquinho.protectedareaserver.managers;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.models.AdvancedAreaRules;
import me.marquinho.protectedareaserver.models.AdvancedRuleType;
import me.marquinho.protectedareaserver.models.AreaRule;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.network.ProtectedAreaPayload;
import me.marquinho.protectedareaserver.util.SchedulerUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.*;
import java.util.*;

public class DebugManager {

    private final ProtectedAreaInit plugin;
    private final Map<UUID, Integer> activeSessions = new HashMap<>();

    public static final int PAGE_OVERVIEW   = 0;
    public static final int PAGE_RULES      = 1;
    public static final int PAGE_EXCEPTIONS = 2;
    public static final int PAGE_LIMIT      = 3;
    public static final int PAGE_ADVANCED   = 4;
    public static final int TOTAL_PAGES     = 5;

    public DebugManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        SchedulerUtil.runTimer(this::refreshActiveSessions, 20, 20);
    }

    public void enableDebug(ServerPlayerEntity target) {
        activeSessions.put(target.getUuid(), PAGE_OVERVIEW);
        sendPage(target, PAGE_OVERVIEW);
    }

    public void disableDebug(ServerPlayerEntity target) {
        activeSessions.remove(target.getUuid());
        sendClose(target);
    }

    public boolean isActive(UUID uuid) { return activeSessions.containsKey(uuid); }
    public void removeSession(UUID uuid) { activeSessions.remove(uuid); }

    public void sendPage(ServerPlayerEntity player, int page) {
        if (!activeSessions.containsKey(player.getUuid())) return;
        activeSessions.put(player.getUuid(), page);

        String dim = player.getServerWorld().getRegistryKey().getValue().toString();
        ProtectedArea currentArea = plugin.getAreaManager().getAreaAt(dim, player.getX(), player.getY(), player.getZ());
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
            ServerPlayerEntity player = plugin.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) sendPage(player, entry.getValue());
        }
    }

    private void sendOverviewPage(ServerPlayerEntity player, String currentAreaId) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeUTF("DEBUG_DATA");
            out.writeInt(PAGE_OVERVIEW);
            out.writeInt(TOTAL_PAGES);
            out.writeUTF(currentAreaId);
            out.writeInt(plugin.getAreaManager().getAreas().size());
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendRulesPage(ServerPlayerEntity player, ProtectedArea area, String currentAreaId) {
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

    private void sendExceptionsPage(ServerPlayerEntity player, ProtectedArea area, String currentAreaId) {
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

    private void sendLimitPage(ServerPlayerEntity player, ProtectedArea area, String currentAreaId) {
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
                out.writeBoolean(area.hasException(player.getGameProfile().getName(), "limit"));
            }
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(bos.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendAdvancedPage(ServerPlayerEntity player, ProtectedArea area, String currentAreaId) {
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
                AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getId());
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
