package me.marquinho.protectedarea.client.debug;

public final class DebugStateHolder {

    private static final DebugState INSTANCE = new DebugState();

    private DebugStateHolder() {}

    public static DebugState get() {
        return INSTANCE;
    }
}