package org.btik.server.video.device.task;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 异步执行器
 */
public class AsyncTaskExecutor extends Thread {
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    /**
     * 阻塞消除器
     */
    Runnable blockCanceler = () -> {
    };

    private volatile boolean runFlag = true;


    @Override
    public void run() {
        while (runFlag) {
            try {
                Runnable take = queue.take();
                try {
                    take.run();
                } catch (Exception e) {
                    // 任务失败不能影响核心线程
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                shutdown(e.getMessage());
            }
        }
    }

    public void execute(Runnable runnable) {
        queue.add(runnable);
    }

    public void shutdown(String msg) {
        System.out.println("shutdown with:" + msg);
        runFlag = false;
        queue.add(blockCanceler);
    }
}
