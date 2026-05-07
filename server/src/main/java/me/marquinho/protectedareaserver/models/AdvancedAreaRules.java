package me.marquinho.protectedareaserver.models;

import java.util.*;

public class AdvancedAreaRules {
    private final String areaId;
    private final Map<AdvancedRuleType, Set<String>> blockRules;
    private final Map<AdvancedRuleType, Set<String>> entityRules;

    public AdvancedAreaRules(String areaId) {
        this.areaId = areaId;
        this.blockRules = new HashMap<>();
        this.entityRules = new HashMap<>();

        for (AdvancedRuleType type : AdvancedRuleType.values()) {
            blockRules.put(type, new HashSet<>());
            entityRules.put(type, new HashSet<>());
        }
    }

    public String getAreaId() {
        return areaId;
    }

    public boolean addBlock(AdvancedRuleType ruleType, String blockId) {
        return blockRules.get(ruleType).add(blockId.toLowerCase());
    }

    public boolean addEntity(AdvancedRuleType ruleType, String entityId) {
        return entityRules.get(ruleType).add(entityId.toLowerCase());
    }

    public boolean removeBlock(AdvancedRuleType ruleType, String blockId) {
        return blockRules.get(ruleType).remove(blockId.toLowerCase());
    }

    public boolean removeEntity(AdvancedRuleType ruleType, String entityId) {
        return entityRules.get(ruleType).remove(entityId.toLowerCase());
    }

    public void clearBlocks(AdvancedRuleType ruleType) {
        blockRules.get(ruleType).clear();
    }

    public void clearEntities(AdvancedRuleType ruleType) {
        entityRules.get(ruleType).clear();
    }

    public void clearRule(AdvancedRuleType ruleType) {
        clearBlocks(ruleType);
        clearEntities(ruleType);
    }

    public boolean hasBlock(AdvancedRuleType ruleType, String blockId) {
        return blockRules.get(ruleType).contains(blockId.toLowerCase());
    }

    public boolean hasEntity(AdvancedRuleType ruleType, String entityId) {
        return entityRules.get(ruleType).contains(entityId.toLowerCase());
    }

    public boolean hasItem(AdvancedRuleType ruleType, String itemId) {
        return blockRules.get(ruleType).contains(itemId.toLowerCase());
    }

    public Set<String> getBlocks(AdvancedRuleType ruleType) {
        return new HashSet<>(blockRules.get(ruleType));
    }

    public Set<String> getEntities(AdvancedRuleType ruleType) {
        return new HashSet<>(entityRules.get(ruleType));
    }

    public boolean hasAnyRules(AdvancedRuleType ruleType) {
        return !blockRules.get(ruleType).isEmpty() || !entityRules.get(ruleType).isEmpty();
    }

    public Map<AdvancedRuleType, Set<String>> getAllBlockRules() {
        Map<AdvancedRuleType, Set<String>> copy = new HashMap<>();
        for (Map.Entry<AdvancedRuleType, Set<String>> entry : blockRules.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }

    public Map<AdvancedRuleType, Set<String>> getAllEntityRules() {
        Map<AdvancedRuleType, Set<String>> copy = new HashMap<>();
        for (Map.Entry<AdvancedRuleType, Set<String>> entry : entityRules.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }
}