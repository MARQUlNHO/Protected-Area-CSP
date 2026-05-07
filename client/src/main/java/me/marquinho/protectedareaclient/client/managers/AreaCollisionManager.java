package me.marquinho.protectedareaclient.client.managers;

import me.marquinho.protectedareaclient.client.models.ProtectedArea;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AreaCollisionManager {

    private static final double WALL_THICKNESS = 0.0;
    private static final double EPSILON = 0.001;

    public static List<Box> getCollisionBoxes(ProtectedArea area, boolean isPlayerInside, String playerName) {
        if (!area.shouldCollide(isPlayerInside, playerName)) {
            return Collections.emptyList();
        }

        List<Box> boxes = new ArrayList<>(6);

        double minX = area.getMinX();
        double minY = area.getMinY();
        double minZ = area.getMinZ();
        double maxX = area.getMaxX();
        double maxY = area.getMaxY();
        double maxZ = area.getMaxZ();

        double expand = 0.0;

        boxes.add(new Box(
                minX - WALL_THICKNESS,
                minY - expand,
                minZ - expand,
                minX,
                maxY + expand,
                maxZ + expand
        ));

        boxes.add(new Box(
                maxX,
                minY - expand,
                minZ - expand,
                maxX + WALL_THICKNESS,
                maxY + expand,
                maxZ + expand
        ));

        boxes.add(new Box(
                minX - expand,
                minY - WALL_THICKNESS,
                minZ - expand,
                maxX + expand,
                minY,
                maxZ + expand
        ));

        boxes.add(new Box(
                minX - expand,
                maxY,
                minZ - expand,
                maxX + expand,
                maxY + WALL_THICKNESS,
                maxZ + expand
        ));

        boxes.add(new Box(
                minX - expand,
                minY - expand,
                minZ - WALL_THICKNESS,
                maxX + expand,
                maxY + expand,
                minZ
        ));

        boxes.add(new Box(
                minX - expand,
                minY - expand,
                maxZ,
                maxX + expand,
                maxY + expand,
                maxZ + WALL_THICKNESS
        ));

        return boxes;
    }

    public static Vec3d adjustMovement(Entity entity, Vec3d movement, AreaTracker areaTracker) {
        if (areaTracker == null || movement.lengthSquared() < EPSILON) {
            return movement;
        }

        Vec3d currentPos = entity.getPos();
        Box entityBox = entity.getBoundingBox();

        String worldName = getWorldName(entity.getWorld().getRegistryKey());
        String dimension = getDimensionKey(entity.getWorld().getRegistryKey());

        String playerName = null;
        if (entity instanceof net.minecraft.client.network.ClientPlayerEntity) {
            playerName = entity.getName().getString();
        }

        Box targetBox = entityBox.offset(movement);

        double adjustedX = movement.x;
        double adjustedY = movement.y;
        double adjustedZ = movement.z;

        for (ProtectedArea area : areaTracker.getAreas().values()) {
            if (!area.getWorldName().equals(worldName) || !area.getDimension().equals(dimension)) {
                continue;
            }

            if (!area.hasCollisionRule()) {
                continue;
            }

            boolean isInside = area.isInside(currentPos.x, currentPos.y, currentPos.z, worldName, dimension);
            List<Box> collisionBoxes = getCollisionBoxes(area, isInside, playerName);

            for (Box collisionBox : collisionBoxes) {
                if (targetBox.intersects(collisionBox)) {
                    CollisionAxis axis = determineCollisionAxis(collisionBox, area);

                    switch (axis) {
                        case X:
                            if ((adjustedX > 0 && currentPos.x < collisionBox.minX) ||
                                    (adjustedX < 0 && currentPos.x > collisionBox.maxX)) {
                                adjustedX = 0;
                            }
                            break;

                        case Y:
                            if ((adjustedY > 0 && currentPos.y < collisionBox.minY) ||
                                    (adjustedY < 0 && currentPos.y > collisionBox.maxY)) {
                                adjustedY = 0;
                            }
                            break;

                        case Z:
                            if ((adjustedZ > 0 && currentPos.z < collisionBox.minZ) ||
                                    (adjustedZ < 0 && currentPos.z > collisionBox.maxZ)) {
                                adjustedZ = 0;
                            }
                            break;
                    }

                    targetBox = entityBox.offset(new Vec3d(adjustedX, adjustedY, adjustedZ));
                }
            }
        }

        for (ProtectedArea area : areaTracker.getAreas().values()) {
            if (!area.isFlat()) continue;
            if (!area.getWorldName().equals(worldName) || !area.getDimension().equals(dimension)) continue;
            if (area.isPassNegative() && area.isPassPositive()) continue;
            if (!area.isWithinFlatRect(currentPos.x, currentPos.y, currentPos.z, worldName, dimension)) continue;

            int axis = area.getFlatAxis();
            double planeCoord = area.getFlatPlaneCoord();
            double currentCoord = switch (axis) { case 0 -> currentPos.x; case 1 -> currentPos.y; default -> currentPos.z; };
            double delta        = switch (axis) { case 0 -> adjustedX;    case 1 -> adjustedY;    default -> adjustedZ; };
            double targetCoord  = currentCoord + delta;

            boolean crossingToPositive = currentCoord < planeCoord && targetCoord >= planeCoord;
            boolean crossingToNegative = currentCoord >= planeCoord && targetCoord < planeCoord;

            if ((crossingToPositive && !area.isPassPositive()) || (crossingToNegative && !area.isPassNegative())) {
                switch (axis) {
                    case 0 -> adjustedX = 0;
                    case 1 -> adjustedY = 0;
                    default -> adjustedZ = 0;
                }
            }
        }

        return new Vec3d(adjustedX, adjustedY, adjustedZ);
    }

    private static CollisionAxis determineCollisionAxis(Box box, ProtectedArea area) {
        double sizeX = box.maxX - box.minX;
        double sizeY = box.maxY - box.minY;
        double sizeZ = box.maxZ - box.minZ;

        if (sizeX < sizeY && sizeX < sizeZ) {
            return CollisionAxis.X;
        } else if (sizeY < sizeX && sizeY < sizeZ) {
            return CollisionAxis.Y;
        } else {
            return CollisionAxis.Z;
        }
    }

    private enum CollisionAxis {
        X, Y, Z
    }

    private static String getWorldName(net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey) {
        String path = worldKey.getValue().getPath();
        if (worldKey == net.minecraft.world.World.OVERWORLD || path.equals("overworld")) {
            return "world";
        } else if (worldKey == net.minecraft.world.World.NETHER || path.equals("the_nether")) {
            return "world_nether";
        } else if (worldKey == net.minecraft.world.World.END || path.equals("the_end")) {
            return "world_the_end";
        } else {
            return path;
        }
    }

    private static String getDimensionKey(net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimension) {
        if (dimension == net.minecraft.world.World.NETHER) {
            return "minecraft:the_nether";
        } else if (dimension == net.minecraft.world.World.END) {
            return "minecraft:the_end";
        } else {
            return "minecraft:overworld";
        }
    }
}