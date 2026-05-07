package me.marquinho.protectedareaserver.commands.subcommands.cube;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.marquinho.protectedareaserver.ProtectedAreaInit;
import me.marquinho.protectedareaserver.models.ProtectedArea;
import me.marquinho.protectedareaserver.util.SchedulerUtil;
import me.marquinho.protectedareaserver.util.TextUtil;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class TeleportCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(ProtectedAreaInit plugin) {
        return CommandManager.literal("tp")
                .then(CommandManager.argument("area_id", StringArgumentType.word())
                        .suggests(suggestAreaIds(plugin))
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(context -> executeTeleport(context, plugin, 0, 1))
                                .then(CommandManager.argument("delay", IntegerArgumentType.integer(0))
                                        .executes(context -> executeTeleport(context, plugin, IntegerArgumentType.getInteger(context, "delay"), 1))
                                        .then(CommandManager.argument("groupSize", IntegerArgumentType.integer(1))
                                                .executes(context -> executeTeleport(context, plugin,
                                                        IntegerArgumentType.getInteger(context, "delay"),
                                                        IntegerArgumentType.getInteger(context, "groupSize")))
                                        )
                                )
                        )
                );
    }

    private static int executeTeleport(CommandContext<ServerCommandSource> context, ProtectedAreaInit plugin, int delayTicks, int groupSize) {
        try {
            ServerCommandSource source = context.getSource();
            String areaId = StringArgumentType.getString(context, "area_id");
            Collection<ServerPlayerEntity> targetsCollection = EntityArgumentType.getPlayers(context, "targets");
            if (targetsCollection.isEmpty()) { source.sendError(TextUtil.parse("<red>No se encontraron jugadores")); return 0; }
            ProtectedArea area = plugin.getAreaManager().getAreas().get(areaId);
            if (area == null) { source.sendError(TextUtil.parse("<red>No existe un área con el ID: " + areaId)); return 0; }
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(area.getDimension()));
            ServerWorld world = plugin.getServer().getWorld(worldKey);
            if (world == null) { source.sendError(TextUtil.parse("<red>El mundo del área no existe: " + area.getDimension())); return 0; }
            List<ServerPlayerEntity> players = new ArrayList<>(targetsCollection);
            source.sendFeedback(() -> TextUtil.parse("<green>Teletransportando <gold>" + players.size() + " <green>jugador(es) al área <gold>" + areaId), false);
            source.sendFeedback(() -> TextUtil.parse("<yellow>Delay: <gold>" + delayTicks + " ticks <gray>| <yellow>Grupo: <gold>" + groupSize + " jugador(es)"), false);
            teleportPlayersInGroups(plugin, players, area, world, delayTicks, groupSize, source);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static void teleportPlayersInGroups(ProtectedAreaInit plugin, List<ServerPlayerEntity> players,
                                                ProtectedArea area, ServerWorld world,
                                                int delayTicks, int groupSize, ServerCommandSource source) {
        int[] index = {0};
        SchedulerUtil.Cancellable[] handle = {null};
        handle[0] = SchedulerUtil.runTimer(() -> {
            if (index[0] >= players.size()) {
                source.sendFeedback(() -> TextUtil.parse("<green>¡Teletransporte completado!"), false);
                handle[0].cancel();
                return;
            }
            int endIndex = Math.min(index[0] + groupSize, players.size());
            List<ServerPlayerEntity> group = players.subList(index[0], endIndex);
            Set<BlockPos> usedPositions = new HashSet<>();
            for (ServerPlayerEntity player : group) {
                BlockPos safePos = findSafeRandomPosition(area, world, usedPositions, 50);
                if (safePos != null) {
                    final BlockPos fp = safePos;
                    player.teleport(world, fp.getX() + 0.5, fp.getY(), fp.getZ() + 0.5, Set.of(), player.getYaw(), player.getPitch());
                    usedPositions.add(safePos);
                } else {
                    source.sendFeedback(() -> TextUtil.parse("<red>No se pudo encontrar una ubicación segura para <gold>" + player.getGameProfile().getName()), false);
                }
            }
            index[0] = endIndex;
        }, delayTicks > 0 ? delayTicks : 1, delayTicks > 0 ? delayTicks : 1);
    }

    private static BlockPos findSafeRandomPosition(ProtectedArea area, ServerWorld world, Set<BlockPos> usedPositions, int maxAttempts) {
        Random random = new Random();
        int minX = Math.min(area.getX1(), area.getX2()), maxX = Math.max(area.getX1(), area.getX2());
        int minY = Math.min(area.getY1(), area.getY2()), maxY = Math.max(area.getY1(), area.getY2());
        int minZ = Math.min(area.getZ1(), area.getZ2()), maxZ = Math.max(area.getZ1(), area.getZ2());
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = minX + (maxX > minX ? random.nextInt(maxX - minX + 1) : 0);
            int z = minZ + (maxZ > minZ ? random.nextInt(maxZ - minZ + 1) : 0);
            for (int y = maxY; y >= minY; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (isPositionUsed(pos, usedPositions)) continue;
                if (isSafePosition(world, pos)) return pos;
            }
        }
        return null;
    }

    private static boolean isPositionUsed(BlockPos pos, Set<BlockPos> usedPositions) {
        for (BlockPos used : usedPositions)
            if (used.getX() == pos.getX() && used.getY() == pos.getY() && used.getZ() == pos.getZ()) return true;
        return false;
    }

    private static boolean isSafePosition(ServerWorld world, BlockPos pos) {
        BlockPos belowPos = pos.down(), headPos = pos.up();
        var belowState = world.getBlockState(belowPos);
        var feetState  = world.getBlockState(pos);
        var headState  = world.getBlockState(headPos);
        if (belowState.getCollisionShape(world, belowPos).isEmpty()) return false;
        if (belowState.getBlock() == Blocks.LAVA || belowState.getBlock() == Blocks.MAGMA_BLOCK) return false;
        if (!feetState.getCollisionShape(world, pos).isEmpty()) return false;
        if (!headState.getCollisionShape(world, headPos).isEmpty()) return false;
        if (feetState.getBlock() == Blocks.LAVA || feetState.getBlock() == Blocks.FIRE || feetState.getBlock() == Blocks.WATER) return false;
        if (headState.getBlock() == Blocks.LAVA || headState.getBlock() == Blocks.FIRE) return false;
        return true;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAreaIds(ProtectedAreaInit plugin) {
        return (context, builder) -> {
            plugin.getAreaManager().getAreas().entrySet().stream()
                    .filter(e -> !e.getValue().isFlat())
                    .map(java.util.Map.Entry::getKey)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
