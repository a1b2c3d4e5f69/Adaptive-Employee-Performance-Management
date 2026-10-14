package com.aepm.logic.deadline;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class DeadlineEscalationScheduler {

    private static final DeadlineEscalationScheduler INSTANCE =
        new DeadlineEscalationScheduler();

    private final ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "deadline-escalation");
            thread.setDaemon(true);
            return thread;
        });

    private ScheduledFuture<?> scheduledTask;
    private long sessionGeneration = 0;

    private DeadlineEscalationScheduler() {
    }

    public static DeadlineEscalationScheduler getInstance() {
        return INSTANCE;
    }

    public synchronized void start(Supplier<String> tokenSupplier) {

        if (scheduledTask != null && !scheduledTask.isDone()) {
            return;
        }

        long generation = ++sessionGeneration;

        scheduledTask = executor.scheduleWithFixedDelay(
            () -> executeSafely(tokenSupplier, generation),
            0,
            5,
            TimeUnit.MINUTES
        );

        System.out.println("[Escalation] Scheduler started.");
    }

    public synchronized void stop() {

        sessionGeneration++;

        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }

        System.out.println("[Escalation] Scheduler stopped.");
    }

    private void executeSafely(
            Supplier<String> tokenSupplier,
            long generation
    ) {
        try {
            synchronized (this) {
                if (generation != sessionGeneration) {
                    return;
                }
            }

            String token = tokenSupplier.get();

            if (token == null || token.isBlank()) {
                return;
            }

            synchronized (this) {
                if (generation != sessionGeneration) {
                    return;
                }
            }

            DeadlineEscalationService service =
                new DeadlineEscalationService();

            var result = service.execute(token);

            System.out.println(
                "[Escalation] Automatic run: Created="
                    + result.created()
                    + ", Skipped=" + result.skipped()
                    + ", Failed=" + result.failed()
            );

        } catch (Exception ex) {

            System.err.println(
                "[Escalation] Automatic run failed: "
                    + ex.getMessage()
            );
        }
    }
}
