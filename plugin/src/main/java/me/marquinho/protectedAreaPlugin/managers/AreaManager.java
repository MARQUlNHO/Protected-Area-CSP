package me.marquinho.protectedAreaPlugin.managers;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.api.events.AreaCreatedEvent;
import me.marquinho.protectedAreaPlugin.api.events.AreaRemovedEvent;
import me.marquinho.protectedAreaPlugin.models.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;

public class AreaManager {
    private final ProtectedAreaPlugin plugin;
    private final Map<String, ProtectedArea> areas;

    public AreaManager(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
        this.areas = new HashMap<>();

        File areasFolder = new File(plugin.getDataFolder(), "Areas");
        if (!areasFolder.exists()) areasFolder.mkdirs();
        File flatFolder = new File(areasFolder, ".flat");
        if (!flatFolder.exists()) flatFolder.mkdirs();
    }

    public void loadAllAreas() {
        areas.clear();

        File areasFolder = new File(plugin.getDataFolder(), "Areas");
        if (!areasFolder.exists()) {
            areasFolder.mkdirs();
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
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String id = config.getString("id");
        String dimension = config.getString("dimension");

        String worldName = config.getString("world");
        if (worldName == null || worldName.isEmpty()) {
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

        if (config.contains("color")) {
            area.setColor(config.getString("color"));
        }
        if (config.contains("alias")) {
            area.setAlias(config.getString("alias"));
        }

        if (config.contains("priority")) {
            area.setPriority(config.getInt("priority", 0));
        }

        if (config.contains("playerLimit")) {
            area.setPlayerLimit(config.getInt("playerLimit"));
        }

        if (config.contains("limitBlocked")) {
            area.setLimitBlocked(config.getBoolean("limitBlocked", false));
        }

        if (config.contains("rules")) {
            List<String> ruleKeys = config.getStringList("rules");
            Set<AreaRule> rules = new HashSet<>();
            for (String key : ruleKeys) {
                AreaRule rule = AreaRule.fromKey(key);
                if (rule != null) {
                    rules.add(rule);
                }
            }
            area.setRules(rules);
        }

        if (config.contains("exceptions")) {
            Map<String, Set<String>> exceptions = new HashMap<>();
            for (String key : config.getConfigurationSection("exceptions").getKeys(false)) {
                List<String> players = config.getStringList("exceptions." + key);
                exceptions.put(key, new HashSet<>(players));
            }
            area.setExceptions(exceptions);
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
        switch (dimension) {
            case "minecraft:the_nether":
                return "world_nether";
            case "minecraft:the_end":
                return "world_the_end";
            default:
                return "world";
        }
    }

    public boolean createArea(String id, String worldName, String dimension, int x1, int y1, int z1, int x2, int y2, int z2) {
        return createArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2, "cube", 0);
    }

    public boolean createArea(String id, String worldName, String dimension, int x1, int y1, int z1, int x2, int y2, int z2, String type, int flatPosition) {
        if (areas.containsKey(id)) return false;
        ProtectedArea area = new ProtectedArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2);
        area.setType(type);
        area.setFlatPosition(flatPosition);
        areas.put(id, area);
        saveArea(area);
        plugin.getServer().getPluginManager().callEvent(new AreaCreatedEvent(area));
        return true;
    }

    public boolean removeArea(String id) {
        if (!areas.containsKey(id)) {
            return false;
        }

        ProtectedArea area = areas.remove(id);

        File areasFolder = new File(plugin.getDataFolder(), "Areas");
        File file = area.isFlat()
                ? new File(areasFolder, ".flat/" + id + ".yml")
                : new File(areasFolder, id + ".yml");
        if (file.exists()) file.delete();

        plugin.getAdvancedRulesManager().deleteAreaRules(id);
        plugin.getDebugManager().refreshActiveSessions();
        plugin.getServer().getPluginManager().callEvent(new AreaRemovedEvent(id));
        return true;
    }

    public void reloadAreas() {
        broadcastClearAreas();
        loadAllAreas();

        plugin.getAdvancedRulesManager().reloadRules();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendAllAreasToPlayer(player);
        }

        plugin.getDebugManager().refreshActiveSessions();
    }

    private void saveArea(ProtectedArea area) {
        if (area.isFlat()) saveFlatArea(area);
        else saveCubeArea(area);
    }

    private void saveFlatArea(ProtectedArea area) {
        File flatFolder = new File(plugin.getDataFolder(), "Areas/.flat");
        if (!flatFolder.exists()) flatFolder.mkdirs();

        File file = new File(flatFolder, area.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

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

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar el área flat: " + area.getId());
            e.printStackTrace();
        }
    }

