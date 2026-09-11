package me.marquinho.protectedarea.client.managers;

import me.marquinho.protectedarea.client.models.ProtectedArea;

import java.util.*;

public class SkyboxManager {

    private static final float TRANSITION_SPEED = 0.1f;

    private static String activeSkybox = null;
    private static String renderingSkybox = null;
    private static float transitionAlpha = 0f;

    private static final Map<String, DimensionSkybox> dimensionSkyboxes = new HashMap<>();

    public static class DimensionSkybox {
        private final String name;
        private final int priority;

        public DimensionSkybox(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        public String getName() { return name; }
        public int getPriority() { return priority; }
    }

    public static void setDimensionSkyboxes(Map<String, DimensionSkybox> skyboxes) {
        dimensionSkyboxes.clear();
        dimensionSkyboxes.putAll(skyboxes);
    }

    public static void clearDimensionSkyboxes() {
        dimensionSkyboxes.clear();
    }

    public static DimensionSkybox getDimensionSkybox(String dimension) {
        return dimension == null ? null : dimensionSkyboxes.get(dimension);
    }

    public static void update(Set<String> currentAreaIds, Map<String, ProtectedArea> allAreas, String dimension) {
        List<ProtectedArea> matching = new ArrayList<>();
        for (String id : currentAreaIds) {
            ProtectedArea area = allAreas.get(id);
            if (area != null) matching.add(area);
        }

        matching.sort((a, b) -> {
            int byVolume = Long.compare(volumeOf(a), volumeOf(b));
            if (byVolume != 0) return byVolume;
            int byPriority = Integer.compare(b.getPriority(), a.getPriority());
            if (byPriority != 0) return byPriority;
            return a.getId().compareTo(b.getId());
        });

        String bestSkybox = null;
        boolean chainOpen = true;
        Integer lastPriority = null;

        for (ProtectedArea area : matching) {
            if (lastPriority != null && lastPriority > area.getPriority()) {
                chainOpen = false;
                break;
            }
            lastPriority = area.getPriority();
            if (area.hasSkybox()) {
                bestSkybox = area.getSkybox();
                break;
            }
        }

        DimensionSkybox dimensionEntry = getDimensionSkybox(dimension);

        if (bestSkybox == null
                && chainOpen
                && dimensionEntry != null
                && (lastPriority == null || lastPriority <= dimensionEntry.getPriority())
                && dimensionEntry.getName() != null
                && !dimensionEntry.getName().isEmpty()) {
            bestSkybox = dimensionEntry.getName();
        }

        activeSkybox = bestSkybox;

        if (activeSkybox != null && !activeSkybox.equals(renderingSkybox)) {
            renderingSkybox = activeSkybox;
        }
    }

    private static long volumeOf(ProtectedArea area) {
        long width = Math.abs((long) area.getMaxX() - area.getMinX());
        long height = Math.abs((long) area.getMaxY() - area.getMinY());
        long depth = Math.abs((long) area.getMaxZ() - area.getMinZ());
        return width * height * depth;
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
