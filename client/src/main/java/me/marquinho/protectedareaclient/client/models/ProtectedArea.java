package me.marquinho.protectedareaclient.client.models;

import java.util.HashSet;
import java.util.Set;

public class ProtectedArea {
    private final String id;
    private final String worldName;
    private final String dimension;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final String color;
    private final boolean hasNoEntry;
    private final boolean hasNoExit;
    private final Set<String> noEntryExceptions;
    private final Set<String> noExitExceptions;

    private final boolean hasLimit;
    private boolean isLimitActive;
    private final Set<String> limitExceptions;

    private final int priority;
    private final String skybox;
    private final String type;
    private final int flatPosition;
    private final boolean passNegative;
    private final boolean passPositive;

    public ProtectedArea(String id, String worldName, String dimension,
                         int x1, int y1, int z1, int x2, int y2, int z2,
                         String color, boolean hasNoEntry, boolean hasNoExit,
                         Set<String> noEntryExceptions, Set<String> noExitExceptions,
                         boolean hasLimit, boolean isLimitActive, Set<String> limitExceptions,
                         int priority, String skybox, String type, int flatPosition,
                         boolean passNegative, boolean passPositive) {
        this.id = id;
        this.worldName = worldName;
        this.dimension = dimension;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2) + 1;
        this.maxY = Math.max(y1, y2) + 1;
        this.maxZ = Math.max(z1, z2) + 1;
        this.color = color;
        this.hasNoEntry = hasNoEntry;
        this.hasNoExit = hasNoExit;
        this.noEntryExceptions = noEntryExceptions != null ? noEntryExceptions : new HashSet<>();
        this.noExitExceptions = noExitExceptions != null ? noExitExceptions : new HashSet<>();
        this.hasLimit = hasLimit;
        this.isLimitActive = isLimitActive;
        this.limitExceptions = limitExceptions != null ? limitExceptions : new HashSet<>();
        this.priority = priority;
        this.skybox = (skybox == null || skybox.isEmpty()) ? null : skybox;
        this.type = (type == null || type.isEmpty()) ? "cube" : type;
        this.flatPosition = flatPosition;
        this.passNegative = passNegative;
        this.passPositive = passPositive;
    }

    public boolean isInside(double x, double y, double z, String worldName, String worldDimension) {
        if (!this.worldName.equals(worldName)) return false;
        if (!this.dimension.equals(worldDimension)) return false;
        return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
    }

    public int getFlatAxis() {
        if (maxX - minX <= 1) return 0;
        if (maxY - minY <= 1) return 1;
        return 2;
    }

    public double getFlatPlaneCoord() {
        double fp = flatPosition / 16.0;
        return switch (getFlatAxis()) {
            case 0 -> minX + fp;
            case 1 -> minY + fp;
            default -> minZ + fp;
        };
    }

    public boolean isWithinFlatBounds(double x, double y, double z, String worldName, String worldDimension) {
        if (!this.worldName.equals(worldName)) return false;
        if (!this.dimension.equals(worldDimension)) return false;
        double planeCoord = getFlatPlaneCoord();
        return switch (getFlatAxis()) {
            case 0 -> Math.abs(x - planeCoord) <= 2.0 && y >= minY && y < maxY && z >= minZ && z < maxZ;
            case 1 -> Math.abs(y - planeCoord) <= 2.0 && x >= minX && x < maxX && z >= minZ && z < maxZ;
            default -> Math.abs(z - planeCoord) <= 2.0 && x >= minX && x < maxX && y >= minY && y < maxY;
        };
    }

    public boolean getFlatSide(double x, double y, double z) {
        double planeCoord = getFlatPlaneCoord();
        double coord = switch (getFlatAxis()) {
            case 0 -> x;
            case 1 -> y;
            default -> z;
        };
        return coord >= planeCoord;
    }

    public boolean hasCollisionRule() {
        return hasNoEntry || hasNoExit || (hasLimit && isLimitActive);
    }

    public boolean hasException(String playerName, boolean isNoEntry) {
        if (playerName == null) return false;

        String nameLower = playerName.toLowerCase();

        if (isNoEntry) {
            return noEntryExceptions.contains(nameLower);
        } else {
            return noExitExceptions.contains(nameLower);
        }
    }

    public boolean hasLimitException(String playerName) {
        if (playerName == null) return false;
        return limitExceptions.contains(playerName.toLowerCase());
    }

    public boolean shouldCollide(boolean isPlayerInside, String playerName) {
        if (hasLimitException(playerName)) {
            return false;
        }

        if (hasNoEntry && !isPlayerInside) {
            return !hasException(playerName, true);
        }
        if (hasNoExit && isPlayerInside) {
            return !hasException(playerName, false);
        }
        if (hasLimit && isLimitActive && !isPlayerInside) {
            return true;
        }
        return false;
    }

    public void updateLimitState(boolean active) {
        this.isLimitActive = active;
    }

    public String getId() { return id; }
    public String getWorldName() { return worldName; }
    public String getDimension() { return dimension; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    public String getColor() { return color; }
    public boolean hasNoEntry() { return hasNoEntry; }
    public boolean hasNoExit() { return hasNoExit; }
    public Set<String> getNoEntryExceptions() { return new HashSet<>(noEntryExceptions); }
    public Set<String> getNoExitExceptions() { return new HashSet<>(noExitExceptions); }
    public boolean hasLimit() { return hasLimit; }
    public boolean isLimitActive() { return isLimitActive; }
    public int getPriority() { return priority; }
    public String getSkybox() { return skybox; }
    public boolean hasSkybox() { return skybox != null && !skybox.isEmpty(); }

    public String getType() { return type; }
    public boolean isFlat() { return "flat".equals(type); }
    public int getFlatPosition() { return flatPosition; }
    public boolean isPassNegative() { return passNegative; }
    public boolean isPassPositive() { return passPositive; }

    public boolean isWithinFlatRect(double x, double y, double z, String worldName, String worldDimension) {
        if (!this.worldName.equals(worldName)) return false;
        if (!this.dimension.equals(worldDimension)) return false;
        return switch (getFlatAxis()) {
            case 0 -> y >= minY && y < maxY && z >= minZ && z < maxZ;
            case 1 -> x >= minX && x < maxX && z >= minZ && z < maxZ;
            default -> x >= minX && x < maxX && y >= minY && y < maxY;
        };
    }
}