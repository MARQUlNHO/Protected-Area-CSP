package me.marquinho.protectedAreaPlugin.listeners;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.api.events.AreaAdvancedRuleBlockedEvent;
import me.marquinho.protectedAreaPlugin.api.events.AreaRuleBlockedEvent;
import me.marquinho.protectedAreaPlugin.managers.NotificationManager;
import me.marquinho.protectedAreaPlugin.models.AdvancedAreaRules;
import me.marquinho.protectedAreaPlugin.models.AdvancedRuleType;
import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

public class AreaProtectionListener implements Listener {
    private final ProtectedAreaPlugin plugin;
    private final NotificationManager notificationManager;

    private final HashMap<UUID, Set<UUID>> attemptedPickups = new HashMap<>();

    public AreaProtectionListener(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
        this.notificationManager = plugin.getNotificationManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(event.getBlock().getLocation());

        if (area == null) return;

        if (area.hasException(player.getName(), "no_break")) {
            return;
        }

        String blockId = event.getBlock().getType().getKey().toString();
        AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                blockId,
                null,
                area.getId(),
                player.getName()
        );

        if (plugin.getAreaManager().hasInheritedAdvancedBlock(event.getBlock().getLocation(), AdvancedRuleType.NO_BREAK, blockId)) {
            event.setCancelled(true);
            notificationManager.sendAdvancedRulesMessage(player, "no_break_specific", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaAdvancedRuleBlockedEvent(player, area, AdvancedRuleType.NO_BREAK, blockId));
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(event.getBlock().getLocation(), AreaRule.NO_BREAK)) {
            if (advancedRules.hasBlock(AdvancedRuleType.YES_BREAK, blockId)) {
                return;
            }

            event.setCancelled(true);
            notificationManager.sendNotification(player, "Rules", "no_break", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(player, area, AreaRule.NO_BREAK));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(event.getBlock().getLocation());

        if (area == null) return;

        if (area.hasException(player.getName(), "no_place")) {
            return;
        }

        String blockId = event.getBlock().getType().getKey().toString();
        AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                blockId,
                null,
                area.getId(),
                player.getName()
        );

        if (plugin.getAreaManager().hasInheritedAdvancedBlock(event.getBlock().getLocation(), AdvancedRuleType.NO_PLACE, blockId)) {
            event.setCancelled(true);
            notificationManager.sendAdvancedRulesMessage(player, "no_place_specific", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaAdvancedRuleBlockedEvent(player, area, AdvancedRuleType.NO_PLACE, blockId));
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(event.getBlock().getLocation(), AreaRule.NO_PLACE)) {
            if (advancedRules.hasBlock(AdvancedRuleType.YES_PLACE, blockId)) {
                return;
            }

            event.setCancelled(true);
            notificationManager.sendNotification(player, "Rules", "no_place", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(player, area, AreaRule.NO_PLACE));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(event.getBlock().getLocation());

        if (area == null) return;

        if (area.hasException(player.getName(), "no_place")) {
            return;
        }

        Material bucket = event.getBucket();
        String fluidId = bucket.toString().toLowerCase().replace("_bucket", "");
        fluidId = "minecraft:" + fluidId;

        AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                fluidId,
                null,
                area.getId(),
                player.getName()
        );

        if (plugin.getAreaManager().hasInheritedAdvancedBlock(event.getBlock().getLocation(), AdvancedRuleType.NO_PLACE, fluidId)) {
            event.setCancelled(true);
            notificationManager.sendAdvancedRulesMessage(player, "no_place_fluid_specific", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaAdvancedRuleBlockedEvent(player, area, AdvancedRuleType.NO_PLACE, fluidId));
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(event.getBlock().getLocation(), AreaRule.NO_PLACE)) {
            if (advancedRules.hasBlock(AdvancedRuleType.YES_PLACE, fluidId)) {
                return;
            }

            event.setCancelled(true);
            notificationManager.sendNotification(player, "Rules", "no_place_fluid", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(player, area, AreaRule.NO_PLACE));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getClickedBlock() != null) {
            ProtectedArea area = plugin.getAreaManager().getAreaAt(event.getClickedBlock().getLocation());

            if (area == null) return;

            if (area.hasException(player.getName(), "no_interact")) {
                return;
            }

            String blockId = event.getClickedBlock().getType().getKey().toString();
            AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

            Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                    blockId,
                    null,
                    area.getId(),
                    player.getName()
            );

            if (plugin.getAreaManager().hasInheritedAdvancedBlock(event.getClickedBlock().getLocation(), AdvancedRuleType.NO_INTERACT, blockId)) {
                event.setCancelled(true);
                notificationManager.sendAdvancedRulesMessage(player, "no_interact_block", placeholders);
                plugin.getServer().getPluginManager().callEvent(new AreaAdvancedRuleBlockedEvent(player, area, AdvancedRuleType.NO_INTERACT, blockId));
                return;
            }

