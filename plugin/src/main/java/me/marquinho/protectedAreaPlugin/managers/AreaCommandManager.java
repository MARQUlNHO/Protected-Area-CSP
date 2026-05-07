package me.marquinho.protectedAreaPlugin.managers;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.AreaCommandEntry;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class AreaCommandManager {

    private final ProtectedAreaPlugin plugin;

    public AreaCommandManager(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;

        File cacheFolder = getCacheFolder();
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }
    }

    private File getCacheFolder() {
        return new File(plugin.getDataFolder(), "Cache");
    }

    private File getCacheFile(String playerName) {
        return new File(getCacheFolder(), playerName + ".yml");
    }

    private NamespacedKey buildKey(String areaId, String type, int index) {
        String safe = (areaId + "_" + type + "_" + index)
                .toLowerCase()
                .replaceAll("[^a-z0-9_\\-.]", "_");
        return new NamespacedKey(plugin, "uses_" + safe);
    }

    private String buildCacheKey(String areaId, String type, int index) {
        return (areaId + "_" + type + "_" + index)
                .toLowerCase()
                .replaceAll("[^a-z0-9_\\-.]", "_");
    }

    public int getUses(Player player, String areaId, String type, int index) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = buildKey(areaId, type, index);
        return pdc.getOrDefault(key, PersistentDataType.INTEGER, -1);
    }

    public void setUses(Player player, String areaId, String type, int index, int value) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = buildKey(areaId, type, index);
        pdc.set(key, PersistentDataType.INTEGER, value);
    }

    public void saveCache(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        boolean hasAny = false;
        YamlConfiguration cache = new YamlConfiguration();

        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals(plugin.getName().toLowerCase())) continue;
            Integer value = pdc.get(key, PersistentDataType.INTEGER);
            if (value == null) continue;
            cache.set(key.getKey(), value);
            hasAny = true;
        }

        if (!hasAny) return;

        File file = getCacheFile(player.getName());
        try {
            cache.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar cache de " + player.getName());
            e.printStackTrace();
        }
    }

    public void loadCache(Player player) {
        File file = getCacheFile(player.getName());
        if (!file.exists()) return;

        YamlConfiguration cache = YamlConfiguration.loadConfiguration(file);
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        for (String keyStr : cache.getKeys(false)) {
            int value = cache.getInt(keyStr);
            NamespacedKey key = new NamespacedKey(plugin, keyStr);
            pdc.set(key, PersistentDataType.INTEGER, value);
        }

        file.delete();
    }

    public void shiftUsesAfterRemove(String areaId, String type, int removedIndex, int totalBefore) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            shiftPdc(player, areaId, type, removedIndex, totalBefore);
        }

        File cacheFolder = getCacheFolder();
        File[] files = cacheFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            shiftCacheFile(file, areaId, type, removedIndex, totalBefore);
        }
    }

    private void shiftPdc(Player player, String areaId, String type, int removedIndex, int totalBefore) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        for (int i = removedIndex; i < totalBefore - 1; i++) {
            NamespacedKey from = buildKey(areaId, type, i + 1);
            NamespacedKey to   = buildKey(areaId, type, i);

            Integer value = pdc.get(from, PersistentDataType.INTEGER);
            if (value != null) {
                pdc.set(to, PersistentDataType.INTEGER, value);
            } else {
                pdc.remove(to);
            }
        }

        NamespacedKey last = buildKey(areaId, type, totalBefore - 1);
        pdc.remove(last);
    }

    private void shiftCacheFile(File file, String areaId, String type, int removedIndex, int totalBefore) {
        YamlConfiguration cache = YamlConfiguration.loadConfiguration(file);
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
        if (cache.contains(last)) {
            cache.set(last, null);
            modified = true;
        }

        if (modified) {
            try {
                cache.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Error al actualizar cache: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    public void triggerCommands(Player player, String areaId, boolean isEntry) {
        ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
        if (area == null) return;

        List<AreaCommandEntry> commands = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (commands.isEmpty()) return;

        String type = isEntry ? "entry" : "exit";

        for (int i = 0; i < commands.size(); i++) {
            AreaCommandEntry entry = commands.get(i);

            if (entry.getMaxUses() == -1) {
            } else {
                int currentUses = getUses(player, areaId, type, i);

                if (currentUses == -1) {
                    currentUses = entry.getMaxUses();
                }

                if (currentUses <= 0) continue;

                setUses(player, areaId, type, i, currentUses - 1);
            }

            final String cmd = entry.buildCommand(
                    player.getName(),
                    player.getLocation().getX(),
                    player.getLocation().getY(),
                    player.getLocation().getZ()
            );

            if (entry.getDelayTicks() <= 0) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            } else {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
                    }
                }, entry.getDelayTicks());
            }
        }
    }
}