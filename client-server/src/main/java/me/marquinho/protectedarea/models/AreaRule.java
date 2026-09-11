package me.marquinho.protectedarea.models;

public enum AreaRule {
    NO_BREAK("no_break", "Blocks cannot be broken"),
    NO_PLACE("no_place", "Blocks cannot be placed"),
    NO_INTERACT("no_interact", "Cannot interact with blocks/entities"),
    NO_MOBGRIEFING("no_mobgriefing", "Entities cannot break blocks"),
    NO_PVP("no_pvp", "No combat between players"),
    NO_ENTITYATTACK("no_entityattack", "Players cannot attack entities"),
    NO_DAMAGE("no_damage", "Players take no damage"),
    NO_DROP("no_drop", "Players cannot drop items"),
    NO_COLLECT("no_collect", "Players cannot collect items"),
    NO_SPAWN("no_spawn", "No natural entity spawning"),
    NO_ENTRY("no_entry", "Prevents entry into the area (invisible barrier)"),
    NO_EXIT("no_exit", "Prevents leaving the area (allows entry)");

    private final String key;
    private final String description;

    AreaRule(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCollisionRule() {
        return this == NO_ENTRY || this == NO_EXIT;
    }

    public static AreaRule fromKey(String key) {
        for (AreaRule rule : values()) {
            if (rule.key.equalsIgnoreCase(key)) {
                return rule;
            }
        }
        return null;
    }

    public static String[] getAllKeys() {
        AreaRule[] rules = values();
        String[] keys = new String[rules.length];
        for (int i = 0; i < rules.length; i++) {
            keys[i] = rules[i].key;
        }
        return keys;
    }
}