            if (plugin.getAreaManager().hasInheritedRule(event.getClickedBlock().getLocation(), AreaRule.NO_INTERACT)) {
                if (advancedRules.hasBlock(AdvancedRuleType.YES_INTERACT, blockId)) {
                    return;
                }

                event.setCancelled(true);
                notificationManager.sendNotification(player, "Rules", "no_interact", placeholders);
                plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(player, area, AreaRule.NO_INTERACT));
                return;
            }
        }

        ProtectedArea area = plugin.getAreaManager().getAreaAt(player.getLocation());
        if (area != null) {
            if (area.hasException(player.getName(), "no_interact")) {
                return;
            }

            if (plugin.getAreaManager().hasInheritedRule(player.getLocation(), AreaRule.NO_INTERACT)) {
                event.setCancelled(true);

                Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                        null,
                        null,
                        area.getId(),
                        player.getName()
                );

                notificationManager.sendNotification(player, "Rules", "no_interact", placeholders);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
                plugin.getAreaManager().hasInheritedRule(block.getLocation(), AreaRule.NO_MOBGRIEFING)
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }

        if (attacker == null) return;

        ProtectedArea area = plugin.getAreaManager().getAreaAt(victim.getLocation());

        if (area == null) return;

        if (area.hasException(attacker.getName(), "no_pvp")) {
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(victim.getLocation(), AreaRule.NO_PVP)) {
            event.setCancelled(true);

            Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                    null,
                    null,
                    area.getId(),
                    attacker.getName()
            );
            placeholders.put("victim", victim.getName());

            notificationManager.sendNotification(attacker, "Rules", "no_pvp", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(attacker, area, AreaRule.NO_PVP));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (event.getEntity() instanceof Player) return;

        ProtectedArea area = plugin.getAreaManager().getAreaAt(event.getEntity().getLocation());

        if (area == null) return;

        if (area.hasException(attacker.getName(), "no_entityattack")) {
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(event.getEntity().getLocation(), AreaRule.NO_ENTITYATTACK)) {
            event.setCancelled(true);

            Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                    null,
                    null,
                    area.getId(),
                    attacker.getName()
            );

            notificationManager.sendNotification(attacker, "Rules", "no_entityattack", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(attacker, area, AreaRule.NO_ENTITYATTACK));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ProtectedArea area = plugin.getAreaManager().getAreaAt(player.getLocation());

        if (area == null) return;

        if (area.hasException(player.getName(), "no_damage")) {
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(player.getLocation(), AreaRule.NO_DAMAGE)) {
            EntityDamageEvent.DamageCause cause = event.getCause();

            if (cause == EntityDamageEvent.DamageCause.SUICIDE ||
                    cause == EntityDamageEvent.DamageCause.VOID ||
                    cause == EntityDamageEvent.DamageCause.KILL) {
                return;
            }

            if (event instanceof EntityDamageByEntityEvent damageByEntity) {
                Entity damager = damageByEntity.getDamager();

                if (damager instanceof Player) {
                    event.setDamage(0);
                    return;
                }

                if (damager instanceof Projectile projectile) {
                    if (projectile.getShooter() instanceof Player) {
                        event.setDamage(0);
                        return;
                    }
                }
            }

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (event.getEntityType() == EntityType.PLAYER) return;

        if (plugin.getAreaManager().hasInheritedRule(event.getLocation(), AreaRule.NO_SPAWN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(event.getRightClicked().getLocation());

        if (area == null) return;

        if (area.hasException(player.getName(), "no_interact")) {
            return;
        }

        String entityId = event.getRightClicked().getType().getKey().toString();
        AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                null,
                entityId,
                area.getId(),
                player.getName()
        );

        if (plugin.getAreaManager().hasInheritedAdvancedEntity(event.getRightClicked().getLocation(), AdvancedRuleType.NO_INTERACT, entityId)) {
            event.setCancelled(true);
            notificationManager.sendAdvancedRulesMessage(player, "no_interact_entity", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaAdvancedRuleBlockedEvent(player, area, AdvancedRuleType.NO_INTERACT, entityId));
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(event.getRightClicked().getLocation(), AreaRule.NO_INTERACT)) {
            if (advancedRules.hasEntity(AdvancedRuleType.YES_INTERACT, entityId)) {
                return;
            }

            event.setCancelled(true);
            notificationManager.sendNotification(player, "Rules", "no_interact_entity", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(player, area, AreaRule.NO_INTERACT));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;

        ProtectedArea area = plugin.getAreaManager().getAreaAt(event.getVehicle().getLocation());

        if (area == null) return;

        if (area.hasException(player.getName(), "no_interact")) {
            return;
        }

        String vehicleId = event.getVehicle().getType().getKey().toString();
        AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                null,
                vehicleId,
                area.getId(),
                player.getName()
        );

        if (plugin.getAreaManager().hasInheritedAdvancedEntity(event.getVehicle().getLocation(), AdvancedRuleType.NO_INTERACT, vehicleId)) {
            event.setCancelled(true);
            notificationManager.sendAdvancedRulesMessage(player, "no_interact_vehicle", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaAdvancedRuleBlockedEvent(player, area, AdvancedRuleType.NO_INTERACT, vehicleId));
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(event.getVehicle().getLocation(), AreaRule.NO_INTERACT)) {
            if (advancedRules.hasEntity(AdvancedRuleType.YES_INTERACT, vehicleId)) {
                return;
            }

            event.setCancelled(true);
            notificationManager.sendNotification(player, "Rules", "no_interact_vehicle", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(player, area, AreaRule.NO_INTERACT));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof org.bukkit.entity.Entity entity) {
            ProtectedArea area = plugin.getAreaManager().getAreaAt(entity.getLocation());

            if (area == null) return;

            if (area.hasException(player.getName(), "no_interact")) {
                return;
            }

            String entityId = entity.getType().getKey().toString();
            AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

            Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                    null,
                    entityId,
                    area.getId(),
                    player.getName()
            );

            if (plugin.getAreaManager().hasInheritedAdvancedEntity(entity.getLocation(), AdvancedRuleType.NO_INTERACT, entityId)) {
                event.setCancelled(true);
                notificationManager.sendAdvancedRulesMessage(player, "no_interact_inventory", placeholders);
                return;
            }

            if (plugin.getAreaManager().hasInheritedRule(entity.getLocation(), AreaRule.NO_INTERACT)) {
                if (advancedRules.hasEntity(AdvancedRuleType.YES_INTERACT, entityId)) {
                    return;
                }

                event.setCancelled(true);
                notificationManager.sendNotification(player, "Rules", "no_interact_inventory", placeholders);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ProtectedArea area = plugin.getAreaManager().getAreaAt(player.getLocation());

        if (area == null) return;

        if (area.hasException(player.getName(), "no_drop")) {
            return;
        }

        String itemId = event.getItemDrop().getItemStack().getType().getKey().toString();
        AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                itemId,
                null,
                area.getId(),
                player.getName()
        );

        if (plugin.getAreaManager().hasInheritedAdvancedItem(player.getLocation(), AdvancedRuleType.NO_DROP, itemId)) {
            event.setCancelled(true);
            notificationManager.sendAdvancedRulesMessage(player, "no_drop_specific", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaAdvancedRuleBlockedEvent(player, area, AdvancedRuleType.NO_DROP, itemId));
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(player.getLocation(), AreaRule.NO_DROP)) {
            if (advancedRules.hasItem(AdvancedRuleType.YES_DROP, itemId)) {
                return;
            }

            event.setCancelled(true);
            notificationManager.sendNotification(player, "Rules", "no_drop", placeholders);
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(player, area, AreaRule.NO_DROP));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ProtectedArea area = plugin.getAreaManager().getAreaAt(player.getLocation());

        if (area == null) {
            attemptedPickups.remove(player.getUniqueId());
            return;
        }

        if (area.hasException(player.getName(), "no_collect")) {
            return;
        }

        String itemId = event.getItem().getItemStack().getType().getKey().toString();
        AdvancedAreaRules advancedRules = plugin.getAdvancedRulesManager().getRules(area.getId());

        Map<String, String> placeholders = NotificationManager.createFullPlaceholders(
                itemId,
                null,
                area.getId(),
                player.getName()
        );

        if (plugin.getAreaManager().hasInheritedAdvancedItem(player.getLocation(), AdvancedRuleType.NO_COLLECT, itemId)) {
            event.setCancelled(true);

            UUID playerId = player.getUniqueId();
            UUID itemEntityId = event.getItem().getUniqueId();
            Set<UUID> playerAttempts = attemptedPickups.computeIfAbsent(playerId, k -> new HashSet<>());

            if (!playerAttempts.contains(itemEntityId)) {
                notificationManager.sendAdvancedRulesMessage(player, "no_collect_specific", placeholders);
                playerAttempts.add(itemEntityId);
            }
            plugin.getServer().getPluginManager().callEvent(new AreaAdvancedRuleBlockedEvent(player, area, AdvancedRuleType.NO_COLLECT, itemId));
            return;
        }

        if (plugin.getAreaManager().hasInheritedRule(player.getLocation(), AreaRule.NO_COLLECT)) {
            if (advancedRules.hasItem(AdvancedRuleType.YES_COLLECT, itemId)) {
                return;
            }

            event.setCancelled(true);

            UUID playerId = player.getUniqueId();
            UUID itemEntityId = event.getItem().getUniqueId();
            Set<UUID> playerAttempts = attemptedPickups.computeIfAbsent(playerId, k -> new HashSet<>());

            if (!playerAttempts.contains(itemEntityId)) {
                notificationManager.sendNotification(player, "Rules", "no_collect", placeholders);
                playerAttempts.add(itemEntityId);
            }
            plugin.getServer().getPluginManager().callEvent(new AreaRuleBlockedEvent(player, area, AreaRule.NO_COLLECT));
        }
    }

    @EventHandler
    public void onItemDespawn(org.bukkit.event.entity.ItemDespawnEvent event) {
        UUID itemId = event.getEntity().getUniqueId();

        for (Set<UUID> attempts : attemptedPickups.values()) {
            attempts.remove(itemId);
        }
    }
}