package tech.codestory.zookeeper.aalvcai.base_curator_ZK_lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryOneTime;

public class InterProcessMutexE {
    static int a = 0;
    public static void main(String[] args) throws Exception {
/**
 * 这个不行 失败的
 */
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", new RetryOneTime(1000));
        client.start();

        for (int i = 0; i < 50; i++) {
            new Thread(() -> {
                InterProcessMutex lock = new InterProcessMutex(client, "/d-lock");
                String threadName = Thread.currentThread().getName();
                try {
                    System.err.println("[" + threadName + "] 尝试获取锁...");
                    lock.acquire();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.err.println("[" + threadName + "] 获得锁...");


                try {
                    System.err.println("5秒 执行中...");
                    Thread.sleep(1000 );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println("[" + threadName + "] 执行完，释放锁...");



                try {
                    lock.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.err.println("[" + threadName + "] -------------- 释放成功 --------------");
            }).start();
        }
    }
}