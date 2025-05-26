package org.example.concurrent.policies;

import org.example.concurrent.CustomThreadPoolExecutor;

public class AbortPolicy implements RejectionPolicy {
    @Override
    public void reject(Runnable task, CustomThreadPoolExecutor executor) {
        executor.log("[Rejected] Task " + task + " was rejected due to overload!");
        throw new RuntimeException("Task " + task + " rejected from " + executor);
    }
}