package org.lulu.share;

import java.util.concurrent.*;

public class TaskHandler {
    private static final String TAG = "TaskHandler";
    private static TaskHandler instance;
    /**执行线程池*/
    private ExecutorService executor;
    /**任务队列 FIFO*/
    private final LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    /**用于执行 Task*/
    private final Runnable invokeTask = () -> {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Runnable task = taskQueue.take();
                System.out.println( "添加任务线程池：" + task);
                addExecutor(task);
            }
        } catch (InterruptedException e) {
            System.out.println( "invokeTask 抛出异常");
            e.printStackTrace();
        }
    };

    public static TaskHandler getInstance() {
        if (instance == null) {
            synchronized (TaskHandler.class) {
                if (instance == null) {
                    instance = new TaskHandler();
                }
            }
        }
        return instance;
    }

    private TaskHandler() {
        //启动执行任务
        executorService().execute(invokeTask);
    }

    private synchronized ExecutorService executorService() {
        if (executor == null) {
            //核心线程：4 最大线程数：10 存活时间：5 分钟
            executor = new ThreadPoolExecutor(
                    3,
                    10,
                    5 * 60,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), r -> new Thread(r, "TaskHandler"), (r, executor) -> {
                //处理被抛出来的任务(被拒绝的任务)
                enqueue(r);
                System.out.println("executorService: 任务拒绝！");
            });
        }
        return executor;
    }

    /**入队添加任务*/
    public synchronized void enqueue(Runnable task) {
        try {
            taskQueue.add(task);
        } catch (Exception e) {
            System.out.println( "addTask 抛出异常");
            e.printStackTrace();
        }
    }

    /**添加到线程池执行*/
    private synchronized void addExecutor(Runnable runnable) {
        executorService().execute(runnable);
    }
}
