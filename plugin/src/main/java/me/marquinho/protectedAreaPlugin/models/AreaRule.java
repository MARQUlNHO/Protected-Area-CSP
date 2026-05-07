package me.marquinho.protectedAreaPlugin.models;

public enum AreaRule {
    NO_BREAK("no_break", "No se pueden romper bloques"),
    NO_PLACE("no_place", "No se pueden colocar bloques"),
    NO_INTERACT("no_interact", "No se puede interactuar con bloques/entidades"),
    NO_MOBGRIEFING("no_mobgriefing", "Las entidades no pueden romper bloques"),
    NO_PVP("no_pvp", "No hay combate entre jugadores"),
    NO_ENTITYATTACK("no_entityattack", "Los jugadores no pueden atacar entidades"),
    NO_DAMAGE("no_damage", "Los jugadores no reciben daño"),
    NO_DROP("no_drop", "Los jugadores no pueden tirar items"),
    NO_COLLECT("no_collect", "Los jugadores no pueden recoger items"),
    NO_SPAWN("no_spawn", "No hay spawn natural de entidades"),
    NO_ENTRY("no_entry", "Impide la entrada al área (barrera invisible)"),
    NO_EXIT("no_exit", "Impide la salida del área (permite entrada)");

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