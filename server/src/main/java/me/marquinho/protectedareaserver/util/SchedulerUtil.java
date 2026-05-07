package me.marquinho.protectedareaserver.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class SchedulerUtil {

    public interface Cancellable {
        void cancel();
    }

    private static final CopyOnWriteArrayList<TaskEntry> tasks = new CopyOnWriteArrayList<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    public static void runLater(Runnable task, int delayTicks) {
        tasks.add(new TaskEntry(task, Math.max(1, delayTicks), -1));
    }

    public static Cancellable runTimer(Runnable task, int initialDelay, int periodTicks) {
        TaskEntry entry = new TaskEntry(task, Math.max(1, initialDelay), periodTicks);
        tasks.add(entry);
        return () -> entry.cancelled = true;
    }

    private static void tick() {
        Iterator<TaskEntry> it = tasks.iterator();
        while (it.hasNext()) {
            TaskEntry entry = it.next();
            if (entry.cancelled) {
                tasks.remove(entry);
                continue;
            }
            entry.remaining--;
            if (entry.remaining <= 0) {
                try {
                    entry.task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (entry.period > 0) {
                    entry.remaining = entry.period;
                } else {
                    tasks.remove(entry);
                }
            }
        }
    }

    private static class TaskEntry {
        final Runnable task;
        int remaining;
        final int period;
        volatile boolean cancelled = false;

        TaskEntry(Runnable task, int initialDelay, int period) {
            this.task = task;
            this.remaining = initialDelay;
            this.period = period;
        }
    }
}
