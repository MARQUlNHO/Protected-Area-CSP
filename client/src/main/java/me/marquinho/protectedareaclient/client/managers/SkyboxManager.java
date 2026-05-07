package me.marquinho.protectedareaclient.client.managers;

import me.marquinho.protectedareaclient.client.models.ProtectedArea;

import java.util.Map;
import java.util.Set;

public class SkyboxManager {

    private static final float TRANSITION_SPEED = 0.1f;

    private static String activeSkybox = null;
    private static String renderingSkybox = null;
    private static float transitionAlpha = 0f;

    public static void update(Set<String> currentAreaIds, Map<String, ProtectedArea> allAreas) {
        String bestSkybox = null;
        int bestPriority = Integer.MIN_VALUE;

        for (String id : currentAreaIds) {
            ProtectedArea area = allAreas.get(id);
            if (area != null && area.hasSkybox() && area.getPriority() > bestPriority) {
                bestPriority = area.getPriority();
                bestSkybox = area.getSkybox();
            }
        }

        activeSkybox = bestSkybox;

        if (activeSkybox != null && !activeSkybox.equals(renderingSkybox)) {
            renderingSkybox = activeSkybox;
        }
    }

    public static void tick() {
        if (activeSkybox != null) {
            transitionAlpha = Math.min(1f, transitionAlpha + TRANSITION_SPEED);
        } else {
            transitionAlpha = Math.max(0f, transitionAlpha - TRANSITION_SPEED);
            if (transitionAlpha == 0f) {
                renderingSkybox = null;
            }
        }
    }

    public static void clear() {
        activeSkybox = null;
        renderingSkybox = null;
        transitionAlpha = 0f;
    }

    public static boolean isRendering() {
        return renderingSkybox != null && transitionAlpha > 0f;
    }

    public static String getRenderingSkybox() { return renderingSkybox; }
    public static String getActiveSkybox()    { return activeSkybox; }
    public static float getTransitionAlpha()  { return transitionAlpha; }


}