    private void saveCubeArea(ProtectedArea area) {
        File areasFolder = new File(plugin.getDataFolder(), "Areas");
        if (!areasFolder.exists()) areasFolder.mkdirs();

        File file = new File(areasFolder, area.getId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

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
        if (!exceptions.isEmpty()) {
            for (Map.Entry<String, Set<String>> entry : exceptions.entrySet()) {
                config.set("exceptions." + entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }

        List<String> entryCommandStrings = new ArrayList<>();
        for (AreaCommandEntry e : area.getEntryCommands()) entryCommandStrings.add(e.toYmlString());
        if (!entryCommandStrings.isEmpty()) config.set("commandEntry", entryCommandStrings);

        List<String> exitCommandStrings = new ArrayList<>();
        for (AreaCommandEntry e : area.getExitCommands()) exitCommandStrings.add(e.toYmlString());
        if (!exitCommandStrings.isEmpty()) config.set("commandExit", exitCommandStrings);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar el área: " + area.getId());
            e.printStackTrace();
        }
    }

    public int getPlayersInArea(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return 0;

        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ProtectedArea playerArea = getAreaAt(player.getLocation());
            if (playerArea != null && playerArea.getId().equals(areaId)) {
                if (!area.hasException(player.getName(), "limit") && !area.hasException(player.getName(), "all")) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean canPlayerEnterArea(Player player, ProtectedArea area) {
        if (area.hasException(player.getName(), "limit")) {
            return true;
        }

        if (area.isLimitBlocked()) {
            return false;
        }

        if (area.hasPlayerLimit()) {
            int currentPlayers = getPlayersInArea(area.getId());
            return currentPlayers < area.getPlayerLimit();
        }

        return true;
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

    private void updateAreaLimitState(ProtectedArea area) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendAreaToPlayer(player, area);
        }
    }

    public void notifyCollision(Player player, String areaId, boolean isNoEntry) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return;

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                null,
                null,
                areaId,
                player.getName()
        );

        String messageKey = isNoEntry ? "no_entry_collision" : "no_exit_collision";
        plugin.getNotificationManager().sendNotification(player, "Rules", messageKey, placeholders);
    }

    public void returnPlayerToArea(Player player, ProtectedArea area) {
        double centerX = (area.getX1() + area.getX2()) / 2.0 + 0.5;
        double centerY = area.getY1() + 1;
        double centerZ = (area.getZ1() + area.getZ2()) / 2.0 + 0.5;

        org.bukkit.Location returnLocation = new org.bukkit.Location(
                player.getWorld(),
                centerX,
                centerY,
                centerZ,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        org.bukkit.Location safeLocation = findSafeLocationInside(player, area, returnLocation);

        if (safeLocation != null) {
            player.teleport(safeLocation);
        } else {
            player.teleport(returnLocation);
        }

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                null,
                null,
                area.getId(),
                player.getName()
        );
        plugin.getNotificationManager().sendNotification(player, "Rules", "no_exit_returned", placeholders);
    }

    private org.bukkit.Location findSafeLocationInside(Player player, ProtectedArea area, org.bukkit.Location defaultLocation) {
        org.bukkit.World world = player.getWorld();

        for (int y = area.getY2(); y >= area.getY1(); y--) {
            org.bukkit.Location testLoc = new org.bukkit.Location(
                    world,
                    defaultLocation.getX(),
                    y,
                    defaultLocation.getZ()
            );

            if (testLoc.getBlock().getType().isSolid() &&
                    testLoc.clone().add(0, 1, 0).getBlock().getType().isAir() &&
                    testLoc.clone().add(0, 2, 0).getBlock().getType().isAir()) {
                return testLoc.add(0, 1, 0);
            }
        }

        return null;
    }

    public boolean addRuleToArea(String areaId, AreaRule rule) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) {
            return false;
        }

