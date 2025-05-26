package org.example;

import org.example.concurrent.CustomThreadPoolExecutor;
import org.example.concurrent.CustomThreadFactory;
import org.example.concurrent.policies.CallerRunsPolicy;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(
                2, 4, 1, 5, TimeUnit.SECONDS, 5,
                new CustomThreadFactory("MyPool"),
                new CallerRunsPolicy()
        );

        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Task " + taskId + " started in " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("Task " + taskId + " completed");
                }

                @Override
                public String toString() {
                    return "Task-" + taskId;
                }
            });
            Thread.sleep(300);
        }
        executor.shutdown();


        Thread.sleep(2000);

        System.out.println("\n=== Final Status ===");
        System.out.println("Executor shutdown: " + executor.isShutdown());
        System.out.println("Active threads: " + Thread.activeCount());
    }
}