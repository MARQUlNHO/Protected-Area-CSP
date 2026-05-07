package me.marquinho.protectedAreaPlugin.commands.subcommands.cube;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TeleportCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(ProtectedAreaPlugin plugin) {
        return Commands.literal("tp")
                .then(Commands.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(Commands.argument("targets", ArgumentTypes.players())
                                .executes(context -> executeTeleport(context, plugin, 0, 1))
                                .then(Commands.argument("delay", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            int delay = IntegerArgumentType.getInteger(context, "delay");
                                            return executeTeleport(context, plugin, delay, 1);
                                        })
                                        .then(Commands.argument("groupSize", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    int delay = IntegerArgumentType.getInteger(context, "delay");
                                                    int groupSize = IntegerArgumentType.getInteger(context, "groupSize");
                                                    return executeTeleport(context, plugin, delay, groupSize);
                                                })
                                        )
                                )
                        )
                );
    }

    private static int executeTeleport(CommandContext<CommandSourceStack> context, ProtectedAreaPlugin plugin, int delayTicks, int groupSize) {
        try {
            CommandSender sender = context.getSource().getSender();
            String areaId = StringArgumentType.getString(context, "area_id");
            PlayerSelectorArgumentResolver resolver = context.getArgument("targets", PlayerSelectorArgumentResolver.class);
            List<Player> targets = resolver.resolve(context.getSource());

            if (targets.isEmpty()) { sender.sendMessage("§cNo se encontraron jugadores con ese selector"); return 0; }

            ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
            if (area == null) { sender.sendMessage("§cNo existe un área con el ID: " + areaId); return 0; }

            World world = plugin.getServer().getWorld(area.getWorldName());
            if (world == null) { sender.sendMessage("§cEl mundo del área no existe: " + area.getWorldName()); return 0; }

            sender.sendMessage("§aTeletransportando §6" + targets.size() + " §ajugador(es) al área §6" + areaId);
            sender.sendMessage("§eDelay: §6" + delayTicks + " ticks §7| §eGrupo: §6" + groupSize + " jugador(es)");

            teleportPlayersInGroups(plugin, new ArrayList<>(targets), area, world, delayTicks, groupSize, sender);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static void teleportPlayersInGroups(ProtectedAreaPlugin plugin, List<Player> players, ProtectedArea area, World world, int delayTicks, int groupSize, CommandSender sender) {
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= players.size()) {
                    sender.sendMessage("§a¡Teletransporte completado!");
                    cancel();
                    return;
                }

                int endIndex = Math.min(index + groupSize, players.size());
                List<Player> group = players.subList(index, endIndex);
                Set<Location> usedLocations = new HashSet<>();

                for (Player player : group) {
                    Location safeLoc = findSafeRandomLocation(area, world, usedLocations, 50);
                    if (safeLoc != null) {
                        player.teleport(safeLoc);
                        usedLocations.add(safeLoc);
                    } else {
                        sender.sendMessage("§cNo se pudo encontrar una ubicación segura para §6" + player.getName());
                    }
                }

                index = endIndex;
            }
        }.runTaskTimer(plugin, 0L, delayTicks);
    }

    private static Location findSafeRandomLocation(ProtectedArea area, World world, Set<Location> usedLocations, int maxAttempts) {
        Random random = new Random();
        int minX = Math.min(area.getX1(), area.getX2()), maxX = Math.max(area.getX1(), area.getX2());
        int minY = Math.min(area.getY1(), area.getY2()), maxY = Math.max(area.getY1(), area.getY2());
        int minZ = Math.min(area.getZ1(), area.getZ2()), maxZ = Math.max(area.getZ1(), area.getZ2());

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = minX + (maxX > minX ? random.nextInt(maxX - minX + 1) : 0);
            int z = minZ + (maxZ > minZ ? random.nextInt(maxZ - minZ + 1) : 0);
            for (int y = maxY; y >= minY; y--) {
                Location loc = new Location(world, x + 0.5, y, z + 0.5);
                if (isLocationUsed(loc, usedLocations)) continue;
                if (isSafeLocation(loc)) return loc;
            }
        }
        return null;
    }

    private static boolean isLocationUsed(Location loc, Set<Location> usedLocations) {
        for (Location used : usedLocations)
            if (used.getBlockX() == loc.getBlockX() && used.getBlockY() == loc.getBlockY() && used.getBlockZ() == loc.getBlockZ())
                return true;
        return false;
    }

    private static boolean isSafeLocation(Location loc) {
        Material below = loc.clone().subtract(0, 1, 0).getBlock().getType();
        Material feet  = loc.getBlock().getType();
        Material head  = loc.clone().add(0, 1, 0).getBlock().getType();
        if (!below.isSolid()) return false;
        if (below == Material.LAVA || below == Material.MAGMA_BLOCK) return false;
        if (!feet.isAir() && feet.isSolid()) return false;
        if (!head.isAir() && head.isSolid()) return false;
        if (feet == Material.LAVA || feet == Material.FIRE || feet == Material.WATER) return false;
        if (head == Material.LAVA || head == Material.FIRE) return false;
        return true;
    }

    private static SuggestionProvider<CommandSourceStack> suggestAreaIds(ProtectedAreaPlugin plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(java.util.Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
