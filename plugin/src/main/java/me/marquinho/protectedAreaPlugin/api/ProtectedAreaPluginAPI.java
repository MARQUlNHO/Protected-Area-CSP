package me.marquinho.protectedAreaPlugin.api;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.api.events.AreaCreatedEvent;
import me.marquinho.protectedAreaPlugin.api.events.AreaRemovedEvent;
import me.marquinho.protectedAreaPlugin.models.AdvancedAreaRules;
import me.marquinho.protectedAreaPlugin.models.AdvancedRuleType;
import me.marquinho.protectedAreaPlugin.models.AreaRule;
import me.marquinho.protectedAreaPlugin.models.ProtectedArea;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

// Entry point for interacting with ProtectedAreaPlugin from other plugins.
// Usage: ProtectedAreaPluginAPI api = ProtectedAreaPluginAPI.getInstance();
public final class ProtectedAreaPluginAPI {

    private static final ProtectedAreaPluginAPI INSTANCE = new ProtectedAreaPluginAPI();

    private ProtectedAreaPluginAPI() {}

    public static ProtectedAreaPluginAPI getInstance() {
        return INSTANCE;
    }

    private ProtectedAreaPlugin plugin() {
        return ProtectedAreaPlugin.getInstance();
    }

    // ---------
    // Query API
    // ---------

    // All registered areas.
    public Collection<ProtectedArea> getAreas() {
        return Collections.unmodifiableCollection(plugin().getAreaManager().getAreas().values());
    }

    // Area by ID, or empty if not found.
    public Optional<ProtectedArea> getArea(String id) {
        return Optional.ofNullable(plugin().getAreaManager().getAreas().get(id));
    }

    // The highest-priority area at the given location, or empty if none.
    public Optional<ProtectedArea> getAreaAt(org.bukkit.Location location) {
        return Optional.ofNullable(plugin().getAreaManager().getAreaAt(location));
    }

    // All areas whose bounds contain the given location.
    public Collection<ProtectedArea> getAreasAt(org.bukkit.Location location) {
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "";
        String dimension = worldToDimension(worldName);
        double x = location.getX(), y = location.getY(), z = location.getZ();
        return plugin().getAreaManager().getAreas().values().stream()
                .filter(area -> area.isInside(x, y, z, worldName, dimension))
                .toList();
    }

    // Whether the player can enter the area (respects limits, limitBlocked, and limit exceptions).
    public boolean canPlayerEnter(Player player, String areaId) {
        ProtectedArea area = plugin().getAreaManager().getAreas().get(areaId);
        if (area == null) return true;
        return plugin().getAreaManager().canPlayerEnterArea(player, area);
    }

    // Number of players currently inside the area.
    public int getPlayersInArea(String areaId) {
        return plugin().getAreaManager().getPlayersInArea(areaId);
    }

    // Whether the area has the given basic rule.
    public boolean hasRule(String areaId, AreaRule rule) {
        ProtectedArea area = plugin().getAreaManager().getAreas().get(areaId);
        if (area == null) return false;
        return area.hasRule(rule);
    }

    // Whether the player has an exception for the given rule in the area.
    public boolean hasException(String areaId, String playerName, AreaRule rule) {
        ProtectedArea area = plugin().getAreaManager().getAreas().get(areaId);
        if (area == null) return false;
        return area.hasException(playerName, rule.getKey());
    }

    // Advanced rules (block/entity rules) for the given area.
    public AdvancedAreaRules getAdvancedRules(String areaId) {
        return plugin().getAdvancedRulesManager().getRules(areaId);
    }

    // ----------
    // Action API
    // ----------

