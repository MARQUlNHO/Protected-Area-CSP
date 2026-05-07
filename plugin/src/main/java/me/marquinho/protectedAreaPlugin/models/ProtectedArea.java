package me.marquinho.protectedAreaPlugin.models;

import java.util.*;
import me.marquinho.protectedAreaPlugin.models.AreaCommandEntry;

public class ProtectedArea {
    private String id;
    private String worldName;
    private String dimension;
    private int x1, y1, z1;
    private int x2, y2, z2;
    private String color;
    private String alias;
    private int priority;
    private Set<AreaRule> rules;
    private Map<String, Set<String>> exceptions;

    private Integer playerLimit;
    private boolean limitBlocked;

    private String skybox;
    private String type;
    private int flatPosition;
    private boolean passNegative = true;
    private boolean passPositive = true;

    private List<AreaCommandEntry> entryCommands;
    private List<AreaCommandEntry> exitCommands;

    public ProtectedArea(String id, String worldName, String dimension, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.id = id;
        this.worldName = worldName;
        this.dimension = dimension;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
        this.color = "#FFFFFF";
        this.alias = "";
        this.priority = 0;
        this.rules = new HashSet<>();
        this.exceptions = new HashMap<>();
        this.playerLimit = null;
        this.limitBlocked = false;
        this.skybox = null;
        this.type = "cube";
        this.flatPosition = 0;
        this.entryCommands = new ArrayList<>();
        this.exitCommands  = new ArrayList<>();
    }

    public boolean isInside(double x, double y, double z, String worldName, String worldDimension) {
        if (!this.worldName.equals(worldName)) {
            return false;
        }

        if (!this.dimension.equals(worldDimension)) {
            return false;
        }

        return x >= x1 && x <= x2 &&
                y >= y1 && y <= y2 &&
                z >= z1 && z <= z2;
    }

    public boolean hasRule(AreaRule rule) {
        return rules.contains(rule);
    }

    public boolean addRule(AreaRule rule) {
        return rules.add(rule);
    }

    public boolean removeRule(AreaRule rule) {
        return rules.remove(rule);
    }

    public Set<AreaRule> getRules() {
        return new HashSet<>(rules);
    }

    public void setRules(Set<AreaRule> rules) {
        this.rules = new HashSet<>(rules);
    }

    public boolean addException(String ruleKey, String playerName) {
        return exceptions.computeIfAbsent(ruleKey.toLowerCase(), k -> new HashSet<>())
                .add(playerName.toLowerCase());
    }

    public boolean removeException(String ruleKey, String playerName) {
        Set<String> players = exceptions.get(ruleKey.toLowerCase());
        if (players == null) return false;

        boolean removed = players.remove(playerName.toLowerCase());
        if (players.isEmpty()) {
            exceptions.remove(ruleKey.toLowerCase());
        }
        return removed;
    }

    public void clearExceptions(String ruleKey) {
        exceptions.remove(ruleKey.toLowerCase());
    }

    public boolean hasException(String playerName, String ruleKey) {
        String playerNameLower = playerName.toLowerCase();

        Set<String> allExceptions = exceptions.get("all");
        if (allExceptions != null && allExceptions.contains(playerNameLower)) {
            return true;
        }

        Set<String> ruleExceptions = exceptions.get(ruleKey.toLowerCase());
        return ruleExceptions != null && ruleExceptions.contains(playerNameLower);
    }

    public Set<String> getExceptions(String ruleKey) {
        Set<String> players = exceptions.get(ruleKey.toLowerCase());
        return players != null ? new HashSet<>(players) : new HashSet<>();
    }

    public Map<String, Set<String>> getAllExceptions() {
        Map<String, Set<String>> copy = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : exceptions.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    public void setExceptions(Map<String, Set<String>> exceptions) {
        this.exceptions = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : exceptions.entrySet()) {
            this.exceptions.put(entry.getKey().toLowerCase(), new HashSet<>(entry.getValue()));
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Integer getPlayerLimit() {
        return playerLimit;
    }

    public void setPlayerLimit(Integer playerLimit) {
        this.playerLimit = playerLimit;
    }

    public boolean isLimitBlocked() {
        return limitBlocked;
    }

    public void setLimitBlocked(boolean limitBlocked) {
        this.limitBlocked = limitBlocked;
    }

    public boolean hasPlayerLimit() {
        return playerLimit != null && playerLimit > 0;
    }

    public String getId() { return id; }
    public String getWorldName() { return worldName; }
    public String getDimension() { return dimension; }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getZ1() { return z1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public int getZ2() { return z2; }
    public String getColor() { return color; }
    public String getAlias() { return alias; }

    public String getSkybox() { return skybox; }
    public void setSkybox(String skybox) { this.skybox = (skybox == null || skybox.isEmpty()) ? null : skybox; }
    public boolean hasSkybox() { return skybox != null && !skybox.isEmpty(); }

    public String getType() { return type; }
    public void setType(String type) { this.type = (type == null || type.isEmpty()) ? "cube" : type; }
    public boolean isFlat() { return "flat".equals(type); }
    public int getFlatPosition() { return flatPosition; }
    public void setFlatPosition(int flatPosition) { this.flatPosition = flatPosition; }

    public int getFlatAxis() {
        if (x2 - x1 <= 1) return 0;
        if (y2 - y1 <= 1) return 1;
        return 2;
    }

    public double getFlatPlaneCoord() {
        double fp = flatPosition / 16.0;
        return switch (getFlatAxis()) {
            case 0 -> x1 + fp;
            case 1 -> y1 + fp;
            default -> z1 + fp;
        };
    }

    public boolean isPassNegative() { return passNegative; }
    public void setPassNegative(boolean passNegative) { this.passNegative = passNegative; }
    public boolean isPassPositive() { return passPositive; }
    public void setPassPositive(boolean passPositive) { this.passPositive = passPositive; }

    public void setColor(String color) { this.color = color; }
    public void setAlias(String alias) { this.alias = alias; }

    public List<AreaCommandEntry> getEntryCommands() { return entryCommands; }
    public List<AreaCommandEntry> getExitCommands()  { return exitCommands; }

    public void setEntryCommands(List<AreaCommandEntry> entryCommands) { this.entryCommands = new ArrayList<>(entryCommands); }
    public void setExitCommands(List<AreaCommandEntry> exitCommands)   { this.exitCommands  = new ArrayList<>(exitCommands); }
}