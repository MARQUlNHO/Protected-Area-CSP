package me.marquinho.protectedarea.managers;

import me.marquinho.protectedarea.ProtectedAreaInit;
import me.marquinho.protectedarea.models.AdvancedAreaRules;
import me.marquinho.protectedarea.models.AdvancedRuleType;
import me.marquinho.protectedarea.util.SimpleYaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AdvancedRulesManager {

    private final ProtectedAreaInit plugin;
    private final Map<String, AdvancedAreaRules> advancedRules;
    private final File rulesFolder;

    public AdvancedRulesManager(ProtectedAreaInit plugin) {
        this.plugin = plugin;
        this.advancedRules = new HashMap<>();
        this.rulesFolder = new File(plugin.getDataPath(), "Advanced/Rules");
        rulesFolder.mkdirs();
    }

    public void loadAllRules() {
        advancedRules.clear();
        if (!rulesFolder.exists()) { rulesFolder.mkdirs(); return; }

        loadRulesRecursively(rulesFolder);

        plugin.getLogger().info("Loaded " + advancedRules.size() + " advanced rule configurations");
    }


    private void loadRulesRecursively(File currentFolder) {
        File[] entries = currentFolder.listFiles();
        if (entries == null) return;

        for (File entry : entries) {
            if (entry.isDirectory()) {
                loadRulesRecursively(entry);
            } else if (entry.getName().endsWith(".yml")) {
                String relativePath = rulesFolder.toPath().relativize(entry.toPath()).toString();
                String areaId = relativePath.replace(File.separator, "/").replace(".yml", "");
                loadRulesFromFile(entry, areaId);
            }
        }
    }

    public void reloadRulesFor(String areaId) {
        advancedRules.remove(areaId);
        File file = new File(rulesFolder, areaId + ".yml");
        if (file.exists()) loadRulesFromFile(file, areaId);
    }

    private void loadRulesFromFile(File file, String areaId) {
        SimpleYaml config = SimpleYaml.load(file);
        AdvancedAreaRules rules = new AdvancedAreaRules(areaId);

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            String rulePath = ruleType.getKey();
            if (config.contains(rulePath + ".Blocks"))
                for (String block : config.getStringList(rulePath + ".Blocks"))
                    rules.addBlock(ruleType, block);

            if (config.contains(rulePath + ".Entities"))
                for (String entity : config.getStringList(rulePath + ".Entities"))
                    rules.addEntity(ruleType, entity);
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
        SimpleYaml config = new SimpleYaml();

        for (AdvancedRuleType ruleType : AdvancedRuleType.values()) {
            Set<String> blocks = rules.getBlocks(ruleType);
            Set<String> entities = rules.getEntities(ruleType);
            if (!blocks.isEmpty() || !entities.isEmpty()) {
                config.set(ruleType.getKey() + ".Blocks", new ArrayList<>(blocks));
                config.set(ruleType.getKey() + ".Entities", new ArrayList<>(entities));
            }
        }

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().error("Error saving advanced rules for: " + areaId, e); }
    }

    public AdvancedAreaRules getRules(String areaId) {
        return advancedRules.computeIfAbsent(areaId, AdvancedAreaRules::new);
    }

    public boolean addBlockRule(String areaId, AdvancedRuleType ruleType, String blockId) {
        AdvancedAreaRules rules = getRules(areaId);
        boolean added = rules.addBlock(ruleType, blockId);
        if (added) saveRules(areaId);
        return added;
    }

    public boolean addEntityRule(String areaId, AdvancedRuleType ruleType, String entityId) {
        AdvancedAreaRules rules = getRules(areaId);
        boolean added = rules.addEntity(ruleType, entityId);
        if (added) saveRules(areaId);
        return added;
    }

    public boolean removeBlockRule(String areaId, AdvancedRuleType ruleType, String blockId) {
        AdvancedAreaRules rules = advancedRules.get(areaId);
        if (rules == null) return false;
        boolean removed = rules.removeBlock(ruleType, blockId);
        if (removed) saveRules(areaId);
        return removed;
    }

    public boolean removeEntityRule(String areaId, AdvancedRuleType ruleType, String entityId) {
        AdvancedAreaRules rules = advancedRules.get(areaId);
        if (rules == null) return false;
        boolean removed = rules.removeEntity(ruleType, entityId);
        if (removed) saveRules(areaId);
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
        if (file.exists()) file.delete();
    }

    public boolean hasAdvancedRules(String areaId) {
        return advancedRules.containsKey(areaId);
    }
}
