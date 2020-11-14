package tech.codestory.zookeeper.aalvcai.base_I0Itec_ZK_lock;

import java.util.concurrent.TimeUnit;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/13 16:53
 */
public class Test {
    static volatile int num = 0;
    //static I0Itec_DistributeLock distributeLock = new I0Itec_DistributeLock();
    static Zk distributeLock = new Zk();
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            new Thread(()->{
                try {
                    distributeLock.lock();
                    TimeUnit.MILLISECONDS.sleep(100);
                    for (int j = 0; j < 10; j++) {
                        num++;
                    }
                    System.out.println( "num的值是 : "+ num );
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    distributeLock.unlock();
                }
            }).start();
        }
    }
}
