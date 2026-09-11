package me.marquinho.protectedarea.mixin.client;

import me.marquinho.protectedarea.client.ProtectedareaClient;
import me.marquinho.protectedarea.client.managers.AreaCollisionManager;
import me.marquinho.protectedarea.client.managers.AreaTracker;
import me.marquinho.protectedarea.client.models.ProtectedArea;
import me.marquinho.protectedarea.client.network.ClientNetworkHandler;
import me.marquinho.protectedarea.client.util.WorldDimensionUtil;
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
            AreaTracker tracker = ProtectedareaClient.getAreaTracker();

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
        String dimension = WorldDimensionUtil.getDimensionKey(player.getWorld().getRegistryKey());
        String playerName = player.getName().getString();

        for (ProtectedArea area : tracker.getAreas().values()) {
            if (!area.getDimension().equals(dimension)) {
                continue;
            }

            if (!area.hasCollisionRule()) {
                continue;
            }

            boolean isInside = area.isInside(pos.x, pos.y, pos.z, dimension);

            if (area.shouldCollide(isInside, playerName)) {
                boolean isNoEntry = area.hasNoEntry() && !isInside;
                ClientNetworkHandler.requestCollisionNotification(area.getId(), isNoEntry);

                collisionNotificationCooldown = 40;
                break;
            }
        }
    }

}