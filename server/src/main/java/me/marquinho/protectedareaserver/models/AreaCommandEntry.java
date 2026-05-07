package me.marquinho.protectedareaserver.models;

public class AreaCommandEntry {

    private final int maxUses;
    private final int delayTicks;
    private final String command;

    public AreaCommandEntry(int maxUses, int delayTicks, String command) {
        this.maxUses    = maxUses;
        this.delayTicks = delayTicks;
        this.command    = command;
    }

    public static AreaCommandEntry fromString(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.split(" ", 3);
        if (parts.length < 3) return null;
        try {
            int uses  = Integer.parseInt(parts[0]);
            int delay = Integer.parseInt(parts[1]);
            return new AreaCommandEntry(uses, delay, parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String toYmlString() {
        return maxUses + " " + delayTicks + " " + command;
    }

    public String buildCommand(String playerName, double x, double y, double z) {
        String coords = x + " " + y + " " + z;
        return command
                .replace("{player}", playerName)
                .replace("{coords}", coords);
    }

    public int getMaxUses()    { return maxUses; }
    public int getDelayTicks() { return delayTicks; }
    public String getCommand() { return command; }
}