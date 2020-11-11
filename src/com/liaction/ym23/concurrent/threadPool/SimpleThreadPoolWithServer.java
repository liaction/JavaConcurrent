package com.liaction.ym23.concurrent.threadPool;

import java.util.LinkedList;

public class SimpleThreadPoolWithServer {

    private final static int DEFAULT_POOL_SIZE = 10;
    private final int poolSize;
    private final static String THREAD_PREFIX = "Thread-ym23-";
    private final static String THREAD_GROUP_NAME = "Thread_pool_group";

    private final static LinkedList<Runnable> WORK_QUEUE = new LinkedList<>();
    private final static LinkedList<WorkTask> THREAD_QUEUE = new LinkedList<>();

    private WorkTaskServer workTaskServer;
    private final static Object THREAD_POOL_LOCK = new Object();
    private final static Object LOCK = new Object();

    private boolean destroyed = false;

    public SimpleThreadPoolWithServer() {
        this(DEFAULT_POOL_SIZE);
    }

    public SimpleThreadPoolWithServer(int poolSize) {
        this.poolSize = poolSize;
        initThreadPool();
    }

    private void initThreadPool() {
        workTaskServer = new WorkTaskServer(poolSize);
        workTaskServer.start();
    }

    public void addTask(Runnable runnable) {
        if (isDestroyed()) {
            throw new IllegalStateException("thread pool is died, so you should not add task again.");
        }
        synchronized (WORK_QUEUE) {
            WORK_QUEUE.addLast(runnable);
            WORK_QUEUE.notifyAll();
        }
    }

    public void shutDown(long waitTime) throws InterruptedException {
        shutDown(waitTime > 0, waitTime);
    }

    public void shutDown(boolean now, long waitTime) throws InterruptedException {
        if (!now) {
            shutDown();
            return;
        }
        synchronized (THREAD_POOL_LOCK) {
            THREAD_POOL_LOCK.wait(waitTime);
            workTaskServer.interrupt();
            destroyed = true;
            System.out.println("the thread pool server is destroyed now.");
        }
    }

    public void shutDown() throws InterruptedException {
        // 等待任务做完
        while (!WORK_QUEUE.isEmpty()) {
            Thread.sleep(23);
        }
        int currentThreadSize = THREAD_QUEUE.size();
        while (currentThreadSize > 0) {
            for (WorkTask workTask :
                    THREAD_QUEUE) {
                if (workTask.getWorkState() == WorkState.BLOCKED) {
                    workTask.interrupt();
                    workTask.close();
                    currentThreadSize--;
                }
            }
            Thread.sleep(23);
        }
        destroyed = true;
        System.out.println("thread pool is destroyed.");
    }

    public int getPoolSize() {
        return poolSize;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    private enum WorkState {
        IDLE, RUNNING, BLOCKED, DEAD
    }

    private static class WorkTaskServer extends Thread {
        private int thread_seq = 0;
        private final int poolSize;

        private WorkTaskServer(int poolSize) {
            this.poolSize = poolSize;
        }

        @Override
        public void run() {
            // 添加
            for (int i = 0; i < poolSize; i++) {
                WorkTask workTask = createWorkTask();
                THREAD_QUEUE.addLast(workTask);
                workTask.setDaemon(true);
                workTask.start();
            }
            System.out.println(THREAD_QUEUE.size());
            synchronized (LOCK) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    System.out.println("the thread pool is destroyed now.");
                }
            }
        }

        private WorkTask createWorkTask() {
            return new WorkTask(new ThreadGroup(THREAD_GROUP_NAME), THREAD_PREFIX + (thread_seq++));
        }
    }

    private static class WorkTask extends Thread {

        private volatile WorkState workState = WorkState.IDLE;

        WorkTask(ThreadGroup threadGroup, String name) {
            super(threadGroup, name);
        }

        @Override
        public void run() {
            OUTER:
            while (!isDead()) {
                Runnable runnable;
                synchronized (WORK_QUEUE) {
                    while (WORK_QUEUE.isEmpty()) {
                        try {
                            this.workState = WorkState.BLOCKED;
                            WORK_QUEUE.wait();
                        } catch (InterruptedException e) {
                            break OUTER;
                        }
                    }
                    this.workState = WorkState.RUNNING;
                    runnable = WORK_QUEUE.removeFirst();
                }
                if (runnable != null) {
                    runnable.run();
                }
                this.workState = WorkState.IDLE;
            }
        }

        public WorkState getWorkState() {
            return workState;
        }

        public boolean isDead() {
            return getWorkState() == WorkState.DEAD;
        }

        public void close() {
            workState = WorkState.DEAD;
        }
    }

}
