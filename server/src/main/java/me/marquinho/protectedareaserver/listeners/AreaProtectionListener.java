package me.marquinho.protectedareaserver.listeners;

import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.api.ProtectedAreaServerEvents;
import me.marquinho.protectedareaserver.managers.NotificationManager;
import me.marquinho.protectedareaserver.models.AdvancedAreaRules;
import me.marquinho.protectedareaserver.models.AdvancedRuleType;
import me.marquinho.protectedareaserver.models.AreaRule;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class AreaProtectionListener {

    public static AreaProtectionListener INSTANCE;

    private final ProtectedAreaInit plugin;

    public final HashMap<UUID, Set<UUID>> attemptedPickups = new HashMap<>();

    private AreaProtectionListener(ProtectedAreaInit plugin) {
        this.plugin = plugin;
    }

    public static void register(ProtectedAreaInit plugin) {
        AreaProtectionListener listener = new AreaProtectionListener(plugin);
        INSTANCE = listener;

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (!(player instanceof ServerPlayerEntity sPlayer)) return true;
            if (!(world instanceof ServerWorld sw)) return true;

            String dim = sw.getRegistryKey().getValue().toString();
            ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, pos.getX(), pos.getY(), pos.getZ());
            if (area == null) return true;
            if (area.hasException(sPlayer.getGameProfile().getName(), "no_break")) return true;

            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getId());
            Map<String, String> ph = NotificationManager.createFullPlaceholders(blockId, null, area.getId(), sPlayer.getGameProfile().getName());

            if (plugin.getAreaManager().hasInheritedAdvancedBlock(dim, pos.getX(), pos.getY(), pos.getZ(), AdvancedRuleType.NO_BREAK, blockId)) {
                plugin.getNotificationManager().sendAdvancedRulesMessage(sPlayer, "no_break_specific", ph);
                ProtectedAreaServerEvents.ADVANCED_RULE_BLOCKED.invoker().onAdvancedRuleBlocked(sPlayer, area, AdvancedRuleType.NO_BREAK, blockId);
                return false;
            }

            if (plugin.getAreaManager().hasInheritedRule(dim, pos.getX(), pos.getY(), pos.getZ(), AreaRule.NO_BREAK)) {
                if (adv.hasBlock(AdvancedRuleType.YES_BREAK, blockId)) return true;
                plugin.getNotificationManager().sendNotification(sPlayer, "Rules", "no_break", ph);
                ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(sPlayer, area, AreaRule.NO_BREAK);
                return false;
            }
            return true;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity sPlayer)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            String dim = sw.getRegistryKey().getValue().toString();

            var stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof BucketItem) {
                ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, pos.getX(), pos.getY(), pos.getZ());
                if (area == null) return ActionResult.PASS;
                if (area.hasException(sPlayer.getGameProfile().getName(), "no_place")) return ActionResult.PASS;

                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                String fluidId = "minecraft:" + itemId.replace("minecraft:", "").replace("_bucket", "");
                AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getId());
                Map<String, String> ph = NotificationManager.createFullPlaceholders(fluidId, null, area.getId(), sPlayer.getGameProfile().getName());

                if (plugin.getAreaManager().hasInheritedAdvancedBlock(dim, pos.getX(), pos.getY(), pos.getZ(), AdvancedRuleType.NO_PLACE, fluidId)) {
                    plugin.getNotificationManager().sendAdvancedRulesMessage(sPlayer, "no_place_fluid_specific", ph);
                    ProtectedAreaServerEvents.ADVANCED_RULE_BLOCKED.invoker().onAdvancedRuleBlocked(sPlayer, area, AdvancedRuleType.NO_PLACE, fluidId);
                    return ActionResult.FAIL;
                }

                if (plugin.getAreaManager().hasInheritedRule(dim, pos.getX(), pos.getY(), pos.getZ(), AreaRule.NO_PLACE)) {
                    if (adv.hasBlock(AdvancedRuleType.YES_PLACE, fluidId)) return ActionResult.PASS;
                    plugin.getNotificationManager().sendNotification(sPlayer, "Rules", "no_place_fluid", ph);
                    ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(sPlayer, area, AreaRule.NO_PLACE);
                    return ActionResult.FAIL;
                }

                return ActionResult.PASS;
            }

            ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, pos.getX(), pos.getY(), pos.getZ());
            if (area == null) return ActionResult.PASS;
            if (area.hasException(sPlayer.getGameProfile().getName(), "no_interact")) return ActionResult.PASS;

            String blockId = Registries.BLOCK.getId(sw.getBlockState(pos).getBlock()).toString();
            AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getId());
            Map<String, String> ph = NotificationManager.createFullPlaceholders(blockId, null, area.getId(), sPlayer.getGameProfile().getName());

            if (plugin.getAreaManager().hasInheritedAdvancedBlock(dim, pos.getX(), pos.getY(), pos.getZ(), AdvancedRuleType.NO_INTERACT, blockId)) {
                plugin.getNotificationManager().sendAdvancedRulesMessage(sPlayer, "no_interact_block", ph);
                ProtectedAreaServerEvents.ADVANCED_RULE_BLOCKED.invoker().onAdvancedRuleBlocked(sPlayer, area, AdvancedRuleType.NO_INTERACT, blockId);
                return ActionResult.FAIL;
            }

            if (plugin.getAreaManager().hasInheritedRule(dim, pos.getX(), pos.getY(), pos.getZ(), AreaRule.NO_INTERACT)) {
                if (adv.hasBlock(AdvancedRuleType.YES_INTERACT, blockId)) return ActionResult.PASS;
                plugin.getNotificationManager().sendNotification(sPlayer, "Rules", "no_interact", ph);
                ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(sPlayer, area, AreaRule.NO_INTERACT);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity sPlayer)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

            double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
            String dim = sw.getRegistryKey().getValue().toString();
            ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, ex, ey, ez);
            if (area == null) return ActionResult.PASS;
            if (area.hasException(sPlayer.getGameProfile().getName(), "no_interact")) return ActionResult.PASS;

            String entityId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getId());
            Map<String, String> ph = NotificationManager.createFullPlaceholders(null, entityId, area.getId(), sPlayer.getGameProfile().getName());

            if (plugin.getAreaManager().hasInheritedAdvancedEntity(dim, ex, ey, ez, AdvancedRuleType.NO_INTERACT, entityId)) {
                plugin.getNotificationManager().sendAdvancedRulesMessage(sPlayer, "no_interact_entity", ph);
                ProtectedAreaServerEvents.ADVANCED_RULE_BLOCKED.invoker().onAdvancedRuleBlocked(sPlayer, area, AdvancedRuleType.NO_INTERACT, entityId);
                return ActionResult.FAIL;
            }

            if (plugin.getAreaManager().hasInheritedRule(dim, ex, ey, ez, AreaRule.NO_INTERACT)) {
                if (adv.hasEntity(AdvancedRuleType.YES_INTERACT, entityId)) return ActionResult.PASS;
                plugin.getNotificationManager().sendNotification(sPlayer, "Rules", "no_interact_entity", ph);
                ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(sPlayer, area, AreaRule.NO_INTERACT);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity attacker)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

            double ex = entity.getX(), ey = entity.getY(), ez = entity.getZ();
            String dim = sw.getRegistryKey().getValue().toString();
            ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, ex, ey, ez);
            if (area == null) return ActionResult.PASS;
            String attackerName = attacker.getGameProfile().getName();

            if (entity instanceof ServerPlayerEntity) {
                if (area.hasException(attackerName, "no_pvp")) return ActionResult.PASS;
                if (plugin.getAreaManager().hasInheritedRule(dim, ex, ey, ez, AreaRule.NO_PVP)) {
                    Map<String, String> ph = NotificationManager.createFullPlaceholders(null, null, area.getId(), attackerName);
                    ph.put("victim", ((ServerPlayerEntity) entity).getGameProfile().getName());
                    plugin.getNotificationManager().sendNotification(attacker, "Rules", "no_pvp", ph);
                    ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(attacker, area, AreaRule.NO_PVP);
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            if (area.hasException(attackerName, "no_entityattack")) return ActionResult.PASS;
            if (plugin.getAreaManager().hasInheritedRule(dim, ex, ey, ez, AreaRule.NO_ENTITYATTACK)) {
                Map<String, String> ph = NotificationManager.createFullPlaceholders(null, null, area.getId(), attackerName);
                plugin.getNotificationManager().sendNotification(attacker, "Rules", "no_entityattack", ph);
                ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(attacker, area, AreaRule.NO_ENTITYATTACK);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity sPlayer)) return true;
            if (!(entity.getWorld() instanceof ServerWorld sw)) return true;

            String dim = sw.getRegistryKey().getValue().toString();
            ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, sPlayer.getX(), sPlayer.getY(), sPlayer.getZ());
            if (area == null) return true;
            if (area.hasException(sPlayer.getGameProfile().getName(), "no_damage")) return true;

            if (plugin.getAreaManager().hasInheritedRule(dim, sPlayer.getX(), sPlayer.getY(), sPlayer.getZ(), AreaRule.NO_DAMAGE)) {
                if (source.getTypeRegistryEntry().getIdAsString().contains("out_of_world") ||
                    source.getTypeRegistryEntry().getIdAsString().contains("generic_kill")) {
                    return true;
                }
                Entity attacker = source.getAttacker();
                if (attacker instanceof PlayerEntity || (attacker instanceof ProjectileEntity)) {
                    return false;
                }
                return false;
            }
            return true;
        });
    }

    public boolean onBlockPlace(ServerPlayerEntity player, ServerWorld world, BlockPos pos, String blockId) {
        String dim = world.getRegistryKey().getValue().toString();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, pos.getX(), pos.getY(), pos.getZ());
        if (area == null) return true;
        if (area.hasException(player.getGameProfile().getName(), "no_place")) return true;

        AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getId());
        Map<String, String> ph = NotificationManager.createFullPlaceholders(blockId, null, area.getId(), player.getGameProfile().getName());

        if (plugin.getAreaManager().hasInheritedAdvancedBlock(dim, pos.getX(), pos.getY(), pos.getZ(), AdvancedRuleType.NO_PLACE, blockId)) {
            plugin.getNotificationManager().sendAdvancedRulesMessage(player, "no_place_specific", ph);
            ProtectedAreaServerEvents.ADVANCED_RULE_BLOCKED.invoker().onAdvancedRuleBlocked(player, area, AdvancedRuleType.NO_PLACE, blockId);
            return false;
        }

        if (plugin.getAreaManager().hasInheritedRule(dim, pos.getX(), pos.getY(), pos.getZ(), AreaRule.NO_PLACE)) {
            if (adv.hasBlock(AdvancedRuleType.YES_PLACE, blockId)) return true;
            plugin.getNotificationManager().sendNotification(player, "Rules", "no_place", ph);
            ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(player, area, AreaRule.NO_PLACE);
            return false;
        }
        return true;
    }

    public boolean onItemDrop(ServerPlayerEntity player, String itemId) {
        String dim = player.getServerWorld().getRegistryKey().getValue().toString();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, player.getX(), player.getY(), player.getZ());
        if (area == null) return true;
        if (area.hasException(player.getGameProfile().getName(), "no_drop")) return true;

        AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getId());
        Map<String, String> ph = NotificationManager.createFullPlaceholders(itemId, null, area.getId(), player.getGameProfile().getName());

        if (plugin.getAreaManager().hasInheritedAdvancedItem(dim, player.getX(), player.getY(), player.getZ(), AdvancedRuleType.NO_DROP, itemId)) {
            plugin.getNotificationManager().sendAdvancedRulesMessage(player, "no_drop_specific", ph);
            ProtectedAreaServerEvents.ADVANCED_RULE_BLOCKED.invoker().onAdvancedRuleBlocked(player, area, AdvancedRuleType.NO_DROP, itemId);
            return false;
        }

        if (plugin.getAreaManager().hasInheritedRule(dim, player.getX(), player.getY(), player.getZ(), AreaRule.NO_DROP)) {
            if (adv.hasItem(AdvancedRuleType.YES_DROP, itemId)) return true;
            plugin.getNotificationManager().sendNotification(player, "Rules", "no_drop", ph);
            ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(player, area, AreaRule.NO_DROP);
            return false;
        }
        return true;
    }

    public boolean onItemPickup(ServerPlayerEntity player, ItemEntity itemEntity, String itemId) {
        String dim = player.getServerWorld().getRegistryKey().getValue().toString();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, player.getX(), player.getY(), player.getZ());
        if (area == null) {
            attemptedPickups.remove(player.getUuid());
            return true;
        }
        if (area.hasException(player.getGameProfile().getName(), "no_collect")) return true;

        AdvancedAreaRules adv = plugin.getAdvancedRulesManager().getRules(area.getId());
        Map<String, String> ph = NotificationManager.createFullPlaceholders(itemId, null, area.getId(), player.getGameProfile().getName());

        if (plugin.getAreaManager().hasInheritedAdvancedItem(dim, player.getX(), player.getY(), player.getZ(), AdvancedRuleType.NO_COLLECT, itemId)) {
            notifyPickupOnce(player, itemEntity, () ->
                plugin.getNotificationManager().sendAdvancedRulesMessage(player, "no_collect_specific", ph));
            ProtectedAreaServerEvents.ADVANCED_RULE_BLOCKED.invoker().onAdvancedRuleBlocked(player, area, AdvancedRuleType.NO_COLLECT, itemId);
            return false;
        }

        if (plugin.getAreaManager().hasInheritedRule(dim, player.getX(), player.getY(), player.getZ(), AreaRule.NO_COLLECT)) {
            if (adv.hasItem(AdvancedRuleType.YES_COLLECT, itemId)) return true;
            notifyPickupOnce(player, itemEntity, () ->
                plugin.getNotificationManager().sendNotification(player, "Rules", "no_collect", ph));
            ProtectedAreaServerEvents.RULE_BLOCKED.invoker().onRuleBlocked(player, area, AreaRule.NO_COLLECT);
            return false;
        }
        return true;
    }

    private void notifyPickupOnce(ServerPlayerEntity player, ItemEntity item, Runnable notify) {
        UUID pid = player.getUuid();
        UUID eid = item.getUuid();
        Set<UUID> attempts = attemptedPickups.computeIfAbsent(pid, k -> new HashSet<>());
        if (!attempts.contains(eid)) {
            notify.run();
            attempts.add(eid);
        }
    }

    public void onItemDespawn(UUID itemEntityId) {
        for (Set<UUID> attempts : attemptedPickups.values()) {
            attempts.remove(itemEntityId);
        }
    }

    public boolean onMobSpawn(net.minecraft.entity.EntityType<?> type, ServerWorld world, BlockPos pos) {
        String dim = world.getRegistryKey().getValue().toString();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(dim, pos.getX(), pos.getY(), pos.getZ());
        if (area == null) return true;
        if (plugin.getAreaManager().hasInheritedRule(dim, pos.getX(), pos.getY(), pos.getZ(), AreaRule.NO_SPAWN)) {
            return false;
        }
        return true;
    }

    public boolean onExplosionBlockDestruction(String dimension, double x, double y, double z) {
        ProtectedArea area = plugin.getAreaManager().getAreaAt(dimension, x, y, z);
        if (area == null) return true;
        if (plugin.getAreaManager().hasInheritedRule(dimension, x, y, z, AreaRule.NO_MOBGRIEFING)) {
            return false;
        }
        return true;
    }

    public ProtectedAreaInit getPlugin() { return plugin; }
}
