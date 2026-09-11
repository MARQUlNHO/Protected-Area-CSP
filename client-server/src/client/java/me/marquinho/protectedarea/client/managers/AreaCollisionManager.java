package me.marquinho.protectedarea.client.managers;

import me.marquinho.protectedarea.client.models.ProtectedArea;
import me.marquinho.protectedarea.client.util.WorldDimensionUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AreaCollisionManager {

    private static final double WALL_THICKNESS = 0.0;

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
        if (areaTracker == null) {
            return movement;
        }

        Vec3d currentPos = entity.getPos();
        Box entityBox = entity.getBoundingBox();

        String dimension = WorldDimensionUtil.getDimensionKey(entity.getWorld().getRegistryKey());

        String playerName = null;
        if (entity instanceof ClientPlayerEntity) {
            playerName = entity.getName().getString();
        }

        Box targetBox = entityBox.offset(movement);

        double adjustedX = movement.x;
        double adjustedY = movement.y;
        double adjustedZ = movement.z;

        for (ProtectedArea area : areaTracker.getAreas().values()) {
            if (!area.getDimension().equals(dimension)) {
                continue;
            }

            if (!area.hasCollisionRule()) {
                continue;
            }

            boolean isInside = area.isInside(currentPos.x, currentPos.y, currentPos.z, dimension);
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
            if (!area.getDimension().equals(dimension)) continue;
            if (area.isPassNegative() && area.isPassPositive()) continue;
            if (!area.isWithinFlatRect(currentPos.x, currentPos.y, currentPos.z, dimension)) continue;

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

}