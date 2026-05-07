package me.marquinho.protectedareaclient.client.render;

import me.marquinho.protectedareaclient.client.ProtectedareaclientClient;
import me.marquinho.protectedareaclient.client.managers.AreaTracker;
import me.marquinho.protectedareaclient.client.models.ProtectedArea;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

    public class AreaOutlineRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(AreaOutlineRenderer::renderAreas);
    }

    private static void renderAreas(WorldRenderContext context) {
        AreaTracker tracker = ProtectedareaclientClient.getAreaTracker();
        if (tracker == null || !tracker.isViewEnabled()) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider vertexConsumers = context.consumers();
        if (vertexConsumers == null) return;

        Vec3d camera = context.camera().getPos();

        String currentWorldName = getWorldName(context.world().getRegistryKey());
        String currentDimension = getDimensionKey(context.world().getRegistryKey());

        matrices.push();

        int areasRendered = 0;
        for (ProtectedArea area : tracker.getAreas().values()) {

            if (!area.getWorldName().equals(currentWorldName)) {
                continue;
            }

            if (!area.getDimension().equals(currentDimension)) {
                continue;
            }

            areasRendered++;

            float red, green, blue;
            try {
                String hex = area.getColor();
                int colorInt = Integer.parseInt(hex.substring(1), 16);
                red = ((colorInt >> 16) & 0xFF) / 255f;
                green = ((colorInt >> 8) & 0xFF) / 255f;
                blue = (colorInt & 0xFF) / 255f;
            } catch (Exception e) {
                red = green = blue = 1f;
            }
            float alpha = 0.8f;

            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());

            if (area.isFlat()) {
                renderFlatArea(matrices, vertexConsumer, area, camera, red, green, blue, alpha);
            } else {
                Box box = new Box(
                        area.getMinX() - camera.x,
                        area.getMinY() - camera.y,
                        area.getMinZ() - camera.z,
                        area.getMaxX() - camera.x,
                        area.getMaxY() - camera.y,
                        area.getMaxZ() - camera.z
                );
                WorldRenderer.drawBox(matrices, vertexConsumer, box, red, green, blue, alpha);
            }
        }

        matrices.pop();
    }

    private static String getWorldName(RegistryKey<World> worldKey) {
        String path = worldKey.getValue().getPath();

        if (worldKey == World.OVERWORLD || path.equals("overworld")) {
            return "world";
        } else if (worldKey == World.NETHER || path.equals("the_nether")) {
            return "world_nether";
        } else if (worldKey == World.END || path.equals("the_end")) {
            return "world_the_end";
        } else {
            return path;
        }
    }

    private static String getDimensionKey(RegistryKey<World> dimension) {
        if (dimension == World.NETHER) {
            return "minecraft:the_nether";
        } else if (dimension == World.END) {
            return "minecraft:the_end";
        } else {
            return "minecraft:overworld";
        }
    }

    private static void renderFlatArea(MatrixStack matrices, VertexConsumer vc,
                                       ProtectedArea area, Vec3d camera,
                                       float r, float g, float b, float a) {
        double fp = area.getFlatPosition() / 16.0;
        double minX = area.getMinX(), maxX = area.getMaxX();
        double minY = area.getMinY(), maxY = area.getMaxY();
        double minZ = area.getMinZ(), maxZ = area.getMaxZ();
        double cx = camera.x, cy = camera.y, cz = camera.z;

        if (maxX - minX <= 1) {
            double x = minX + fp - cx;
            double ay = minY - cy, by = maxY - cy;
            double az = minZ - cz, bz = maxZ - cz;
            drawLine(matrices, vc, x, ay, az,  x, ay, bz,  r, g, b, a);
            drawLine(matrices, vc, x, ay, bz,  x, by, bz,  r, g, b, a);
            drawLine(matrices, vc, x, by, bz,  x, by, az,  r, g, b, a);
            drawLine(matrices, vc, x, by, az,  x, ay, az,  r, g, b, a);
        } else if (maxY - minY <= 1) {
            double y = minY + fp - cy;
            double ax = minX - cx, bx = maxX - cx;
            double az = minZ - cz, bz = maxZ - cz;
            drawLine(matrices, vc, ax, y, az,  bx, y, az,  r, g, b, a);
            drawLine(matrices, vc, bx, y, az,  bx, y, bz,  r, g, b, a);
            drawLine(matrices, vc, bx, y, bz,  ax, y, bz,  r, g, b, a);
            drawLine(matrices, vc, ax, y, bz,  ax, y, az,  r, g, b, a);
        } else {
            double z = minZ + fp - cz;
            double ax = minX - cx, bx = maxX - cx;
            double ay = minY - cy, by = maxY - cy;
            drawLine(matrices, vc, ax, ay, z,  bx, ay, z,  r, g, b, a);
            drawLine(matrices, vc, bx, ay, z,  bx, by, z,  r, g, b, a);
            drawLine(matrices, vc, bx, by, z,  ax, by, z,  r, g, b, a);
            drawLine(matrices, vc, ax, by, z,  ax, ay, z,  r, g, b, a);
        }
    }

    private static void drawLine(MatrixStack matrices, VertexConsumer vc,
                                  double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  float r, float g, float b, float a) {
        var pose = matrices.peek();
        float dx = (float)(x2 - x1);
        float dy = (float)(y2 - y1);
        float dz = (float)(z2 - z1);
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len == 0) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        vc.vertex(pose.getPositionMatrix(), (float)x1, (float)y1, (float)z1)
          .color(r, g, b, a).normal(nx, ny, nz);
        vc.vertex(pose.getPositionMatrix(), (float)x2, (float)y2, (float)z2)
          .color(r, g, b, a).normal(nx, ny, nz);
    }
}