    // Create a cube area. Returns false if the ID already exists.
    public boolean createArea(String id, String worldName, String dimension,
                              int x1, int y1, int z1, int x2, int y2, int z2) {
        boolean created = plugin().getAreaManager().createArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2);
        if (created) {
            ProtectedArea area = plugin().getAreaManager().getAreas().get(id);
            plugin().getAreaManager().broadcastNewArea(area);
            plugin().getServer().getPluginManager().callEvent(new AreaCreatedEvent(area));
        }
        return created;
    }

    // Create a flat area. Returns false if the ID already exists.
    public boolean createFlatArea(String id, String worldName, String dimension,
                                  int x1, int y1, int z1, int x2, int y2, int z2, int flatPosition) {
        boolean created = plugin().getAreaManager().createArea(id, worldName, dimension, x1, y1, z1, x2, y2, z2, "flat", flatPosition);
        if (created) {
            ProtectedArea area = plugin().getAreaManager().getAreas().get(id);
            plugin().getAreaManager().broadcastNewArea(area);
            plugin().getServer().getPluginManager().callEvent(new AreaCreatedEvent(area));
        }
        return created;
    }

    // Remove an area. Returns false if the ID does not exist.
    public boolean removeArea(String id) {
        boolean removed = plugin().getAreaManager().removeArea(id);
        if (removed) {
            plugin().getAreaManager().broadcastRemoveArea(id);
            plugin().getServer().getPluginManager().callEvent(new AreaRemovedEvent(id));
        }
        return removed;
    }

    // ------------------------
    // Action API — Basic Rules
    // ------------------------

    // Add a basic rule to an area. Returns false if the area does not exist.
    public boolean addRule(String areaId, AreaRule rule) {
        boolean ok = plugin().getAreaManager().addRuleToArea(areaId, rule);
        if (ok) broadcastUpdate(areaId);
        return ok;
    }

    // Remove a basic rule from an area. Returns false if the area does not exist.
    public boolean removeRule(String areaId, AreaRule rule) {
        boolean ok = plugin().getAreaManager().removeRuleFromArea(areaId, rule);
        if (ok) broadcastUpdate(areaId);
        return ok;
    }

    // -----------------------
    // Action API — Exceptions
    // -----------------------

    // Add an exception for a player on a specific rule.
    public boolean addException(String areaId, String playerName, AreaRule rule) {
        ProtectedArea area = plugin().getAreaManager().getAreas().get(areaId);
        if (area == null) return false;
        boolean ok = area.addException(rule.getKey(), playerName.toLowerCase());
        if (ok) { plugin().getAreaManager().saveAreaManually(area); broadcastUpdate(areaId); }
        return ok;
    }

    // Remove an exception for a player on a specific rule.
    public boolean removeException(String areaId, String playerName, AreaRule rule) {
        ProtectedArea area = plugin().getAreaManager().getAreas().get(areaId);
        if (area == null) return false;
        boolean ok = area.removeException(rule.getKey(), playerName.toLowerCase());
        if (ok) { plugin().getAreaManager().saveAreaManually(area); broadcastUpdate(areaId); }
        return ok;
    }

    // -------------------------
    // Action API — Player Limit
    // -------------------------

    // Set the player limit for an area.
    public boolean setLimit(String areaId, int limit) {
        return plugin().getAreaManager().setAreaLimit(areaId, limit);
    }

    // Remove the player limit from an area.
    public boolean removeLimit(String areaId) {
        return plugin().getAreaManager().removeAreaLimit(areaId);
    }

    // Block or unblock entry/exit for an area (regardless of current player count).
    public boolean setLimitBlocked(String areaId, boolean blocked) {
        return plugin().getAreaManager().setAreaLimitBlocked(areaId, blocked);
    }

    // --------------------
    // Action API — Visual
    // --------------------

    // Set the skybox for an area.
    public boolean setSkybox(String areaId, String skyboxName) {
        boolean ok = plugin().getAreaManager().setSkyboxForArea(areaId, skyboxName);
        if (ok) broadcastUpdate(areaId);
        return ok;
    }

    // Remove the skybox from an area.
    public boolean removeSkybox(String areaId) {
        return setSkybox(areaId, "");
    }

    // Set the render priority of an area.
    public boolean setPriority(String areaId, int priority) {
        boolean ok = plugin().getAreaManager().setAreaPriority(areaId, priority);
        if (ok) broadcastUpdate(areaId);
        return ok;
    }

    // --------------------------
    // Action API — Player Movement
    // --------------------------

    // Teleport the player outside the area (used when NO_ENTRY blocked them).
    public void expelPlayer(Player player, String areaId) {
        ProtectedArea area = plugin().getAreaManager().getAreas().get(areaId);
        if (area != null) plugin().getAreaManager().expelPlayerFromArea(player, area);
    }

    // Teleport the player back inside the area (used when NO_EXIT blocked them).
    public void returnPlayer(Player player, String areaId) {
        ProtectedArea area = plugin().getAreaManager().getAreas().get(areaId);
        if (area != null) plugin().getAreaManager().returnPlayerToArea(player, area);
    }

    // ---------------------------
    // Action API — Advanced Rules
    // ---------------------------

    // Add a block advanced rule to an area.
    public boolean addBlockRule(String areaId, AdvancedRuleType ruleType, String blockId) {
        return plugin().getAdvancedRulesManager().addBlockRule(areaId, ruleType, blockId);
    }

    // Remove a block advanced rule from an area.
    public boolean removeBlockRule(String areaId, AdvancedRuleType ruleType, String blockId) {
        return plugin().getAdvancedRulesManager().removeBlockRule(areaId, ruleType, blockId);
    }

    // Add an entity advanced rule to an area.
    public boolean addEntityRule(String areaId, AdvancedRuleType ruleType, String entityId) {
        return plugin().getAdvancedRulesManager().addEntityRule(areaId, ruleType, entityId);
    }

    // Remove an entity advanced rule from an area.
    public boolean removeEntityRule(String areaId, AdvancedRuleType ruleType, String entityId) {
        return plugin().getAdvancedRulesManager().removeEntityRule(areaId, ruleType, entityId);
    }

    // --------
    // Internal
    // --------

    private void broadcastUpdate(String areaId) {
        ProtectedArea area = plugin().getAreaManager().getAreas().get(areaId);
        if (area != null) plugin().getAreaManager().broadcastUpdateArea(area);
    }

    private String worldToDimension(String worldName) {
        return switch (worldName) {
            case "world_nether" -> "minecraft:the_nether";
            case "world_the_end" -> "minecraft:the_end";
            default -> "minecraft:overworld";
        };
    }
}
