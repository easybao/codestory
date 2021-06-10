package tech.codestory.zookeeper.aalvcai.base_curator_ZK_lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/12/17 11:35
 */
public class CuratorDistributeZkLock {
    private String localhost = "127.0.0.1:2181";
    private String path = "/lvcai123";
    private CuratorFramework curatorFramework;
    //锁对象
    private InterProcessMutex dMutex;

    public CuratorDistributeZkLock() {
        curatorFramework =
                CuratorFrameworkFactory.newClient(localhost,new ExponentialBackoffRetry(1000,3));
        curatorFramework.start();
        dMutex = new InterProcessMutex(curatorFramework,path);
    }

    public void lock(){
        try {
            dMutex.acquire(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unlock(){
        try {
            dMutex.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static int a = 0;
    public static void main(String[] args) {

        /**
         *          <dependency>
         *             <groupId>org.apache.curator</groupId>
         *             <artifactId>curator-recipes</artifactId>
         *             <version>2.11.0</version>
         *         </dependency>
         */
        CuratorDistributeZkLock test = new CuratorDistributeZkLock();
        for (int i = 0; i < 100; i++) {
            new Thread(()->{
                try {
                    test.lock();
                    for (int j = 0; j < 1000; j++) {
                        a++;
                        System.out.println("a的值是"+a);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    test.unlock();
                }
            }).start();
        }

    }
}
