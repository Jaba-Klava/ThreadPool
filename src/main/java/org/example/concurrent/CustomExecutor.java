package org.example.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface CustomExecutor extends java.util.concurrent.Executor {
    <T> Future<T> submit(Callable<T> callable);
    void shutdown();
    void shutdownNow();
    boolean isShutdown(); // Добавляем этот метод
}