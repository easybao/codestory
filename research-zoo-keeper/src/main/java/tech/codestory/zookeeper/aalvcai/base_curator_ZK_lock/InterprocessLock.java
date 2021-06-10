package tech.codestory.zookeeper.aalvcai.base_curator_ZK_lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.CountDownLatch;

public class InterprocessLock {
    static volatile int a = 0;
    static CountDownLatch countDownLatch = new CountDownLatch(10);

    public static void main(String[] args)  {
        // 这个不行,报异常原因: 多个线程使用同一把锁, 线程1对锁释放删除了, 线程2获取不到锁了,报异常
        CuratorFramework zkClient = getZkClient();
        String lockPath = "/lock";
        InterProcessMutex lock = new InterProcessMutex(zkClient, lockPath);
        //模拟100个线程抢锁
        for (int i = 0; i < 100; i++) {
            new Thread(new TestThread(i, lock)).start();
        }
        while (Thread.activeCount()>2){}
        System.out.println("a的值最终为"+a);

    }

    static class TestThread implements Runnable {
        private Integer threadFlag;
        private InterProcessMutex lock;

        public TestThread(Integer threadFlag, InterProcessMutex lock) {
            this.threadFlag = threadFlag;
            this.lock = lock;
        }

        @Override
        public void run() {
            try {
                lock.acquire();
                System.out.println("第"+threadFlag+"线程获取到了锁");
                a++;
                //等到1秒后释放锁
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    lock.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static CuratorFramework getZkClient() {
        String zkServerAddress = "127.0.0.1:2181";
        ExponentialBackoffRetry retryPolicy =
                new ExponentialBackoffRetry(1000, 3, 5000);
        CuratorFramework zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkServerAddress)
                .sessionTimeoutMs(5000)// 会话超时
                .connectionTimeoutMs(5000)// 连接超时
                .retryPolicy(retryPolicy)//重试策略
                .build();
        zkClient.start();
        return zkClient;
    }

}