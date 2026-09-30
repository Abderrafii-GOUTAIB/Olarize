package com.olarize.assistant.infrastructure.out.provider;

import com.olarize.assistant.application.exception.AssistantUnavailable;
import com.olarize.assistant.application.port.out.ExpiryIntentPort;
import com.olarize.assistant.domain.model.ExpiryToolCall;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public final class BoundedExpiryIntentAdapter implements ExpiryIntentPort, AutoCloseable {
    private final Function<String, ExpiryToolCall> planner;
    private final Duration timeout;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(4), runnable -> {
                var thread = new Thread(runnable, "expiry-intent"); thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    public BoundedExpiryIntentAdapter(Function<String, ExpiryToolCall> planner, Duration timeout) {
        this.planner = planner; this.timeout = timeout;
    }
    @Override public ExpiryToolCall plan(String question) {
        Future<ExpiryToolCall> future;
        try { future = executor.submit(() -> planner.apply(question)); }
        catch (RejectedExecutionException unavailable) { throw new AssistantUnavailable(); }
        try { return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS); }
        catch (InterruptedException interrupted) {
            future.cancel(true); Thread.currentThread().interrupt(); throw new AssistantUnavailable();
        } catch (ExecutionException | TimeoutException unavailable) {
            future.cancel(true); throw new AssistantUnavailable();
        }
    }
    @Override public void close() { executor.shutdownNow(); }
}
