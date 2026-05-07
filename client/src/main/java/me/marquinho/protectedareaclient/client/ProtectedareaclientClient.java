package me.marquinho.protectedareaclient.client;

import me.marquinho.protectedareaclient.client.debug.DebugKeyHandler;
import me.marquinho.protectedareaclient.client.debug.DebugOverlayRenderer;
import me.marquinho.protectedareaclient.client.debug.DebugState;
import me.marquinho.protectedareaclient.client.debug.DebugStateHolder;
import me.marquinho.protectedareaclient.client.managers.AreaTracker;
import me.marquinho.protectedareaclient.client.managers.SkyboxManager;
import me.marquinho.protectedareaclient.client.models.ProtectedArea;
import me.marquinho.protectedareaclient.client.network.ClientNetworkHandler;
import me.marquinho.protectedareaclient.client.network.payload.RawPayload;
import me.marquinho.protectedareaclient.client.network.payload.ClientNotificationPayload;
import me.marquinho.protectedareaclient.client.render.AreaOutlineRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProtectedareaclientClient implements ClientModInitializer {

    private static AreaTracker areaTracker;
    private static int teleportCheckCooldown = 0;

    @Override
    public void onInitializeClient() {
        areaTracker = new AreaTracker();

        AreaOutlineRenderer.register();

        DebugOverlayRenderer.register();
        DebugKeyHandler.register();

        PayloadTypeRegistry.playS2C().register(RawPayload.ID, RawPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientNotificationPayload.ID, ClientNotificationPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(RawPayload.ID, (payload, context) -> {
            byte[] bytes = payload.data();

            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                String action = in.readUTF();

                if ("ADD_AREA".equals(action)) {
                    String id = in.readUTF();
                    String worldName = in.readUTF();
                    String dimension = in.readUTF();
                    int x1 = in.readInt();
                    int y1 = in.readInt();
                    int z1 = in.readInt();
                    int x2 = in.readInt();
                    int y2 = in.readInt();
                    int z2 = in.readInt();
                    String color = in.readUTF();

                    boolean hasNoEntry = in.readBoolean();
                    boolean hasNoExit = in.readBoolean();

                    int noEntryExceptionsCount = in.readInt();
                    Set<String> noEntryExceptions = new java.util.HashSet<>();
                    for (int i = 0; i < noEntryExceptionsCount; i++) {
                        noEntryExceptions.add(in.readUTF().toLowerCase());
                    }

                    int noExitExceptionsCount = in.readInt();
                    Set<String> noExitExceptions = new java.util.HashSet<>();
                    for (int i = 0; i < noExitExceptionsCount; i++) {
                        noExitExceptions.add(in.readUTF().toLowerCase());
                    }

                    boolean hasLimit = in.readBoolean();
                    boolean isLimitActive;

                    if (hasLimit) {
                        int limit = in.readInt();
                        isLimitActive = in.readBoolean();
                    } else {
                        isLimitActive = false;
                    }

                    int limitExceptionsCount = in.readInt();
                    Set<String> limitExceptions = new java.util.HashSet<>();
                    for (int i = 0; i < limitExceptionsCount; i++) {
                        limitExceptions.add(in.readUTF().toLowerCase());
                    }

                    int priority = 0;
                    String skybox = "";
                    String type = "cube";
                    int flatPosition = 0;
                    boolean passNegative = true;
                    boolean passPositive = true;
                    try {
                        priority = in.readInt();
                        skybox = in.readUTF();
                        type = in.readUTF();
                        flatPosition = in.readInt();
                        passNegative = in.readBoolean();
                        passPositive = in.readBoolean();
                    } catch (EOFException ignored) {}

                    final int finalPriority = priority;
                    final String finalSkybox = skybox;
                    final String finalType = type;
                    final int finalFlatPosition = flatPosition;
                    final boolean finalPassNegative = passNegative;
                    final boolean finalPassPositive = passPositive;

                    context.client().execute(() -> {
                        ProtectedArea area = new ProtectedArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2,
                                color, hasNoEntry, hasNoExit, noEntryExceptions, noExitExceptions,
                                hasLimit, isLimitActive, limitExceptions, finalPriority, finalSkybox,
                                finalType, finalFlatPosition, finalPassNegative, finalPassPositive);

                        boolean isUpdate = areaTracker.getAreas().containsKey(id);
                        areaTracker.addArea(area);

                        if (isUpdate) {
//                            System.out.println("Área ACTUALIZADA: " + id + " | LIMIT: " + (hasLimit ? "ACTIVE" : "NONE"));
                        } else {
//                            System.out.println("Área agregada: " + id + " | LIMIT: " + (hasLimit ? "ACTIVE" : "NONE"));
                        }
                    });
                }
                else if ("REMOVE_AREA".equals(action)) {
                    String id = in.readUTF();

                    context.client().execute(() -> {
                        areaTracker.removeArea(id);
//                        System.out.println("Área removida: " + id);
                    });
                }
                else if ("CLEAR_AREAS".equals(action)) {
                    context.client().execute(() -> {
                        areaTracker.clearAreas();
//                        System.out.println("Todas las áreas han sido limpiadas");
                    });
                }
                else if ("VIEW_TOGGLE".equals(action)) {
                    boolean enable = in.readBoolean();
                    context.client().execute(() -> {
                        areaTracker.setViewEnabled(enable);
//                        System.out.println("Visualización de áreas: " + (enable ? "activada" : "desactivada"));
                    });
                }

                else if ("UPDATE_AREA_LIMIT_STATE".equals(action)) {
                    String areaId = in.readUTF();
                    boolean isLimitActive = in.readBoolean();

                    context.client().execute(() -> {
                        ProtectedArea area = areaTracker.getAreas().get(areaId);
                        if (area != null) {
                            area.updateLimitState(isLimitActive);
//                            System.out.println("[ProtectedAreaClient] Límite actualizado: " + areaId + " | Bloqueado: " + isLimitActive);
                        }
                    });
                }

                else if ("MOD_CHECK".equals(action)) {
                    context.client().execute(() -> {
//                        System.out.println("[ProtectedAreaClient] Verificación de mod recibida, respondiendo...");
                        ClientNetworkHandler.sendModResponse();
                    });
                }

                else if ("DEBUG_DATA".equals(action)) {
                    int page       = in.readInt();
                    int totalPages = in.readInt();
                    String areaId  = in.readUTF();

                    if (page == DebugState.PAGE_OVERVIEW) {
                        int totalAreas = in.readInt();
                        context.client().execute(() -> {
                            DebugState state = DebugStateHolder.get();
                            state.activate();
                            state.applyOverviewData(areaId, totalAreas, totalPages);
                        });
                    }
                    else if (page == DebugState.PAGE_RULES) {
                        int ruleCount = in.readInt();
                        List<DebugState.RuleEntry> entries = new ArrayList<>();
                        for (int i = 0; i < ruleCount; i++) {
                            String key = in.readUTF();
                            boolean hasException = in.readBoolean();
                            entries.add(new DebugState.RuleEntry(key, hasException));
                        }
                        context.client().execute(() -> {
                            DebugState state = DebugStateHolder.get();
                            state.activate();
                            state.applyRulesData(areaId, entries, totalPages);
                        });
                    }
                    else if (page == DebugState.PAGE_EXCEPTIONS) {
                        int count = in.readInt();
                        List<DebugState.ExceptionEntry> entries = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            String ruleKey    = in.readUTF();
                            String playerName = in.readUTF();
                            entries.add(new DebugState.ExceptionEntry(ruleKey, playerName));
                        }
                        context.client().execute(() -> {
                            DebugState state = DebugStateHolder.get();
                            state.activate();
                            state.applyExceptionsData(areaId, entries, totalPages);
                        });
                    }
                    else if (page == DebugState.PAGE_LIMIT) {
                        boolean hasLimit = in.readBoolean();
                        int limitMax = 0, limitCurrent = 0;
                        boolean limitBlocked = false, limitException = false;
                        if (hasLimit) {
                            limitMax       = in.readInt();
                            limitCurrent   = in.readInt();
                            limitBlocked   = in.readBoolean();
                            limitException = in.readBoolean();
                        }
                        final int fMax = limitMax, fCurrent = limitCurrent;
                        final boolean fBlocked = limitBlocked, fException = limitException;
                        context.client().execute(() -> {
                            DebugState state = DebugStateHolder.get();
                            state.activate();
                            state.applyLimitData(areaId, hasLimit, fMax, fCurrent, fBlocked, fException, totalPages);
                        });
                    }
                    else if (page == DebugState.PAGE_ADVANCED) {
                        int count = in.readInt();
                        List<DebugState.AdvancedEntry> entries = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            String ruleType   = in.readUTF();
                            String targetType = in.readUTF();
                            String targetId   = in.readUTF();
                            entries.add(new DebugState.AdvancedEntry(ruleType, targetType, targetId));
                        }
                        context.client().execute(() -> {
                            DebugState state = DebugStateHolder.get();
                            state.activate();
                            state.applyAdvancedData(areaId, entries, totalPages);
                        });
                    }
                }
                else if ("DEBUG_CLOSE".equals(action)) {
                    context.client().execute(() -> DebugStateHolder.get().deactivate());
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            areaTracker.clearAreas();
            teleportCheckCooldown = 0;
            DebugStateHolder.get().deactivate();

//            System.out.println("[ProtectedAreaClient] Conectado: áreas limpiadas.");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            areaTracker.clearAreas();
            DebugStateHolder.get().deactivate();
//            System.out.println("[ProtectedAreaClient] Desconectado: áreas limpiadas.");
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                areaTracker.checkPlayerPosition(client.player, client.world);
                SkyboxManager.tick();

                if (teleportCheckCooldown <= 0) {
                    checkTeleportIntoNoEntry(client);
                    teleportCheckCooldown = 20;
                } else {
                    teleportCheckCooldown--;
                }
            }
        });

//        System.out.println("ProtectedAreaClient mod inicializado correctamente");
    }

    private static void checkTeleportIntoNoEntry(net.minecraft.client.MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();

        String worldName = getWorldName(client.world.getRegistryKey());
        String dimension = getDimensionKey(client.world.getRegistryKey());
        String playerName = client.player.getName().getString();

        for (ProtectedArea area : areaTracker.getAreas().values()) {
            if (area.hasNoEntry() && area.isInside(x, y, z, worldName, dimension)) {
                if (!area.hasException(playerName, true)) {
                    ClientNetworkHandler.requestExpel(area.getId());
                }
                break;
            }
        }
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

    public static AreaTracker getAreaTracker() {
        return areaTracker;
    }
}