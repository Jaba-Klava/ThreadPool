package org.example.concurrent.policies;

import org.example.concurrent.CustomThreadPoolExecutor;

public class CallerRunsPolicy implements RejectionPolicy {
    @Override
    public void reject(Runnable task, CustomThreadPoolExecutor executor) {
        executor.log("[Rejected] Task " + task + " will be executed in caller thread");
        if (!executor.isShutdown()) {
            task.run();
        }
    }
}