package me.marquinho.protectedarea.managers;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.api.ProtectedAreaServerEvents;
import me.marquinho.protectedarea.models.*;
import me.marquinho.protectedarea.models.ProtectedArea;
import me.marquinho.protectedarea.network.ProtectedAreaPayload;
import me.marquinho.protectedarea.util.SimpleYaml;
import me.marquinho.protectedarea.util.WorldDimensionUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class AreaManager {

    private final ProtectedAreaInit plugin;
    private final Map<String, ProtectedArea> areas;

    public AreaManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        this.areas = new HashMap<>();
        try {
            Files.createDirectories(new File(plugin.getDataPath(), "Areas").toPath());
            Files.createDirectories(new File(plugin.getDataPath(), "Areas/.flat").toPath());
            Files.createDirectories(new File(plugin.getDataPath(), "Areas/.dimension").toPath());
        } catch (IOException e) {
            plugin.getLogger().error("Could not create the Areas directory", e);
        }
    }


    public void loadAllAreas() {
        areas.clear();
        File areasFolder = new File(plugin.getDataPath(), "Areas");
        if (!areasFolder.exists()) {
            try { Files.createDirectories(areasFolder.toPath()); } catch (IOException ignored) {}
            return;
        }

        loadAreasRecursively(areasFolder, areasFolder, true);

        File flatFolder = new File(areasFolder, ".flat");
        if (flatFolder.exists()) {
            loadAreasRecursively(flatFolder, flatFolder, false);
        }

        File dimensionFolder = new File(areasFolder, ".dimension");
        if (dimensionFolder.exists()) {
            loadAreasRecursively(dimensionFolder, dimensionFolder, false);
        }

        plugin.getLogger().info("Loaded " + areas.size() + " protected areas");
    }

    private void loadAreasRecursively(File baseFolder, File currentFolder, boolean skipDotDirs) {
        File[] entries = currentFolder.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            if (entry.isDirectory()) {
                if (skipDotDirs && entry.getName().startsWith(".")) continue;
                loadAreasRecursively(baseFolder, entry, false);
            } else if (entry.getName().endsWith(".yml")) {
                String relativePath = baseFolder.toPath().relativize(entry.toPath()).toString();
                String id = relativePath.replace(File.separator, "/").replace(".yml", "");
                loadArea(entry, id);
            }
        }
    }

    private void loadArea(File file, String id) {
        SimpleYaml config = SimpleYaml.load(file);
        String dimension = config.getString("dimension");

        String type = config.contains("type") ? config.getString("type") : null;
        boolean dimensionArea = "dimension".equals(type);

        int x1 = 0, y1 = 0, z1 = 0, x2 = 0, y2 = 0, z2 = 0;
        if (!dimensionArea) {
            if (config.contains("xyz1")) {
                int[] c1 = parseXyz(config.getString("xyz1"));
                int[] c2 = parseXyz(config.getString("xyz2"));
                x1 = c1[0]; y1 = c1[1]; z1 = c1[2];
                x2 = c2[0]; y2 = c2[1]; z2 = c2[2];
            } else {
                x1 = config.getInt("x1"); y1 = config.getInt("y1"); z1 = config.getInt("z1");
                x2 = config.getInt("x2"); y2 = config.getInt("y2"); z2 = config.getInt("z2");
            }
        }

        ProtectedArea area = new ProtectedArea(id, dimension, x1, y1, z1, x2, y2, z2);
        if (type != null) area.setType(type);

        if (config.contains("color")) area.setColor(config.getString("color"));
        if (config.contains("alias")) area.setAlias(config.getString("alias"));
        if (config.contains("priority")) area.setPriority(config.getInt("priority", 0));
        else if (dimensionArea) area.setPriority(-1);
        if (config.contains("playerLimit")) area.setPlayerLimit(config.getInt("playerLimit"));
        if (config.contains("limitBlocked")) area.setLimitBlocked(config.getBoolean("limitBlocked", false));

        if (config.contains("rules")) {
            Set<AreaRule> rules = new HashSet<>();
            for (String key : config.getStringList("rules")) {
                AreaRule rule = AreaRule.fromKey(key);
                if (rule != null) rules.add(rule);
            }
            area.setRules(rules);
        }

        if (config.contains("exceptions")) {
            SimpleYaml exceptions = config.getSection("exceptions");
            if (exceptions != null) {
                Map<String, Set<String>> exMap = new HashMap<>();
                for (String key : exceptions.getKeys()) {
                    List<String> players = config.getStringList("exceptions." + key);
                    exMap.put(key, new HashSet<>(players));
                }
                area.setExceptions(exMap);
            }
        }

        if (config.contains("commandEntry")) {
            List<AreaCommandEntry> entryCommands = new ArrayList<>();
            for (String raw : config.getStringList("commandEntry")) {
                AreaCommandEntry e = AreaCommandEntry.fromString(raw);
                if (e != null) entryCommands.add(e);
            }
            area.setEntryCommands(entryCommands);
        }

        if (config.contains("commandExit")) {
            List<AreaCommandEntry> exitCommands = new ArrayList<>();
            for (String raw : config.getStringList("commandExit")) {
                AreaCommandEntry e = AreaCommandEntry.fromString(raw);
                if (e != null) exitCommands.add(e);
            }
            area.setExitCommands(exitCommands);
        }

        if (config.contains("skybox")) area.setSkybox(config.getString("skybox"));
        if (!dimensionArea) {
            if (config.contains("flatPosition")) area.setFlatPosition(config.getInt("flatPosition", 0));
            area.setPassNegative(config.getBoolean("pass.negative", true));
            area.setPassPositive(config.getBoolean("pass.positive", true));
        }

        areas.put(id, area);
    }

    private static int[] parseXyz(String value) {
        if (value == null) return new int[]{0, 0, 0};
        String[] parts = value.split(",");
        return new int[]{
            parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0,
            parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0,
            parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0
        };
    }

    private void saveArea(ProtectedArea area) {
        if (area.isDimension()) {
            saveDimensionArea(area);
        } else if (area.isFlat()) {
            saveFlatArea(area);
        } else {
            saveCubeArea(area);
        }
    }

    private void saveDimensionArea(ProtectedArea area) {
        File dimensionFolder = new File(plugin.getDataPath(), "Areas/.dimension");
        File file = new File(dimensionFolder, area.getId().replace("/", File.separator) + ".yml");
        try { Files.createDirectories(file.getParentFile().toPath()); } catch (IOException ignored) {}
        SimpleYaml config = new SimpleYaml();

        config.set("id", area.getId());
        config.set("type", area.getType());
        config.set("dimension", area.getDimension());
        config.set("alias", area.getAlias());
        config.set("priority", area.getPriority());
        config.set("skybox", area.hasSkybox() ? area.getSkybox() : "");

        List<String> ruleKeys = new ArrayList<>();
        for (AreaRule rule : area.getRules()) ruleKeys.add(rule.getKey());
        config.set("rules", ruleKeys);

        Map<String, Set<String>> exceptions = area.getAllExceptions();
        for (Map.Entry<String, Set<String>> entry : exceptions.entrySet()) {
            config.set("exceptions." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().error("Error saving dimension area: " + area.getId(), e); }
    }

    private void saveFlatArea(ProtectedArea area) {
        File flatFolder = new File(plugin.getDataPath(), "Areas/.flat");
        File file = new File(flatFolder, area.getId().replace("/", File.separator) + ".yml");
        try { Files.createDirectories(file.getParentFile().toPath()); } catch (IOException ignored) {}
        SimpleYaml config = new SimpleYaml();

        config.set("id", area.getId());
        config.set("type", area.getType());
        config.set("flatPosition", area.getFlatPosition());
        config.set("pass.negative", area.isPassNegative());
        config.set("pass.positive", area.isPassPositive());
        config.set("dimension", area.getDimension());
        config.set("xyz1", area.getX1() + "," + area.getY1() + "," + area.getZ1());
        config.set("xyz2", area.getX2() + "," + area.getY2() + "," + area.getZ2());
        config.set("color", area.getColor());
        config.set("alias", area.getAlias());

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().error("Error saving flat area: " + area.getId(), e); }
    }

    private void saveCubeArea(ProtectedArea area) {
        File areasFolder = new File(plugin.getDataPath(), "Areas");
        File file = new File(areasFolder, area.getId().replace("/", File.separator) + ".yml");
        try { Files.createDirectories(file.getParentFile().toPath()); } catch (IOException ignored) {}
        SimpleYaml config = new SimpleYaml();

        config.set("id", area.getId());
        config.set("type", area.getType());
        config.set("skybox", area.hasSkybox() ? area.getSkybox() : "");
        config.set("dimension", area.getDimension());
        config.set("xyz1", area.getX1() + "," + area.getY1() + "," + area.getZ1());
        config.set("xyz2", area.getX2() + "," + area.getY2() + "," + area.getZ2());
        config.set("color", area.getColor());
        config.set("alias", area.getAlias());
        config.set("priority", area.getPriority());
        if (area.getPlayerLimit() != null) config.set("playerLimit", area.getPlayerLimit());
        config.set("limitBlocked", area.isLimitBlocked());

        List<String> ruleKeys = new ArrayList<>();
        for (AreaRule rule : area.getRules()) ruleKeys.add(rule.getKey());
        config.set("rules", ruleKeys);

        Map<String, Set<String>> exceptions = area.getAllExceptions();
        for (Map.Entry<String, Set<String>> entry : exceptions.entrySet()) {
            config.set("exceptions." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        List<String> entryStrs = new ArrayList<>();
        for (AreaCommandEntry e : area.getEntryCommands()) entryStrs.add(e.toYmlString());
        if (!entryStrs.isEmpty()) config.set("commandEntry", entryStrs);

        List<String> exitStrs = new ArrayList<>();
        for (AreaCommandEntry e : area.getExitCommands()) exitStrs.add(e.toYmlString());
        if (!exitStrs.isEmpty()) config.set("commandExit", exitStrs);

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().error("Error saving area: " + area.getId(), e); }
    }


    public boolean createArea(String id, String dimension, int x1, int y1, int z1, int x2, int y2, int z2) {
        return createArea(id, dimension, x1, y1, z1, x2, y2, z2, "cube", 0);
    }

    public boolean createArea(String id, String dimension, int x1, int y1, int z1, int x2, int y2, int z2, String type, int flatPosition) {
        if (areas.containsKey(id)) return false;
        ProtectedArea area = new ProtectedArea(id, dimension, x1, y1, z1, x2, y2, z2);
        area.setType(type);
        area.setFlatPosition(flatPosition);
        areas.put(id, area);
        saveArea(area);
        ProtectedAreaServerEvents.AREA_CREATED.invoker().onArea(area);
        return true;
    }

    public boolean removeArea(String id) {
        if (!areas.containsKey(id)) return false;
        ProtectedArea area = areas.remove(id);

        File file = areaFile(area.getType(), id);
        if (file.exists()) file.delete();

        plugin.getAdvancedRulesManager().deleteAreaRules(area.getStorageKey());
        plugin.getDebugManager().refreshActiveSessions();
        ProtectedAreaServerEvents.AREA_REMOVED.invoker().onAreaRemoved(id);
        return true;
    }

    private File areaFile(String type, String id) {
        String relativePath = id.replace("/", File.separator) + ".yml";
        if ("dimension".equals(type)) return new File(plugin.getDataPath(), "Areas/.dimension/" + relativePath);
        if ("flat".equals(type)) return new File(plugin.getDataPath(), "Areas/.flat/" + relativePath);
        return new File(plugin.getDataPath(), "Areas/" + relativePath);
    }

    public boolean reloadArea(String id, String requestedType) {
        ProtectedArea existing = areas.get(id);
        String type = existing != null ? existing.getType() : requestedType;

        File file = areaFile(type, id);
        if (!file.exists()) return false;

        loadArea(file, id);

        ProtectedArea area = areas.get(id);
        plugin.getAdvancedRulesManager().reloadRulesFor(area != null ? area.getStorageKey() : id);

        if (area != null) {
            if (area.isDimension()) broadcastDimensionSkyboxes();
            else broadcastUpdateArea(area);
        }
        plugin.getDebugManager().refreshActiveSessions();
        return true;
    }

    public void reloadAreas() {
        broadcastClearAreas();
        loadAllAreas();
        plugin.getAdvancedRulesManager().reloadRules();

        for (ServerPlayerEntity player : plugin.getServer().getPlayerManager().getPlayerList()) {
            sendAllAreasToPlayer(player);
        }
        plugin.getDebugManager().refreshActiveSessions();
    }

    public void saveAreaManually(ProtectedArea area) {
        saveArea(area);
    }

    public void provisionDimensionAreas() {
        File dimensionFolder = new File(plugin.getDataPath(), "Areas/.dimension");
        try { Files.createDirectories(dimensionFolder.toPath()); } catch (IOException ignored) {}

        int created = 0;
        for (ServerWorld world : plugin.getServer().getWorlds()) {
            String dimensionKey = WorldDimensionUtil.getDimensionKey(world.getRegistryKey());
            String id = dimensionIdFromKey(dimensionKey);

            File file = new File(dimensionFolder, id.replace("/", File.separator) + ".yml");
            if (file.exists()) continue;

            SimpleYaml config = new SimpleYaml();
            config.set("id", id);
            config.set("type", "dimension");
            config.set("dimension", dimensionKey);
            config.set("alias", "");
            config.set("priority", -1);
            config.set("skybox", "");
            config.set("rules", new ArrayList<String>());

            try {
                config.save(file);
                created++;
            } catch (IOException e) {
                plugin.getLogger().error("Error creating dimension area: " + id, e);
            }
        }

        if (created > 0) {
            plugin.getLogger().info("Created " + created + " dimension area(s)");
        }
    }

    private static String dimensionIdFromKey(String dimensionKey) {
        int colon = dimensionKey.indexOf(':');
        if (colon < 0) return dimensionKey;
        return dimensionKey.substring(0, colon) + "/" + dimensionKey.substring(colon + 1);
    }


    public ProtectedArea getDimensionArea(String dimension) {
        for (ProtectedArea area : areas.values()) {
            if (area.isDimension() && area.getDimension().equals(dimension)) return area;
        }
        return null;
    }

    public int countDimensionAreas() {
        int total = 0;
        for (ProtectedArea area : areas.values()) if (area.isDimension()) total++;
        return total;
    }

    public ProtectedArea getCubeAreaAt(String dimension, double x, double y, double z) {
        ProtectedArea selected = null;
        int highestPriority = 0;
        long smallestVolume = 0;

        for (ProtectedArea area : areas.values()) {
            if (area.isDimension() || area.isFlat()) continue;
            if (!area.isInside(x, y, z, dimension)) continue;

            int areaPriority = area.getPriority();
            long vol = calculateVolume(area);
            if (selected == null || areaPriority > highestPriority
                    || (areaPriority == highestPriority && vol < smallestVolume)) {
                highestPriority = areaPriority;
                smallestVolume = vol;
                selected = area;
            }
        }
        return selected;
    }

    public ProtectedArea getAreaAt(String dimension, double x, double y, double z) {
        ProtectedArea selected = null;
        int highestPriority = Integer.MIN_VALUE;
        long smallestVolume = Long.MAX_VALUE;

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, dimension)) {
                int areaPriority = area.getPriority();
                if (selected == null || areaPriority > highestPriority) {
                    highestPriority = areaPriority;
                    smallestVolume = calculateVolume(area);
                    selected = area;
                } else if (areaPriority == highestPriority) {
                    long vol = calculateVolume(area);
                    if (vol < smallestVolume) { smallestVolume = vol; selected = area; }
                }
            }
        }
        return selected;
    }

    private long calculateVolume(ProtectedArea area) {
        if (area.isDimension()) return Long.MAX_VALUE;
        return (long) Math.abs(area.getX2() - area.getX1())
             * Math.abs(area.getY2() - area.getY1())
             * Math.abs(area.getZ2() - area.getZ1());
    }

    private List<ProtectedArea> buildInheritanceChain(String dimension, double x, double y, double z) {
        List<ProtectedArea> matching = new ArrayList<>();
        for (ProtectedArea a : areas.values())
            if (!a.isFlat() && a.isInside(x, y, z, dimension)) matching.add(a);

        matching.sort((a, b) -> {
            int byVolume = Long.compare(calculateVolume(a), calculateVolume(b));
            if (byVolume != 0) return byVolume;
            int byPriority = Integer.compare(b.getPriority(), a.getPriority());
            if (byPriority != 0) return byPriority;
            return a.getId().compareTo(b.getId());
        });

        List<ProtectedArea> chain = new ArrayList<>();
        for (ProtectedArea candidate : matching) {
            if (!chain.isEmpty() && chain.get(chain.size() - 1).getPriority() > candidate.getPriority()) break;
            chain.add(candidate);
        }
        return chain;
    }

    public boolean hasInheritedRule(String dimension, double x, double y, double z, AreaRule rule) {
        for (ProtectedArea a : buildInheritanceChain(dimension, x, y, z))
            if (a.hasRule(rule)) return true;
        return false;
    }

    public boolean hasInheritedException(String dimension, double x, double y, double z, String playerName, String ruleKey) {
        for (ProtectedArea a : buildInheritanceChain(dimension, x, y, z))
            if (a.hasException(playerName, ruleKey)) return true;
        return false;
    }

    public boolean hasInheritedAdvancedBlock(String dimension, double x, double y, double z, AdvancedRuleType ruleType, String blockId) {
        for (ProtectedArea a : buildInheritanceChain(dimension, x, y, z))
            if (plugin.getAdvancedRulesManager().getRules(a.getStorageKey()).hasBlock(ruleType, blockId)) return true;
        return false;
    }

    public boolean hasInheritedAdvancedEntity(String dimension, double x, double y, double z, AdvancedRuleType ruleType, String entityId) {
        for (ProtectedArea a : buildInheritanceChain(dimension, x, y, z))
            if (plugin.getAdvancedRulesManager().getRules(a.getStorageKey()).hasEntity(ruleType, entityId)) return true;
        return false;
    }

    public boolean hasInheritedAdvancedItem(String dimension, double x, double y, double z, AdvancedRuleType ruleType, String itemId) {
        for (ProtectedArea a : buildInheritanceChain(dimension, x, y, z))
            if (plugin.getAdvancedRulesManager().getRules(a.getStorageKey()).hasItem(ruleType, itemId)) return true;
        return false;
    }


    public List<ServerPlayerEntity> getPlayersInsideArea(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return List.of();
        List<ServerPlayerEntity> inside = new ArrayList<>();
        for (ServerPlayerEntity p : plugin.getServer().getPlayerManager().getPlayerList()) {
            String dim = p.getServerWorld().getRegistryKey().getValue().toString();
            if (area.isInside(p.getX(), p.getY(), p.getZ(), dim)) inside.add(p);
        }
        return inside;
    }

    public int getPlayersInArea(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return 0;
        int count = 0;
        for (ServerPlayerEntity p : getPlayersInsideArea(areaId)) {
            if (!area.hasException(p.getGameProfile().getName(), "limit") &&
                !area.hasException(p.getGameProfile().getName(), "all"))
                count++;
        }
        return count;
    }

    public boolean canPlayerEnterArea(ServerPlayerEntity player, ProtectedArea area) {
        String name = player.getGameProfile().getName();
        if (area.hasException(name, "limit")) return true;
        if (area.isLimitBlocked()) return false;
        if (area.hasPlayerLimit()) {
            return getPlayersInArea(area.getId()) < area.getPlayerLimit();
        }
        return true;
    }


    public void notifyCollision(ServerPlayerEntity player, String areaId, boolean isNoEntry) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return;
        Map<String, String> ph = NotificationManager.createFullPlaceholders(null, null, areaId, player.getGameProfile().getName());
        String messageKey = isNoEntry ? "no_entry_collision" : "no_exit_collision";
        plugin.getNotificationManager().sendNotification(player, "Rules", messageKey, ph);
    }

    public void returnPlayerToArea(ServerPlayerEntity player, ProtectedArea area) {
        double cx = (area.getX1() + area.getX2()) / 2.0 + 0.5;
        double cy = area.getY1() + 1;
        double cz = (area.getZ1() + area.getZ2()) / 2.0 + 0.5;

        ServerWorld world = player.getServerWorld();
        double[] safe = findSafeLocationInside(world, area, cx, cy, cz);
        double tx = safe != null ? safe[0] : cx;
        double ty = safe != null ? safe[1] : cy;
        double tz = safe != null ? safe[2] : cz;

        player.teleport(world, tx, ty, tz, Set.of(), player.getYaw(), player.getPitch());

        Map<String, String> ph = NotificationManager.createFullPlaceholders(null, null, area.getId(), player.getGameProfile().getName());
        plugin.getNotificationManager().sendNotification(player, "Rules", "no_exit_returned", ph);
    }

    private double[] findSafeLocationInside(ServerWorld world, ProtectedArea area, double cx, double cy, double cz) {
        for (int y = area.getY2(); y >= area.getY1(); y--) {
            BlockPos pos = new BlockPos((int) cx, y, (int) cz);
            BlockState state  = world.getBlockState(pos);
            BlockState above1 = world.getBlockState(pos.up());
            BlockState above2 = world.getBlockState(pos.up(2));
            if (state.isSolidBlock(world, pos) && above1.isAir() && above2.isAir()) {
                return new double[]{cx, y + 1, cz};
            }
        }
        return null;
    }

    public void expelPlayerFromArea(ServerPlayerEntity player, ProtectedArea area) {
        ServerWorld world = player.getServerWorld();
        double[] safe = findSafeLocationOutside(world, player, area);
        if (safe != null) {
            player.teleport(world, safe[0], safe[1], safe[2], Set.of(), player.getYaw(), player.getPitch());
        } else {
            BlockPos spawn = world.getSpawnPos();
            player.teleport(world, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, Set.of(), player.getYaw(), player.getPitch());
        }
    }

    private double[] findSafeLocationOutside(ServerWorld world, ServerPlayerEntity player, ProtectedArea area) {
        double centerX = (area.getX1() + area.getX2()) / 2.0;
        double centerZ = (area.getZ1() + area.getZ2()) / 2.0;
        double dirX = player.getX() - centerX;
        double dirZ = player.getZ() - centerZ;
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (length > 0) { dirX /= length; dirZ /= length; }

        double edgeX, edgeZ;
        if (Math.abs(dirX) > Math.abs(dirZ)) {
            edgeX = dirX > 0 ? area.getX2() + 2 : area.getX1() - 2;
            edgeZ = player.getZ();
        } else {
            edgeX = player.getX();
            edgeZ = dirZ > 0 ? area.getZ2() + 2 : area.getZ1() - 2;
        }

        for (int y = world.getTopY() - 1; y >= world.getBottomY(); y--) {
            BlockPos pos = new BlockPos((int) edgeX, y, (int) edgeZ);
            BlockState state  = world.getBlockState(pos);
            BlockState above1 = world.getBlockState(pos.up());
            BlockState above2 = world.getBlockState(pos.up(2));
            if (state.isSolidBlock(world, pos) && above1.isAir() && above2.isAir()) {
                return new double[]{edgeX + 0.5, y + 1, edgeZ + 0.5};
            }
        }
        return null;
    }


    public boolean addRuleToArea(String areaId, AreaRule rule) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return false;
        boolean added = area.addRule(rule);
        if (added) {
            saveArea(area);
            if (rule.isCollisionRule()) broadcastUpdateArea(area);
            plugin.getDebugManager().refreshActiveSessions();
        }
        return added;
    }

    public boolean removeRuleFromArea(String areaId, AreaRule rule) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return false;
        boolean removed = area.removeRule(rule);
        if (removed) {
            saveArea(area);
            if (rule.isCollisionRule()) broadcastUpdateArea(area);
            plugin.getDebugManager().refreshActiveSessions();
        }
        return removed;
    }


    public boolean setAreaLimit(String areaId, int limit) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return false;
        area.setPlayerLimit(limit);
        saveArea(area);
        broadcastUpdateArea(area);
        broadcastAreaLimitUpdate(areaId);
        plugin.getDebugManager().refreshActiveSessions();
        return true;
    }

    public boolean removeAreaLimit(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return false;
        area.setPlayerLimit(null);
        area.setLimitBlocked(false);
        saveArea(area);
        broadcastUpdateArea(area);
        plugin.getDebugManager().refreshActiveSessions();
        return true;
    }

    public boolean setAreaLimitBlocked(String areaId, boolean blocked) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return false;
        area.setLimitBlocked(blocked);
        saveArea(area);
        broadcastUpdateArea(area);
        broadcastAreaLimitUpdate(areaId);
        plugin.getDebugManager().refreshActiveSessions();
        return true;
    }


    public boolean setAreaColor(String id, String color, String alias) {
        ProtectedArea area = areas.get(id);
        if (area == null) return false;
        area.setColor(color);
        area.setAlias(alias);
        saveArea(area);
        return true;
    }

    public boolean setSkyboxForArea(String areaId, String skyboxName) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return false;
        area.setSkybox(skyboxName);
        saveArea(area);
        broadcastUpdateArea(area);
        return true;
    }

    public boolean setAreaPriority(String areaId, int priority) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return false;
        area.setPriority(priority);
        saveArea(area);
        broadcastUpdateArea(area);
        plugin.getDebugManager().refreshActiveSessions();
        return true;
    }


    public void addAreaCommand(String areaId, AreaCommandEntry entry, boolean isEntry) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return;
        if (isEntry) area.getEntryCommands().add(entry);
        else         area.getExitCommands().add(entry);
        saveArea(area);
    }

    public void removeAreaCommand(String areaId, int index, boolean isEntry) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return;
        List<AreaCommandEntry> list = isEntry ? area.getEntryCommands() : area.getExitCommands();
        if (index >= 0 && index < list.size()) {
            int totalBefore = list.size();
            list.remove(index);
            saveArea(area);
            String type = isEntry ? "entry" : "exit";
            plugin.getAreaCommandManager().shiftUsesAfterRemove(areaId, type, index, totalBefore);
        }
    }


    public void sendAllAreasToPlayer(ServerPlayerEntity player) {
        for (ProtectedArea area : areas.values()) sendAreaToPlayer(player, area);
        sendDimensionSkyboxesToPlayer(player);
    }

    private ProtectedAreaPayload buildDimensionSkyboxesPayload() {
        List<ProtectedArea> dimensionAreas = new ArrayList<>();
        for (ProtectedArea area : areas.values()) {
            if (area.isDimension() && area.hasSkybox()) dimensionAreas.add(area);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("DIMENSION_SKYBOXES");
            out.writeInt(dimensionAreas.size());
            for (ProtectedArea area : dimensionAreas) {
                out.writeUTF(area.getDimension());
                out.writeUTF(area.getSkybox());
                out.writeInt(area.getPriority());
            }
            return new ProtectedAreaPayload(stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void sendDimensionSkyboxesToPlayer(ServerPlayerEntity player) {
        ProtectedAreaPayload payload = buildDimensionSkyboxesPayload();
        if (payload == null) return;
        ServerPlayNetworking.send(player, payload);
    }

    public void broadcastDimensionSkyboxes() {
        ProtectedAreaPayload payload = buildDimensionSkyboxesPayload();
        if (payload == null) return;
        for (ServerPlayerEntity p : plugin.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(p, payload);
    }

    public void sendAreaToPlayer(ServerPlayerEntity player, ProtectedArea area) {
        if (area.isDimension()) return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("ADD_AREA");
            out.writeUTF(area.getId());
            out.writeUTF(area.getDimension());
            out.writeInt(area.getX1()); out.writeInt(area.getY1()); out.writeInt(area.getZ1());
            out.writeInt(area.getX2()); out.writeInt(area.getY2()); out.writeInt(area.getZ2());
            out.writeUTF(area.getColor());

            boolean hasNoEntry = area.hasRule(AreaRule.NO_ENTRY);
            boolean hasNoExit  = area.hasRule(AreaRule.NO_EXIT) || area.isLimitBlocked();
            out.writeBoolean(hasNoEntry);
            out.writeBoolean(hasNoExit);

            Set<String> noEntryEx = area.getExceptions("no_entry");
            Set<String> allEx     = area.getExceptions("all");
            Set<String> combinedEntry = new HashSet<>(noEntryEx);
            combinedEntry.addAll(allEx);
            out.writeInt(combinedEntry.size());
            for (String n : combinedEntry) out.writeUTF(n);

            Set<String> noExitEx   = area.getExceptions("no_exit");
            Set<String> limitEx    = area.getExceptions("limit");
            Set<String> combinedExit = new HashSet<>(noExitEx);
            combinedExit.addAll(allEx);
            if (area.isLimitBlocked()) combinedExit.addAll(limitEx);
            out.writeInt(combinedExit.size());
            for (String n : combinedExit) out.writeUTF(n);

            boolean hasLimit = area.hasPlayerLimit();
            out.writeBoolean(hasLimit);
            if (hasLimit) {
                out.writeInt(area.getPlayerLimit());
                int cur = getPlayersInArea(area.getId());
                out.writeBoolean(cur >= area.getPlayerLimit() || area.isLimitBlocked());
            }

            Set<String> combinedLimit = new HashSet<>(limitEx);
            combinedLimit.addAll(allEx);
            out.writeInt(combinedLimit.size());
            for (String n : combinedLimit) out.writeUTF(n);

            out.writeInt(area.getPriority());
            out.writeUTF(area.hasSkybox() ? area.getSkybox() : "");
            out.writeUTF(area.getType());
            out.writeInt(area.getFlatPosition());
            out.writeBoolean(area.isPassNegative());
            out.writeBoolean(area.isPassPositive());

            ServerPlayNetworking.send(player, new ProtectedAreaPayload(stream.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void broadcastNewArea(ProtectedArea area) {
        for (ServerPlayerEntity p : plugin.getServer().getPlayerManager().getPlayerList())
            sendAreaToPlayer(p, area);
    }

    public void broadcastUpdateArea(ProtectedArea area) {
        for (ServerPlayerEntity p : plugin.getServer().getPlayerManager().getPlayerList())
            sendAreaToPlayer(p, area);
    }

    public void broadcastRemoveArea(String areaId) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("REMOVE_AREA");
            out.writeUTF(areaId);
            ProtectedAreaPayload payload = new ProtectedAreaPayload(stream.toByteArray());
            for (ServerPlayerEntity p : plugin.getServer().getPlayerManager().getPlayerList())
                ServerPlayNetworking.send(p, payload);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void broadcastClearAreas() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("CLEAR_AREAS");
            ProtectedAreaPayload payload = new ProtectedAreaPayload(stream.toByteArray());
            for (ServerPlayerEntity p : plugin.getServer().getPlayerManager().getPlayerList())
                ServerPlayNetworking.send(p, payload);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void broadcastAreaLimitUpdate(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null || area.isDimension() || !area.hasPlayerLimit()) return;

        broadcastUpdateArea(area);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("UPDATE_AREA_LIMIT_STATE");
            out.writeUTF(areaId);
            int cur = getPlayersInArea(areaId);
            out.writeBoolean(cur >= area.getPlayerLimit() || area.isLimitBlocked());
            ProtectedAreaPayload payload = new ProtectedAreaPayload(stream.toByteArray());
            for (ServerPlayerEntity p : plugin.getServer().getPlayerManager().getPlayerList())
                ServerPlayNetworking.send(p, payload);
        } catch (IOException e) {
            plugin.getLogger().error("Error sending area limit update: " + areaId, e);
        }
        plugin.getDebugManager().refreshActiveSessions();
    }

    public void sendViewToggle(ServerPlayerEntity player, boolean enable) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("VIEW_TOGGLE");
            out.writeBoolean(enable);
            ServerPlayNetworking.send(player, new ProtectedAreaPayload(stream.toByteArray()));
        } catch (IOException e) { e.printStackTrace(); }
    }


    public Map<String, ProtectedArea> getAreas() { return areas; }

    public List<ProtectedArea> getAreasByFolder(String folderPrefix) {
        String prefix = folderPrefix.endsWith("/") ? folderPrefix : folderPrefix + "/";
        return areas.values().stream()
                .filter(a -> !a.isDimension())
                .filter(a -> a.getId().startsWith(prefix))
                .collect(Collectors.toList());
    }

    public List<ProtectedArea> getAreasByFolder(String folderPrefix, String type) {
        String prefix = folderPrefix.endsWith("/") ? folderPrefix : folderPrefix + "/";
        return areas.values().stream()
                .filter(a -> matchesType(a, type))
                .filter(a -> a.getId().startsWith(prefix))
                .collect(Collectors.toList());
    }

    private static boolean matchesType(ProtectedArea area, String type) {
        if ("dimension".equals(type)) return area.isDimension();
        if ("flat".equals(type)) return area.isFlat();
        if ("cube".equals(type)) return area.isCube();
        return false;
    }

    public Set<String> getFolderPrefixes(boolean cubeOnly) {
        Set<String> folders = new LinkedHashSet<>();
        for (Map.Entry<String, ProtectedArea> entry : areas.entrySet()) {
            if (entry.getValue().isDimension()) continue;
            if (cubeOnly && entry.getValue().isFlat()) continue;
            if (!cubeOnly && !entry.getValue().isFlat()) continue;
            String id = entry.getKey();
            if (id.contains("/")) {
                String[] parts = id.split("/");
                StringBuilder prefix = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    prefix.append(parts[i]).append("/");
                    folders.add(prefix.toString());
                }
            }
        }
        return folders;
    }

    public Set<String> getAllFolderPrefixes() {
        Set<String> folders = new LinkedHashSet<>();
        for (String id : areas.keySet()) {
            if (id.contains("/")) {
                String[] parts = id.split("/");
                StringBuilder prefix = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    prefix.append(parts[i]).append("/");
                    folders.add(prefix.toString());
                }
            }
        }
        return folders;
    }
}
