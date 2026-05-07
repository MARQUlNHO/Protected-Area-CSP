package me.marquinho.protectedareaclient.mixin;

import me.marquinho.protectedareaclient.client.ProtectedareaclientClient;
import me.marquinho.protectedareaclient.client.managers.AreaCollisionManager;
import me.marquinho.protectedareaclient.client.managers.AreaTracker;
import me.marquinho.protectedareaclient.client.models.ProtectedArea;
import me.marquinho.protectedareaclient.client.network.ClientNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Unique
    private static int collisionNotificationCooldown = 0;

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3d onMove(Vec3d movement, MovementType type) {
        Entity entity = (Entity) (Object) this;

        if (entity.getWorld().isClient()) {
            AreaTracker tracker = ProtectedareaclientClient.getAreaTracker();

            if (tracker != null) {
                Vec3d adjustedMovement = AreaCollisionManager.adjustMovement(entity, movement, tracker);

                if (!adjustedMovement.equals(movement) && entity instanceof ClientPlayerEntity) {
                    notifyCollision((ClientPlayerEntity) entity, tracker);
                }

                return adjustedMovement;
            }
        }

        return movement;
    }

    @Unique
    private void notifyCollision(ClientPlayerEntity player, AreaTracker tracker) {
        if (collisionNotificationCooldown > 0) {
            collisionNotificationCooldown--;
            return;
        }

        Vec3d pos = player.getPos();
        String worldName = getWorldName(player.getWorld().getRegistryKey());
        String dimension = getDimensionKey(player.getWorld().getRegistryKey());
        String playerName = player.getName().getString();

        for (ProtectedArea area : tracker.getAreas().values()) {
            if (!area.getWorldName().equals(worldName) || !area.getDimension().equals(dimension)) {
                continue;
            }

            if (!area.hasCollisionRule()) {
                continue;
            }

            boolean isInside = area.isInside(pos.x, pos.y, pos.z, worldName, dimension);

            if (area.shouldCollide(isInside, playerName)) {
                boolean isNoEntry = area.hasNoEntry() && !isInside;
                ClientNetworkHandler.requestCollisionNotification(area.getId(), isNoEntry);

                collisionNotificationCooldown = 40;
                break;
            }
        }
    }

    @Unique
    private String getWorldName(net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey) {
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

    @Unique
    private String getDimensionKey(net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimension) {
        if (dimension == net.minecraft.world.World.NETHER) {
            return "minecraft:the_nether";
        } else if (dimension == net.minecraft.world.World.END) {
            return "minecraft:the_end";
        } else {
            return "minecraft:overworld";
        }
    }
}