package me.marquinho.protectedAreaPlugin.models;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum AdvancedRuleType {
    YES_BREAK("yes_break", "Allow breaking specific blocks", Target.BLOCK),
    YES_PLACE("yes_place", "Allow placing specific blocks", Target.BLOCK),
    YES_INTERACT("yes_interact", "Allow interacting with specific blocks/entities", Target.BLOCK, Target.ENTITY),
    YES_DROP("yes_drop", "Allow dropping specific items", Target.ITEM),
    YES_COLLECT("yes_collect", "Allow collecting specific items", Target.ITEM),

    NO_BREAK("no_break", "Block breaking specific blocks", Target.BLOCK),
    NO_PLACE("no_place", "Block placing specific blocks", Target.BLOCK),
    NO_INTERACT("no_interact", "Block interacting with specific blocks/entities", Target.BLOCK, Target.ENTITY),
    NO_DROP("no_drop", "Block dropping specific items", Target.ITEM),
    NO_COLLECT("no_collect", "Block collecting specific items", Target.ITEM);

    public enum Target {
        BLOCK("block", "Block"),
        ENTITY("entity", "Entity"),
        ITEM("item", "Item");

        private final String key;
        private final String displayName;

        Target(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String key;
    private final String description;
    private final Set<Target> targets;

    AdvancedRuleType(String key, String description, Target... targets) {
        this.key = key;
        this.description = description;

        Set<Target> set = EnumSet.noneOf(Target.class);
        Collections.addAll(set, targets);
        this.targets = Collections.unmodifiableSet(set);
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public Set<Target> getTargets() {
        return targets;
    }

    public boolean supports(Target target) {
        return targets.contains(target);
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
