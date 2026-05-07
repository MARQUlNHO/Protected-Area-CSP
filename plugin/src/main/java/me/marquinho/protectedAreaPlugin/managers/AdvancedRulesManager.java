package me.marquinho.protectedAreaPlugin.managers;

import me.marquinho.protectedAreaPlugin.ProtectedAreaPlugin;
import me.marquinho.protectedAreaPlugin.models.AdvancedAreaRules;
import me.marquinho.protectedAreaPlugin.models.AdvancedRuleType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AdvancedRulesManager {
    private final ProtectedAreaPlugin plugin;
    private final Map<String, AdvancedAreaRules> advancedRules;
    private final File rulesFolder;

    public AdvancedRulesManager(ProtectedAreaPlugin plugin) {
        this.plugin = plugin;
        this.advancedRules = new HashMap<>();
        this.rulesFolder = new File(plugin.getDataFolder(), "Advanced/Rules");

        if (!rulesFolder.exists()) {
            rulesFolder.mkdirs();
        }
    }

    public void loadAllRules() {
        advancedRules.clear();

        if (!rulesFolder.exists()) {
            rulesFolder.mkdirs();
            return;
        }

        File[] files = rulesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            loadRulesFromFile(file);
        }

        plugin.getLogger().info("Cargadas " + advancedRules.size() + " configuraciones de reglas avanzadas");
    }

    private void loadRulesFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String areaId = file.getName().replace(".yml", "");

        AdvancedAreaRules rules = new AdvancedAreaRules(areaId);

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            String rulePath = ruleType.getKey();

            if (config.contains(rulePath + ".Blocks")) {
                List<String> blocks = config.getStringList(rulePath + ".Blocks");
                for (String block : blocks) {
                    rules.addBlock(ruleType, block);
                }
            }

            if (config.contains(rulePath + ".Entities")) {
                List<String> entities = config.getStringList(rulePath + ".Entities");
                for (String entity : entities) {
                    rules.addEntity(ruleType, entity);
                }
            }
        }

        advancedRules.put(areaId, rules);
    }

    public void reloadRules() {
        advancedRules.clear();
        loadAllRules();
    }

    public void saveRules(String areaId) {
        AdvancedAreaRules rules = advancedRules.get(areaId);
        if (rules == null) return;

        File file = new File(rulesFolder, areaId + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            Set<String> blocks = rules.getBlocks(ruleType);
            Set<String> entities = rules.getEntities(ruleType);

            if (!blocks.isEmpty() || !entities.isEmpty()) {
                config.set(ruleType.getKey() + ".Blocks", new ArrayList<>(blocks));
                config.set(ruleType.getKey() + ".Entities", new ArrayList<>(entities));
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar reglas avanzadas para el área: " + areaId);
            e.printStackTrace();
        }
    }

    public AdvancedAreaRules getRules(String areaId) {
        return advancedRules.computeIfAbsent(areaId, AdvancedAreaRules::new);
    }

    public boolean addBlockRule(String areaId, AdvancedRuleType ruleType, String blockId) {
        AdvancedAreaRules rules = getRules(areaId);
        boolean added = rules.addBlock(ruleType, blockId);
        if (added) {
            saveRules(areaId);
        }
        return added;
    }

    public boolean addEntityRule(String areaId, AdvancedRuleType ruleType, String entityId) {
        AdvancedAreaRules rules = getRules(areaId);
        boolean added = rules.addEntity(ruleType, entityId);
        if (added) {
            saveRules(areaId);
        }
        return added;
    }

    public boolean removeBlockRule(String areaId, AdvancedRuleType ruleType, String blockId) {
        AdvancedAreaRules rules = advancedRules.get(areaId);
        if (rules == null) return false;

        boolean removed = rules.removeBlock(ruleType, blockId);
        if (removed) {
            saveRules(areaId);
        }
        return removed;
    }

    public boolean removeEntityRule(String areaId, AdvancedRuleType ruleType, String entityId) {
        AdvancedAreaRules rules = advancedRules.get(areaId);
        if (rules == null) return false;

        boolean removed = rules.removeEntity(ruleType, entityId);
        if (removed) {
            saveRules(areaId);
        }
        return removed;
    }

    public void clearRule(String areaId, AdvancedRuleType ruleType) {
        AdvancedAreaRules rules = advancedRules.get(areaId);
        if (rules == null) return;

        rules.clearRule(ruleType);
        saveRules(areaId);
    }

    public void deleteAreaRules(String areaId) {
        advancedRules.remove(areaId);
        File file = new File(rulesFolder, areaId + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean hasAdvancedRules(String areaId) {
        return advancedRules.containsKey(areaId);
    }
}