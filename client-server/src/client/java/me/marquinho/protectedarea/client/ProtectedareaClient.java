package me.marquinho.protectedarea.client;

import me.marquinho.protectedarea.client.debug.DebugKeyHandler;
import me.marquinho.protectedarea.client.debug.DebugOverlayRenderer;
import me.marquinho.protectedarea.client.debug.DebugState;
import me.marquinho.protectedarea.client.debug.DebugStateHolder;
import me.marquinho.protectedarea.client.managers.AreaTracker;
import me.marquinho.protectedarea.client.managers.SkyboxManager;
import me.marquinho.protectedarea.client.models.ProtectedArea;
import me.marquinho.protectedarea.client.network.ClientNetworkHandler;
import me.marquinho.protectedarea.network.ProtectedAreaPayload;
import me.marquinho.protectedarea.client.render.AreaOutlineRenderer;
import me.marquinho.protectedarea.client.util.ClientTextUtil;
import me.marquinho.protectedarea.client.util.WorldDimensionUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProtectedareaClient implements ClientModInitializer {

    private static AreaTracker areaTracker;
    private static int teleportCheckCooldown = 0;

    @Override
    public void onInitializeClient() {
        areaTracker = new AreaTracker();

        AreaOutlineRenderer.register();

        DebugOverlayRenderer.register();
        DebugKeyHandler.register();

        ClientPlayNetworking.registerGlobalReceiver(ProtectedAreaPayload.ID, (payload, context) -> {
            byte[] bytes = payload.data();

            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                String action = in.readUTF();

                if ("ADD_AREA".equals(action)) {
                    String id = in.readUTF();
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
                        ProtectedArea area = new ProtectedArea(id, dimension, x1, y1, z1, x2, y2, z2,
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
                    String scope   = in.available() > 0 ? in.readUTF() : DebugState.TYPE_CUBE;
                    boolean dimension = DebugState.TYPE_DIMENSION.equals(scope);

                    int logical = !dimension ? page : switch (page) {
                        case DebugState.DIM_PAGE_RULES      -> DebugState.PAGE_RULES;
                        case DebugState.DIM_PAGE_EXCEPTIONS -> DebugState.PAGE_EXCEPTIONS;
                        case DebugState.DIM_PAGE_ADVANCED   -> DebugState.PAGE_ADVANCED;
                        default                             -> DebugState.PAGE_OVERVIEW;
                    };

                    if (logical == DebugState.PAGE_OVERVIEW) {
                        if (dimension) {
                            int totalDimensionAreas = in.readInt();
                            String dimensionKey     = in.readUTF();
                            int dimensionPriority   = in.readInt();
                            String dimensionSkybox  = in.readUTF();
                            context.client().execute(() -> {
                                DebugState state = DebugStateHolder.get();
                                state.activate();
                                state.applyDimensionOverviewData(areaId, totalDimensionAreas, dimensionKey,
                                        dimensionPriority, dimensionSkybox, totalPages);
                            });
                        } else {
                            int totalAreas = in.readInt();
                            context.client().execute(() -> {
                                DebugState state = DebugStateHolder.get();
                                state.activate();
                                state.applyOverviewData(areaId, totalAreas, totalPages);
                            });
                        }
                    }
                    else if (logical == DebugState.PAGE_RULES) {
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
                    else if (logical == DebugState.PAGE_EXCEPTIONS) {
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
                    else if (logical == DebugState.PAGE_LIMIT) {
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
                    else if (logical == DebugState.PAGE_ADVANCED) {
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
                else if ("MESSAGE".equals(action)) {
                    String key = in.readUTF();
                    byte kind = in.readByte();
                    int argCount = in.readInt();
                    java.util.List<ClientTextUtil.Arg> args = new java.util.ArrayList<>();
                    for (int i = 0; i < argCount; i++) {
                        String name = in.readUTF();
                        String value = in.readUTF();
                        args.add(new ClientTextUtil.Arg(name, value, in.readBoolean()));
                    }
                    context.client().execute(() -> {
                        if (context.client().player == null) return;
                        context.client().player.sendMessage(ClientTextUtil.translate(key, args), kind == 2);
                    });
                }
                else if ("DIMENSION_SKYBOXES".equals(action)) {
                    int count = in.readInt();
                    java.util.Map<String, SkyboxManager.DimensionSkybox> skyboxes = new java.util.HashMap<>();
                    for (int i = 0; i < count; i++) {
                        String dimension = in.readUTF();
                        String skybox = in.readUTF();
                        int priority = in.readInt();
                        skyboxes.put(dimension, new SkyboxManager.DimensionSkybox(skybox, priority));
                    }

                    context.client().execute(() -> SkyboxManager.setDimensionSkyboxes(skyboxes));
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
            SkyboxManager.clearDimensionSkyboxes();
            teleportCheckCooldown = 0;
            DebugStateHolder.get().deactivate();

//            System.out.println("[ProtectedAreaClient] Conectado: áreas limpiadas.");
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            areaTracker.clearAreas();
            SkyboxManager.clearDimensionSkyboxes();
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

        String dimension = WorldDimensionUtil.getDimensionKey(client.world.getRegistryKey());
        String playerName = client.player.getName().getString();

        for (ProtectedArea area : areaTracker.getAreas().values()) {
            if (area.hasNoEntry() && area.isInside(x, y, z, dimension)) {
                if (!area.hasException(playerName, true)) {
                    ClientNetworkHandler.requestExpel(area.getId());
                }
                break;
            }
        }
    }

    public static AreaTracker getAreaTracker() {
        return areaTracker;
    }
}