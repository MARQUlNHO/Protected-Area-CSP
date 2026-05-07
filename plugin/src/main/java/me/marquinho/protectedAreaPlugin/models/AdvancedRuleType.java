package me.marquinho.protectedAreaPlugin.models;

public enum AdvancedRuleType {
    YES_BREAK("yes_break", "Permitir romper bloques específicos"),
    YES_PLACE("yes_place", "Permitir colocar bloques específicos"),
    YES_INTERACT("yes_interact", "Permitir interactuar con bloques/entidades específicos"),
    YES_DROP("yes_drop", "Permitir tirar items específicos"),
    YES_COLLECT("yes_collect", "Permitir recoger items específicos"),

    NO_BREAK("no_break", "Bloquear romper bloques específicos"),
    NO_PLACE("no_place", "Bloquear colocar bloques específicos"),
    NO_INTERACT("no_interact", "Bloquear interactuar con bloques/entidades específicos"),
    NO_DROP("no_drop", "Bloquear tirar items específicos"),
    NO_COLLECT("no_collect", "Bloquear recoger items específicos");

    private final String key;
    private final String description;

    AdvancedRuleType(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public boolean isYesRule() {
        return this == YES_BREAK || this == YES_PLACE || this == YES_INTERACT || this == YES_DROP || this == YES_COLLECT;
    }

    public boolean isNoRule() {
        return this == NO_BREAK || this == NO_PLACE || this == NO_INTERACT || this == NO_DROP || this == NO_COLLECT;
    }

    public static AdvancedRuleType fromKey(String key) {
        for (AdvancedRuleType rule : values()) {
            if (rule.key.equalsIgnoreCase(key)) {
                return rule;
            }
        }
        return null;
    }

    public static String[] getAllKeys() {
        AdvancedRuleType[] rules = values();
        String[] keys = new String[rules.length];
        for (int i = 0; i < rules.length; i++) {
            keys[i] = rules[i].key;
        }
        return keys;
    }
}