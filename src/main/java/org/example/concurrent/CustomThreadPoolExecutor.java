package org.example.concurrent;

import org.example.concurrent.policies.RejectionPolicy;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CustomThreadPoolExecutor implements CustomExecutor {
    private final ReentrantLock mainLock = new ReentrantLock();
    private final List<Worker> workers = new ArrayList<>();
    private final List<TaskQueue> queues;
    private final ThreadFactory threadFactory;
    private final RejectionPolicy rejectionPolicy;

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int minSpareThreads;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;

    private volatile boolean isShutdown = false;
    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger nextQueueIndex = new AtomicInteger(0);

    public CustomThreadPoolExecutor(int corePoolSize, int maxPoolSize, int minSpareThreads,
                                    long keepAliveTime, TimeUnit timeUnit, int queueSize,
                                    ThreadFactory threadFactory, RejectionPolicy rejectionPolicy) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.minSpareThreads = minSpareThreads;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.threadFactory = threadFactory;
        this.rejectionPolicy = rejectionPolicy;

        this.queues = new ArrayList<>(corePoolSize);
        for (int i = 0; i < corePoolSize; i++) {
            queues.add(new TaskQueue(queueSize));
        }


        for (int i = 0; i < corePoolSize; i++) {
            addWorker(queues.get(i));
        }
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor is shutting down");
        }

        boolean taskAdded = false;
        TaskQueue selectedQueue = null;

        int startIdx = nextQueueIndex.getAndIncrement() % queues.size();
        for (int i = 0; i < queues.size(); i++) {
            int idx = (startIdx + i) % queues.size();
            TaskQueue queue = queues.get(idx);
            if (queue.offer(command)) {
                taskAdded = true;
                selectedQueue = queue;
                break;
            }
        }

        if (taskAdded) {
            totalTasks.incrementAndGet();
            log("[Pool] Task accepted into queue #" + queues.indexOf(selectedQueue) + ": " + command);

            checkSpareThreads();
            return;
        }


        if (workers.size() < maxPoolSize) {
            TaskQueue newQueue = new TaskQueue(queueSize);
            if (newQueue.offer(command)) {
                queues.add(newQueue);
                addWorker(newQueue);
                totalTasks.incrementAndGet();
                log("[Pool] Task accepted into new queue #" + (queues.size() - 1) + ": " + command);
                return;
            }
        }

        // Reject task according to policy
        rejectionPolicy.reject(command, this);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }


    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public void shutdown() {
        mainLock.lock();
        try {
            isShutdown = true;
            for (Worker worker : workers) {
                worker.interruptIfIdle();
            }
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void shutdownNow() {
        mainLock.lock();
        try {
            isShutdown = true;
            for (Worker worker : workers) {
                worker.interruptNow();
            }
            for (TaskQueue queue : queues) {
                queue.clear();
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void addWorker(TaskQueue queue) {
        Worker worker = new Worker(queue);
        Thread thread = threadFactory.newThread(worker);
        worker.setThread(thread);

        mainLock.lock();
        try {
            workers.add(worker);
        } finally {
            mainLock.unlock();
        }

        thread.start();
    }

    private void checkSpareThreads() {
        int freeThreads = 0;
        for (Worker worker : workers) {
            if (worker.isIdle()) {
                freeThreads++;
            }
        }

        if (freeThreads < minSpareThreads && workers.size() < maxPoolSize) {
            TaskQueue newQueue = new TaskQueue(queueSize);
            queues.add(newQueue);
            addWorker(newQueue);
        }
    }

    private void removeWorker(Worker worker) {
        mainLock.lock();
        try {
            workers.remove(worker);
            queues.remove(worker.getQueue());
        } finally {
            mainLock.unlock();
        }
    }

    public void log(String message) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + message);
    }

    private class Worker implements Runnable {
        private final TaskQueue queue;
        private Thread thread;
        private volatile boolean running = true;
        private volatile boolean processingTask = false;

        public Worker(TaskQueue queue) {
            this.queue = queue;
        }

        public void setThread(Thread thread) {
            this.thread = thread;
        }

        public TaskQueue getQueue() {
            return queue;
        }

        public boolean isIdle() {
            return !processingTask && queue.isEmpty();
        }

        public void interruptIfIdle() {
            if (!processingTask) {
                thread.interrupt();
            }
        }

        public void interruptNow() {
            thread.interrupt();
        }

        @Override
        public void run() {
            try {
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        Runnable task = queue.poll(keepAliveTime, timeUnit);
                        if (task != null) {
                            processingTask = true;
                            log("[Worker] " + Thread.currentThread().getName() + " executes " + task);
                            task.run();
                            totalTasks.decrementAndGet();
                            processingTask = false;
                        } else if (workers.size() > corePoolSize) {
                            // Idle timeout for non-core threads
                            log("[Worker] " + Thread.currentThread().getName() + " idle timeout, stopping");
                            break;
                        }
                    } catch (InterruptedException e) {
                        if (isShutdown && queue.isEmpty()) {
                            break;
                        }
                        // Ignore and continue
                    }
                }
            } finally {
                removeWorker(this);
                log("[Worker] " + Thread.currentThread().getName() + " terminated");
            }
        }
    }

    private static class TaskQueue extends ArrayBlockingQueue<Runnable> {
        public TaskQueue(int capacity) {
            super(capacity);
        }
    }
}