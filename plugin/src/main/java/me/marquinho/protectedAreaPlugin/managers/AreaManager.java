package me.marquinho.protectedAreaPlugin.managers;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.api.events.AreaCreatedEvent;
import me.marquinho.protectedAreaPlugin.api.events.AreaRemovedEvent;
import me.marquinho.protectedAreaPlugin.models.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
        File dimensionFolder = new File(areasFolder, ".dimension");
        if (!dimensionFolder.exists()) dimensionFolder.mkdirs();
    }

    public void loadAllAreas() {
        areas.clear();

        File areasFolder = new File(plugin.getDataFolder(), "Areas");
        if (!areasFolder.exists()) {
            areasFolder.mkdirs();
            return;
        }

        File flatFolder = new File(areasFolder, ".flat");
        File dimensionFolder = new File(areasFolder, ".dimension");
        scanAreaFiles(areasFolder, areasFolder, List.of(flatFolder, dimensionFolder));
        if (flatFolder.exists()) {
            scanAreaFiles(flatFolder, flatFolder, List.of());
        }
        if (dimensionFolder.exists()) {
            scanAreaFiles(dimensionFolder, dimensionFolder, List.of());
        }

        plugin.getLogger().info("Loaded " + areas.size() + " protected areas");
    }

    private void scanAreaFiles(File dir, File root, List<File> excludeDirs) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                if (isExcluded(file, excludeDirs)) continue;
                scanAreaFiles(file, root, excludeDirs);
            } else if (file.getName().endsWith(".yml")) {
                String relativePath = root.toURI().relativize(file.toURI()).getPath();
                String id = relativePath.replaceAll("\\.yml$", "").replace('\\', '/');
                loadArea(file, id);
            }
        }
    }

    private boolean isExcluded(File dir, List<File> excludeDirs) {
        for (File excluded : excludeDirs) {
            if (dir.getAbsolutePath().equals(excluded.getAbsolutePath())) return true;
        }
        return false;
    }

    private void loadArea(File file, String id) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

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

        if (config.contains("color")) {
            area.setColor(config.getString("color"));
        }
        if (config.contains("alias")) {
            area.setAlias(config.getString("alias"));
        }

        if (config.contains("priority")) {
            area.setPriority(config.getInt("priority", 0));
        } else if (dimensionArea) {
            area.setPriority(-1);
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
        plugin.getServer().getPluginManager().callEvent(new AreaCreatedEvent(area));
        return true;
    }

    public boolean removeArea(String id) {
        if (!areas.containsKey(id)) {
            return false;
        }

        ProtectedArea area = areas.remove(id);

        File file = areaFile(area.getType(), id);
        if (file.exists()) file.delete();

        plugin.getAdvancedRulesManager().deleteAreaRules(area.getStorageKey());
        plugin.getDebugManager().refreshActiveSessions();
        plugin.getServer().getPluginManager().callEvent(new AreaRemovedEvent(id));
        return true;
    }

    private File areaFile(String type, String id) {
        File areasFolder = new File(plugin.getDataFolder(), "Areas");
        String relativePath = id.replace('/', File.separatorChar) + ".yml";
        if ("dimension".equals(type)) return new File(areasFolder, ".dimension" + File.separatorChar + relativePath);
        if ("flat".equals(type)) return new File(areasFolder, ".flat" + File.separatorChar + relativePath);
        return new File(areasFolder, relativePath);
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

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendAllAreasToPlayer(player);
        }

        plugin.getDebugManager().refreshActiveSessions();
    }

    private void saveArea(ProtectedArea area) {
        if (area.isDimension()) saveDimensionArea(area);
        else if (area.isFlat()) saveFlatArea(area);
        else saveCubeArea(area);
    }

    private void saveDimensionArea(ProtectedArea area) {
        File dimensionFolder = new File(plugin.getDataFolder(), "Areas/.dimension");
        dimensionFolder.mkdirs();

        File file = new File(dimensionFolder, area.getId().replace('/', File.separatorChar) + ".yml");
        file.getParentFile().mkdirs();
        YamlConfiguration config = new YamlConfiguration();

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

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving dimension area: " + area.getId());
            e.printStackTrace();
        }
    }

    private void saveFlatArea(ProtectedArea area) {
        File flatFolder = new File(plugin.getDataFolder(), "Areas/.flat");
        flatFolder.mkdirs();

        File file = new File(flatFolder, area.getId().replace('/', File.separatorChar) + ".yml");
        file.getParentFile().mkdirs();
        YamlConfiguration config = new YamlConfiguration();

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

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving flat area: " + area.getId());
            e.printStackTrace();
        }
    }

    private void saveCubeArea(ProtectedArea area) {
        File areasFolder = new File(plugin.getDataFolder(), "Areas");
        areasFolder.mkdirs();

        File file = new File(areasFolder, area.getId().replace('/', File.separatorChar) + ".yml");
        file.getParentFile().mkdirs();
        YamlConfiguration config = new YamlConfiguration();

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
            plugin.getLogger().severe("Error saving area: " + area.getId());
            e.printStackTrace();
        }
    }

    public List<Player> getPlayersInsideArea(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return List.of();
        List<Player> inside = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            org.bukkit.Location loc = player.getLocation();
            String dimension = loc.getWorld().getKey().toString();
            if (area.isInside(loc.getX(), loc.getY(), loc.getZ(), dimension)) inside.add(player);
        }
        return inside;
    }

    public int getPlayersInArea(String areaId) {
        ProtectedArea area = areas.get(areaId);
        if (area == null) return 0;

        int count = 0;
        for (Player player : getPlayersInsideArea(areaId)) {
            if (!area.hasException(player.getName(), "limit") && !area.hasException(player.getName(), "all")) {
                count++;
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

    public void provisionDimensionAreas() {
        File dimensionFolder = new File(plugin.getDataFolder(), "Areas/.dimension");
        dimensionFolder.mkdirs();

        int created = 0;
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            String dimensionKey = world.getKey().toString();
            String id = dimensionIdFromKey(dimensionKey);

            File file = new File(dimensionFolder, id.replace('/', File.separatorChar) + ".yml");
            if (file.exists()) continue;

            file.getParentFile().mkdirs();
            YamlConfiguration config = new YamlConfiguration();
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
                plugin.getLogger().severe("Error creating dimension area: " + id);
                e.printStackTrace();
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

    public ProtectedArea getCubeAreaAt(org.bukkit.Location location) {
        String dimension = location.getWorld().getKey().toString();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

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

    public ProtectedArea getAreaAt(org.bukkit.Location location) {
        String dimension = location.getWorld().getKey().toString();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        ProtectedArea selectedArea = null;
        int highestPriority = Integer.MIN_VALUE;
        long smallestVolume = Long.MAX_VALUE;

        for (ProtectedArea area : areas.values()) {
            if (area.isInside(x, y, z, dimension)) {
                int areaPriority = area.getPriority();

                if (selectedArea == null || areaPriority > highestPriority) {
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
        if (area.isDimension()) return Long.MAX_VALUE;
        long width = Math.abs(area.getX2() - area.getX1());
        long height = Math.abs(area.getY2() - area.getY1());
        long depth = Math.abs(area.getZ2() - area.getZ1());
        return width * height * depth;
    }

    private List<ProtectedArea> buildInheritanceChain(org.bukkit.Location location) {
        String dimension = location.getWorld().getKey().toString();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        List<ProtectedArea> matching = new ArrayList<>();
        for (ProtectedArea area : areas.values()) {
            if (!area.isFlat() && area.isInside(x, y, z, dimension)) {
                matching.add(area);
            }
        }

        matching.sort((a, b) -> {
            int byVolume = Long.compare(calculateVolume(a), calculateVolume(b));
            if (byVolume != 0) return byVolume;
            int byPriority = Integer.compare(b.getPriority(), a.getPriority());
            if (byPriority != 0) return byPriority;
            return a.getId().compareTo(b.getId());
        });

        List<ProtectedArea> chain = new ArrayList<>();
        for (ProtectedArea candidate : matching) {
            if (!chain.isEmpty() && chain.get(chain.size() - 1).getPriority() > candidate.getPriority()) {
                break;
            }
            chain.add(candidate);
        }

        return chain;
    }

    public boolean hasInheritedRule(org.bukkit.Location location, AreaRule rule) {
        for (ProtectedArea area : buildInheritanceChain(location)) {
            if (area.hasRule(rule)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInheritedException(org.bukkit.Location location, String playerName, String ruleKey) {
        for (ProtectedArea area : buildInheritanceChain(location)) {
            if (area.hasException(playerName, ruleKey)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInheritedAdvancedBlock(org.bukkit.Location location, AdvancedRuleType ruleType, String blockId) {
        for (ProtectedArea area : buildInheritanceChain(location)) {
            AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getStorageKey());
            if (advancedRules.hasBlock(ruleType, blockId)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInheritedAdvancedEntity(org.bukkit.Location location, AdvancedRuleType ruleType, String entityId) {
        for (ProtectedArea area : buildInheritanceChain(location)) {
            AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getStorageKey());
            if (advancedRules.hasEntity(ruleType, entityId)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInheritedAdvancedItem(org.bukkit.Location location, AdvancedRuleType ruleType, String itemId) {
        for (ProtectedArea area : buildInheritanceChain(location)) {
            AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getStorageKey());
            if (advancedRules.hasItem(ruleType, itemId)) {
                return true;
            }
        }
        return false;
    }


    public void sendAllAreasToPlayer(Player player) {
        for (ProtectedArea area : areas.values()) {
            sendAreaToPlayer(player, area);
        }
        sendDimensionSkyboxesToPlayer(player);
    }

    private byte[] buildDimensionSkyboxesPayload() {
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
            return stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void sendDimensionSkyboxesToPlayer(Player player) {
        byte[] data = buildDimensionSkyboxesPayload();
        if (data == null) return;
        player.sendPluginMessage(plugin, "protectedarea:main", data);
    }

    public void broadcastDimensionSkyboxes() {
        byte[] data = buildDimensionSkyboxesPayload();
        if (data == null) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendPluginMessage(plugin, "protectedarea:main", data);
        }
    }

    public void sendAreaToPlayer(Player player, ProtectedArea area) {
        if (area.isDimension()) return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(stream);

        try {
            out.writeUTF("ADD_AREA");
            out.writeUTF(area.getId());
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
        broadcastUpdateArea(area);
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
        if (area == null || area.isDimension() || !area.hasPlayerLimit()) {
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
            plugin.getLogger().severe("Error sending area limit update: " + areaId);
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

    public List<ProtectedArea> getAreasByFolder(String folderPrefix) {
        return areas.values().stream()
                .filter(a -> !a.isDimension())
                .filter(a -> a.getId().startsWith(folderPrefix))
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

}