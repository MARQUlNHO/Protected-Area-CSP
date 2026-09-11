package me.marquinho.protectedarea.managers;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.AreaCommandEntry;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.util.SchedulerUtil;
import me.marquinho.protectedarea.util.SimpleYaml;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AreaCommandManager {

    private final ProtectedAreaInit plugin;
    private final Map<UUID, Map<String, Integer>> usesCache = new HashMap<>();

    public AreaCommandManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        new File(plugin.getDataPath(), "Cache").mkdirs();
    }


    private String buildCacheKey(String areaId, String type, int index) {
        return (areaId + "_" + type + "_" + index)
                .toLowerCase()
                .replaceAll("[^a-z0-9_\\-.]", "_");
    }


    public int getUses(ServerPlayerEntity player, String areaId, String type, int index) {
        Map<String, Integer> map = usesCache.get(player.getUuid());
        if (map == null) return -1;
        return map.getOrDefault(buildCacheKey(areaId, type, index), -1);
    }

    public void setUses(ServerPlayerEntity player, String areaId, String type, int index, int value) {
        usesCache.computeIfAbsent(player.getUuid(), k -> new HashMap<>())
                 .put(buildCacheKey(areaId, type, index), value);
    }


    public void saveCache(ServerPlayerEntity player) {
        Map<String, Integer> map = usesCache.get(player.getUuid());
        if (map == null || map.isEmpty()) return;

        File file = new File(plugin.getDataPath(), "Cache/" + player.getGameProfile().getName() + ".yml");
        SimpleYaml cache = new SimpleYaml();
        for (Map.Entry<String, Integer> e : map.entrySet()) cache.set(e.getKey(), e.getValue());

        try { cache.save(file); }
        catch (IOException e) { plugin.getLogger().error("Error saving cache for " + player.getGameProfile().getName(), e); }
    }

    public void loadCache(ServerPlayerEntity player) {
        File file = new File(plugin.getDataPath(), "Cache/" + player.getGameProfile().getName() + ".yml");
        if (!file.exists()) return;

        SimpleYaml cache = SimpleYaml.load(file);
        Map<String, Integer> map = usesCache.computeIfAbsent(player.getUuid(), k -> new HashMap<>());
        for (String key : cache.getKeys()) {
            map.put(key, cache.getInt(key));
        }
        file.delete();
    }

    public void removeCache(UUID uuid) {
        usesCache.remove(uuid);
    }

    public int applyUsesToOfflineCaches(String areaId, String type, int index, int amount, boolean set, int maxUses) {
        File cacheFolder = new File(plugin.getDataPath(), "Cache");
        File[] files = cacheFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return 0;

        String key = buildCacheKey(areaId, type, index);
        int modified = 0;

        for (File file : files) {
            SimpleYaml cache = SimpleYaml.load(file);

            int newValue;
            if (set) {
                newValue = amount;
            } else {
                int current = cache.contains(key) ? cache.getInt(key) : maxUses;
                newValue = current + amount;
            }
            cache.set(key, newValue);

            try {
                cache.save(file);
                modified++;
            } catch (IOException e) {
                plugin.getLogger().error("Error updating cache: " + file.getName(), e);
            }
        }
        return modified;
    }


    public void shiftUsesAfterRemove(String areaId, String type, int removedIndex, int totalBefore) {
        for (Map.Entry<UUID, Map<String, Integer>> entry : usesCache.entrySet()) {
            shiftMap(entry.getValue(), areaId, type, removedIndex, totalBefore);
        }

        File cacheFolder = new File(plugin.getDataPath(), "Cache");
        File[] files = cacheFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            SimpleYaml cache = SimpleYaml.load(file);
            boolean modified = false;

            for (int i = removedIndex; i < totalBefore - 1; i++) {
                String from = buildCacheKey(areaId, type, i + 1);
                String to   = buildCacheKey(areaId, type, i);
                if (cache.contains(from)) {
                    cache.set(to, cache.getInt(from));
                    modified = true;
                } else {
                    cache.set(to, null);
                }
            }
            String last = buildCacheKey(areaId, type, totalBefore - 1);
            if (cache.contains(last)) { cache.set(last, null); modified = true; }

            if (modified) {
                try { cache.save(file); }
                catch (IOException e) { plugin.getLogger().error("Error updating cache: " + file.getName(), e); }
            }
        }
    }

    private void shiftMap(Map<String, Integer> map, String areaId, String type, int removedIndex, int totalBefore) {
        for (int i = removedIndex; i < totalBefore - 1; i++) {
            String from = buildCacheKey(areaId, type, i + 1);
            String to   = buildCacheKey(areaId, type, i);
            Integer value = map.get(from);
            if (value != null) map.put(to, value);
            else map.remove(to);
        }
        map.remove(buildCacheKey(areaId, type, totalBefore - 1));
    }


    public void triggerCommands(ServerPlayerEntity player, String areaId, boolean isEntry) {
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) return;

        List<AreaCommandEntry> commands = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (commands.isEmpty()) return;

        String type = isEntry ? "entry" : "exit";

        for (int i = 0; i < commands.size(); i++) {
            AreaCommandEntry entry = commands.get(i);

            if (entry.getMaxUses() != -1) {
                int currentUses = getUses(player, areaId, type, i);
                if (currentUses == -1) currentUses = entry.getMaxUses();
                if (currentUses <= 0) continue;
                setUses(player, areaId, type, i, currentUses - 1);
            }

            final String cmd = entry.buildCommand(
                    player.getGameProfile().getName(),
                    player.getX(), player.getY(), player.getZ()
            );

            if (entry.getDelayTicks() <= 0) {
                plugin.getServer().getCommandManager()
                      .executeWithPrefix(plugin.getServer().getCommandSource(), cmd);
            } else {
                SchedulerUtil.runLater(() -> {
                    if (plugin.getServer().getPlayerManager().getPlayer(player.getUuid()) != null) {
                        plugin.getServer().getCommandManager()
                              .executeWithPrefix(plugin.getServer().getCommandSource(), cmd);
                    }
                }, entry.getDelayTicks());
            }
        }
    }
}
