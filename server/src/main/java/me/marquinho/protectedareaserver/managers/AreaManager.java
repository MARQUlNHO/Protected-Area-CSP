package me.marquinho.protectedareaserver.managers;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.api.ProtectedAreaServerEvents;
import me.marquinho.protectedareaserver.models.*;
import me.marquinho.protectedareaserver.network.ProtectedAreaPayload;
import me.marquinho.protectedareaserver.util.SimpleYaml;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import me.marquinho.protectedareaserver.models.ProtectedArea;

import java.io.*;
import java.util.*;

public class AreaManager {

    private final ProtectedAreaInit plugin;
    private final Map<String, ProtectedArea> areas;

    public AreaManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        this.areas = new HashMap<>();
        try {
            java.nio.file.Files.createDirectories(new File(plugin.getDataPath(), "Areas").toPath());
            java.nio.file.Files.createDirectories(new File(plugin.getDataPath(), "Areas/.flat").toPath());
        } catch (java.io.IOException e) {
            plugin.getLogger().error("No se pudo crear el directorio Areas", e);
        }
    }


    public void loadAllAreas() {
        areas.clear();
        File areasFolder = new File(plugin.getDataPath(), "Areas");
        if (!areasFolder.exists()) {
            try { java.nio.file.Files.createDirectories(areasFolder.toPath()); } catch (java.io.IOException ignored) {}
            return;
        }

        File[] cubeFiles = areasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (cubeFiles != null) for (File file : cubeFiles) loadArea(file);

        File flatFolder = new File(areasFolder, ".flat");
        if (flatFolder.exists()) {
            File[] flatFiles = flatFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (flatFiles != null) for (File file : flatFiles) loadArea(file);
        }

        plugin.getLogger().info("Cargadas " + areas.size() + " áreas protegidas");
    }

    private void loadArea(File file) {
        SimpleYaml config = SimpleYaml.load(file);

        String id = config.getString("id");
        String dimension = config.getString("dimension");

        String worldName = config.getString("world");
        if (worldName == null || worldName.isEmpty() || worldName.startsWith("minecraft:")) {
            worldName = inferWorldFromDimension(dimension);
            plugin.getLogger().info("Área '" + id + "' migrada: world inferido como '" + worldName + "'");
        }

        int x1, y1, z1, x2, y2, z2;
        if (config.contains("xyz1")) {
            int[] c1 = parseXyz(config.getString("xyz1"));
            int[] c2 = parseXyz(config.getString("xyz2"));
            x1 = c1[0]; y1 = c1[1]; z1 = c1[2];
            x2 = c2[0]; y2 = c2[1]; z2 = c2[2];
        } else {
            x1 = config.getInt("x1"); y1 = config.getInt("y1"); z1 = config.getInt("z1");
            x2 = config.getInt("x2"); y2 = config.getInt("y2"); z2 = config.getInt("z2");
        }

        ProtectedArea area = new ProtectedArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2);

        if (config.contains("color")) area.setColor(config.getString("color"));
        if (config.contains("alias")) area.setAlias(config.getString("alias"));
        if (config.contains("priority")) area.setPriority(config.getInt("priority", 0));
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
        if (config.contains("type")) area.setType(config.getString("type"));
        if (config.contains("flatPosition")) area.setFlatPosition(config.getInt("flatPosition", 0));
        area.setPassNegative(config.getBoolean("pass.negative", true));
        area.setPassPositive(config.getBoolean("pass.positive", true));

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

    private String inferWorldFromDimension(String dimension) {
        if (dimension == null) return "minecraft:overworld";
        return switch (dimension) {
            case "minecraft:the_nether" -> "world_nether";
            case "minecraft:the_end"    -> "world_the_end";
            default                     -> "world";
        };
    }

    private void saveArea(ProtectedArea area) {
        if (area.isFlat()) {
            saveFlatArea(area);
        } else {
            saveCubeArea(area);
        }
    }

    private void saveFlatArea(ProtectedArea area) {
        File flatFolder = new File(plugin.getDataPath(), "Areas/.flat");
        try { java.nio.file.Files.createDirectories(flatFolder.toPath()); } catch (java.io.IOException ignored) {}

        File file = new File(flatFolder, area.getId() + ".yml");
        SimpleYaml config = new SimpleYaml();

        config.set("id", area.getId());
        config.set("type", area.getType());
        config.set("flatPosition", area.getFlatPosition());
        config.set("pass.negative", area.isPassNegative());
        config.set("pass.positive", area.isPassPositive());
        config.set("world", area.getWorldName());
        config.set("dimension", area.getDimension());
        config.set("xyz1", area.getX1() + "," + area.getY1() + "," + area.getZ1());
        config.set("xyz2", area.getX2() + "," + area.getY2() + "," + area.getZ2());
        config.set("color", area.getColor());
        config.set("alias", area.getAlias());

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().error("Error al guardar el área flat: " + area.getId(), e); }
    }

    private void saveCubeArea(ProtectedArea area) {
        File areasFolder = new File(plugin.getDataPath(), "Areas");
        try { java.nio.file.Files.createDirectories(areasFolder.toPath()); } catch (java.io.IOException ignored) {}

        File file = new File(areasFolder, area.getId() + ".yml");
        SimpleYaml config = new SimpleYaml();

        config.set("id", area.getId());
        config.set("type", area.getType());
        config.set("skybox", area.hasSkybox() ? area.getSkybox() : "");
        config.set("world", area.getWorldName());
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
        catch (IOException e) { plugin.getLogger().error("Error al guardar el área: " + area.getId(), e); }
    }


    public boolean createArea(String id, String worldName, String dimension, int x1, int y1, int z1, int x2, int y2, int z2) {
        return createArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2, "cube", 0);
    }

    public boolean createArea(String id, String worldName, String dimension, int x1, int y1, int z1, int x2, int y2, int z2, String type, int flatPosition) {
        if (areas.containsKey(id)) return false;
        String properWorldName = inferWorldFromDimension(dimension);
        ProtectedArea area = new ProtectedArea(id, properWorldName, dimension, x1, y1, z1, x2, y2, z2);
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

        File file = area.isFlat()
                ? new File(plugin.getDataPath(), "Areas/.flat/" + id + ".yml")
                : new File(plugin.getDataPath(), "Areas/" + id + ".yml");
        if (file.exists()) file.delete();

        plugin.getAdvancedRulesManager().deleteAreaRules(id);
        plugin.getDebugManager().refreshActiveSessions();
        ProtectedAreaServerEvents.AREA_REMOVED.invoker().onAreaRemoved(id);
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


    public ProtectedArea getAreaAt(String dimension, double x, double y, double z) {
        ProtectedArea selected = null;
        int highestPriority = Integer.MIN_VALUE;
        long smallestVolume = Long.MAX_VALUE;

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, dimension)) {
                int areaPriority = area.getPriority();
                if (areaPriority > highestPriority) {
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
        return (long) Math.abs(area.getX2() - area.getX1())
             * Math.abs(area.getY2() - area.getY1())
             * Math.abs(area.getZ2() - area.getZ1());
    }

    public boolean hasInheritedRule(String dimension, double x, double y, double z, AreaRule rule) {
        int highest = Integer.MIN_VALUE;
        for (ProtectedArea a : areas.values())
            if (a.isInside(x, y, z, dimension) && a.getPriority() > highest)
                highest = a.getPriority();

        for (ProtectedArea a : areas.values())
            if (a.isInside(x, y, z, dimension) && a.getPriority() == highest && a.hasRule(rule))
                return true;
        return false;
    }

    public boolean hasInheritedAdvancedBlock(String dimension, double x, double y, double z, AdvancedRuleType ruleType, String blockId) {
        int highest = Integer.MIN_VALUE;
        for (ProtectedArea a : areas.values())
            if (a.isInside(x, y, z, dimension) && a.getPriority() > highest)
                highest = a.getPriority();

        for (ProtectedArea a : areas.values()) {
            if (a.isInside(x, y, z, dimension) && a.getPriority() == highest) {
                if (plugin.getAdvancedRulesManager().getRules(a.getId()).hasBlock(ruleType, blockId))
                    return true;
            }
        }
        return false;
    }

    public boolean hasInheritedAdvancedEntity(String dimension, double x, double y, double z, AdvancedRuleType ruleType, String entityId) {
        int highest = Integer.MIN_VALUE;
        for (ProtectedArea a : areas.values())
            if (a.isInside(x, y, z, dimension) && a.getPriority() > highest)
                highest = a.getPriority();

        for (ProtectedArea a : areas.values()) {
            if (a.isInside(x, y, z, dimension) && a.getPriority() == highest) {
                if (plugin.getAdvancedRulesManager().getRules(a.getId()).hasEntity(ruleType, entityId))
                    return true;
            }
        }
        return false;
    }

    public boolean hasInheritedAdvancedItem(String dimension, double x, double y, double z, AdvancedRuleType ruleType, String itemId) {
        int highest = Integer.MIN_VALUE;
        for (ProtectedArea a : areas.values())
            if (a.isInside(x, y, z, dimension) && a.getPriority() > highest)
                highest = a.getPriority();

        for (ProtectedArea a : areas.values()) {
            if (a.isInside(x, y, z, dimension) && a.getPriority() == highest) {
                if (plugin.getAdvancedRulesManager().getRules(a.getId()).hasItem(ruleType, itemId))
                    return true;
            }
        }
        return false;
    }


    public int getPlayersInArea(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return 0;
        int count = 0;
        for (ServerPlayerEntity p : plugin.getServer().getPlayerManager().getPlayerList()) {
            String dim = p.getServerWorld().getRegistryKey().getValue().toString();
            ProtectedArea pa = getAreaAt(dim, p.getX(), p.getY(), p.getZ());
            if (pa != null && pa.getId().equals(areaId)) {
                if (!area.hasException(p.getGameProfile().getName(), "limit") &&
                    !area.hasException(p.getGameProfile().getName(), "all"))
                    count++;
            }
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
    }

    public void sendAreaToPlayer(ServerPlayerEntity player, ProtectedArea area) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);
        try {
            out.writeUTF("ADD_AREA");
            out.writeUTF(area.getId());
            out.writeUTF(area.getWorldName());
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
        if (area == null || !area.hasPlayerLimit()) return;

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
            plugin.getLogger().error("Error al enviar actualización de límite del área: " + areaId, e);
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
}
