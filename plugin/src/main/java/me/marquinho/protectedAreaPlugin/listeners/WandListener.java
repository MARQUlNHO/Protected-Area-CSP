package me.marquinho.protectedAreaPlugin.listeners;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.items.WandItems;
import me.marquinho.protectedAreaPlugin.managers.WandManager;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public class WandListener implements Listener {

    private final ProtectedAreaPlugin plugin;

    public WandListener(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        String wandType = WandItems.getWandType(plugin, event.getItem());
        if (wandType == null) return;

        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Block block = event.getClickedBlock();
            if (block == null) return;
            plugin.getWandManager().setPos1(player.getUniqueId(), toPoint(block));
            player.sendMessage("§aPosition 1: §6" + block.getX() + ", " + block.getY() + ", " + block.getZ());
            return;
        }

        if (action == Action.LEFT_CLICK_AIR) {
            event.setCancelled(true);
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);

            if (player.isSneaking()) {
                tryFinalize(player, wandType);
                return;
            }

            if (action == Action.RIGHT_CLICK_BLOCK) {
                Block block = event.getClickedBlock();
                if (block == null) return;
                plugin.getWandManager().setPos2(player.getUniqueId(), toPoint(block));
                player.sendMessage("§aPosition 2: §6" + block.getX() + ", " + block.getY() + ", " + block.getZ());
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        String wandType = WandItems.getWandType(plugin, player.getInventory().getItemInMainHand());
        if (wandType == null || !player.isSneaking()) return;

        event.setCancelled(true);
        tryFinalize(player, wandType);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        WandManager wandManager = plugin.getWandManager();
        WandManager.PendingCreation pending = wandManager.getPendingCreation(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String raw = event.getMessage().trim();

        plugin.getServer().getScheduler().runTask(plugin, () -> handleAreaNameInput(player, wandManager, pending, raw));
    }

    private void handleAreaNameInput(Player player, WandManager wandManager, WandManager.PendingCreation pending, String raw) {
        if (raw.equalsIgnoreCase("cancel")) {
            wandManager.clearPendingCreation(player.getUniqueId());
            player.sendMessage("§cArea creation cancelled");
            return;
        }

        if (raw.isEmpty()) {
            player.sendMessage("§cThe name cannot be empty. Type another one:");
            return;
        }

        String id = raw.replace(' ', '_');

        if (plugin.getAreaManager().getAreas().containsKey(id)) {
            player.sendMessage("§cAn area with ID '" + id + "' already exists. Type another name:");
            return;
        }

        WandManager.Point p1 = pending.pos1();
        WandManager.Point p2 = pending.pos2();
        boolean flat = "flat".equals(pending.type());

        boolean created = plugin.getAreaManager().createArea(id, p1.dimension(),
                p1.x(), p1.y(), p1.z(), p2.x(), p2.y(), p2.z(),
                flat ? "flat" : "cube", flat ? 8 : 0);

        if (created) {
            player.sendMessage("§aArea '" + id + "' created successfully!");
            ProtectedArea area = plugin.getAreaManager().getAreas().get(id);
            plugin.getAreaManager().broadcastNewArea(area);
            wandManager.clearPendingCreation(player.getUniqueId());
            wandManager.clearSelection(player.getUniqueId());
        } else {
            player.sendMessage("§cCould not create the area. Type another name:");
        }
    }

    private void tryFinalize(Player player, String wandType) {
        WandManager wandManager = plugin.getWandManager();
        UUID uuid = player.getUniqueId();

        if (!wandManager.hasBothPositions(uuid)) {
            player.sendMessage("§cYou need to select Pos1 and Pos2 before creating the area");
            return;
        }
        if (!wandManager.positionsInSameDimension(uuid)) {
            player.sendMessage("§cPos1 and Pos2 must be in the same dimension");
            return;
        }

        wandManager.beginPendingCreation(uuid, wandType, wandManager.getPos1(uuid), wandManager.getPos2(uuid));
        player.sendMessage("§eType in chat the name of the area you're going to create §7(or 'cancel' to cancel)");
    }

    private WandManager.Point toPoint(Block block) {
        String dimension = block.getWorld().getKey().toString();
        return new WandManager.Point(block.getX(), block.getY(), block.getZ(), dimension);
    }
}