        boolean added = area.addRule(rule);
        if (added) {
            saveArea(area);

            if (rule.isCollisionRule()) {
                broadcastUpdateArea(area);
            }

            plugin.getDebugManager().refreshActiveSessions();
        }
        return added;
    }

    public boolean removeRuleFromArea(String areaId, AreaRule rule) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) {
            return false;
        }

        boolean removed = area.removeRule(rule);
        if (removed) {
            saveArea(area);

            if (rule.isCollisionRule()) {
                broadcastUpdateArea(area);
            }

            plugin.getDebugManager().refreshActiveSessions();
        }
        return removed;
    }

    public ProtectedArea getAreaAt(org.bukkit.Location location) {
        String worldName = location.getWorld().getName();
        String dimension = getDimensionKey(worldName);
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        ProtectedArea selectedArea = null;
        int highestPriority = Integer.MIN_VALUE;
        long smallestVolume = Long.MAX_VALUE;

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                int areaPriority = area.getPriority();

                if (areaPriority > highestPriority) {
                    highestPriority = areaPriority;
                    smallestVolume = calculateVolume(area);
                    selectedArea = area;
                } else if (areaPriority == highestPriority) {
                    long areaVolume = calculateVolume(area);
                    if (areaVolume < smallestVolume) {
                        smallestVolume = areaVolume;
                        selectedArea = area;
                    }
                }
            }
        }

        return selectedArea;
    }

    private long calculateVolume(ProtectedArea area) {
        long width = Math.abs(area.getX2() - area.getX1());
        long height = Math.abs(area.getY2() - area.getY1());
        long depth = Math.abs(area.getZ2() - area.getZ1());
        return width * height * depth;
    }

    public boolean hasInheritedRule(org.bukkit.Location location, AreaRule rule) {
        String worldName = location.getWorld().getName();
        String dimension = getDimensionKey(worldName);
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        int highestPriority = Integer.MIN_VALUE;

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                if (area.getPriority() > highestPriority) {
                    highestPriority = area.getPriority();
                }
            }
        }

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                if (area.getPriority() == highestPriority && area.hasRule(rule)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasInheritedAdvancedBlock(org.bukkit.Location location, AdvancedRuleType ruleType, String blockId) {
        String worldName = location.getWorld().getName();
        String dimension = getDimensionKey(worldName);
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        int highestPriority = Integer.MIN_VALUE;

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                if (area.getPriority() > highestPriority) {
                    highestPriority = area.getPriority();
                }
            }
        }

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                if (area.getPriority() == highestPriority) {
                    AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());
                    if (advancedRules.hasBlock(ruleType, blockId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean hasInheritedAdvancedEntity(org.bukkit.Location location, AdvancedRuleType ruleType, String entityId) {
        String worldName = location.getWorld().getName();
        String dimension = getDimensionKey(worldName);
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        int highestPriority = Integer.MIN_VALUE;

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                if (area.getPriority() > highestPriority) {
                    highestPriority = area.getPriority();
                }
            }
        }

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                if (area.getPriority() == highestPriority) {
                    AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());
                    if (advancedRules.hasEntity(ruleType, entityId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean hasInheritedAdvancedItem(org.bukkit.Location location, AdvancedRuleType ruleType, String itemId) {
        String worldName = location.getWorld().getName();
        String dimension = getDimensionKey(worldName);
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        int highestPriority = Integer.MIN_VALUE;

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                if (area.getPriority() > highestPriority) {
                    highestPriority = area.getPriority();
                }
            }
        }

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, worldName, dimension)) {
                if (area.getPriority() == highestPriority) {
                    AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());
                    if (advancedRules.hasItem(ruleType, itemId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private String getDimensionKey(String worldName) {
        if (worldName.contains("nether")) {
            return "minecraft:the_nether";
        } else if (worldName.contains("end")) {
            return "minecraft:the_end";
        } else {
            return "minecraft:overworld";
        }
    }

    public void sendAllAreasToPlayer(Player player) {
        for (ProtectedArea area : areas.values()) {
            sendAreaToPlayer(player, area);
        }
    }

    public void sendAreaToPlayer(Player player, ProtectedArea area) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF("ADD_AREA");
            out.writeUTF(area.getId());
            out.writeUTF(area.getWorldName());
            out.writeUTF(area.getDimension());
            out.writeInt(area.getX1());
            out.writeInt(area.getY1());
            out.writeInt(area.getZ1());
            out.writeInt(area.getX2());
            out.writeInt(area.getY2());
            out.writeInt(area.getZ2());
            out.writeUTF(area.getColor());

            boolean hasNoEntry = area.hasRule(AreaRule.NO_ENTRY);
            boolean hasNoExit = area.hasRule(AreaRule.NO_EXIT) || area.isLimitBlocked();
            out.writeBoolean(hasNoEntry);
            out.writeBoolean(hasNoExit);

            Set<String> noEntryExceptions = area.getExceptions("no_entry");
            Set<String> allExceptions = area.getExceptions("all");

            Set<String> combinedNoEntry = new HashSet<>(noEntryExceptions);
            combinedNoEntry.addAll(allExceptions);

            out.writeInt(combinedNoEntry.size());
            for (String playerName : combinedNoEntry) {
                out.writeUTF(playerName);
            }

            Set<String> noExitExceptions = area.getExceptions("no_exit");
            Set<String> limitExceptions = area.getExceptions("limit");

            Set<String> combinedNoExit = new HashSet<>(noExitExceptions);
            combinedNoExit.addAll(allExceptions);

            if (area.isLimitBlocked()) {
                combinedNoExit.addAll(limitExceptions);
            }

            out.writeInt(combinedNoExit.size());
            for (String playerName : combinedNoExit) {
                out.writeUTF(playerName);
            }

            boolean hasLimit = area.hasPlayerLimit();
            out.writeBoolean(hasLimit);

            if (hasLimit) {
                out.writeInt(area.getPlayerLimit());
                int currentPlayers = getPlayersInArea(area.getId());
                boolean isLimitReached = currentPlayers >= area.getPlayerLimit();
                out.writeBoolean(isLimitReached || area.isLimitBlocked());
            }

            Set<String> combinedLimit = new HashSet<>(limitExceptions);
            combinedLimit.addAll(allExceptions);

            out.writeInt(combinedLimit.size());
            for (String playerName : combinedLimit) {
                out.writeUTF(playerName);
            }

            out.writeInt(area.getPriority());
            out.writeUTF(area.hasSkybox() ? area.getSkybox() : "");
            out.writeUTF(area.getType());
            out.writeInt(area.getFlatPosition());
            out.writeBoolean(area.isPassNegative());
            out.writeBoolean(area.isPassPositive());

            player.sendPluginMessage(plugin, "protectedarea:main", stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void expelPlayerFromArea(Player player, ProtectedArea area) {
        org.bukkit.Location safeLocation = findSafeLocationOutside(player, area);

        if (safeLocation != null) {
            player.teleport(safeLocation);
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    private org.bukkit.Location findSafeLocationOutside(Player player, ProtectedArea area) {
        org.bukkit.Location playerLoc = player.getLocation();
        org.bukkit.World world = player.getWorld();

        double centerX = (area.getX1() + area.getX2()) / 2.0;
        double centerZ = (area.getZ1() + area.getZ2()) / 2.0;

        double dirX = playerLoc.getX() - centerX;
        double dirZ = playerLoc.getZ() - centerZ;

        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (length > 0) {
            dirX /= length;
            dirZ /= length;
        }

        double edgeX, edgeZ;

        if (Math.abs(dirX) > Math.abs(dirZ)) {
            edgeX = dirX > 0 ? area.getX2() + 2 : area.getX1() - 2;
            edgeZ = playerLoc.getZ();
        } else {
            edgeX = playerLoc.getX();
            edgeZ = dirZ > 0 ? area.getZ2() + 2 : area.getZ1() - 2;
        }

        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            org.bukkit.Location testLoc = new org.bukkit.Location(world, edgeX, y, edgeZ);
            if (testLoc.getBlock().getType().isSolid() &&
                    testLoc.clone().add(0, 1, 0).getBlock().getType().isAir() &&
                    testLoc.clone().add(0, 2, 0).getBlock().getType().isAir()) {
                return testLoc.add(0.5, 1, 0.5);
            }
        }

        return null;
    }

    public void broadcastNewArea(ProtectedArea area) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendAreaToPlayer(player, area);
        }
    }

    public void broadcastRemoveArea(String areaId) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF("REMOVE_AREA");
            out.writeUTF(areaId);

            byte[] data = stream.toByteArray();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendPluginMessage(plugin, "protectedarea:main", data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastClearAreas() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF("CLEAR_AREAS");

            byte[] data = stream.toByteArray();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendPluginMessage(plugin, "protectedarea:main", data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, ProtectedArea> getAreas() {
        return areas;
    }

    public boolean setAreaColor(String id, String color, String alias) {
        ProtectedArea area = areas.get(id);
        if (area == null) {
            return false;
        }

        area.setColor(color);
        area.setAlias(alias);
        saveArea(area);
        return true;
    }

    public boolean setAreaPriority(String areaId, int priority) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) {
            return false;
        }

        area.setPriority(priority);
        saveArea(area);
        plugin.getDebugManager().refreshActiveSessions();
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

    public void sendViewToggle(Player player, boolean enable) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF("VIEW_TOGGLE");
            out.writeBoolean(enable);

            player.sendPluginMessage(plugin, "protectedarea:main", stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAreaManually(ProtectedArea area) {
        saveArea(area);
    }

    public void broadcastUpdateArea(ProtectedArea area) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendAreaToPlayer(player, area);
        }
    }

    public void broadcastAreaLimitUpdate(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null || !area.hasPlayerLimit()) {
            return;
        }

        broadcastUpdateArea(area);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF("UPDATE_AREA_LIMIT_STATE");
            out.writeUTF(areaId);

            int currentPlayers = getPlayersInArea(areaId);
            boolean isLimitReached = currentPlayers >= area.getPlayerLimit();
            boolean shouldBlock = isLimitReached || area.isLimitBlocked();

            out.writeBoolean(shouldBlock);

            byte[] data = stream.toByteArray();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendPluginMessage(plugin, "protectedarea:main", data);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("Error al enviar actualización de límite del área: " + areaId);
            e.printStackTrace();
        }

        plugin.getDebugManager().refreshActiveSessions();
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
}