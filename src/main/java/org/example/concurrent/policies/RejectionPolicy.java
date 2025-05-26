package org.example.concurrent.policies;

import org.example.concurrent.CustomThreadPoolExecutor;

public interface RejectionPolicy {
    void reject(Runnable task, CustomThreadPoolExecutor executor);
}