package tech.codestory.zookeeper.aalvcai;

import lombok.extern.slf4j.Slf4j;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/11 17:03
 */
@Slf4j
public class DistributeLockTest {
    static volatile int num = 0;
    public static void main(String[] args) {
        DistributeLock lock = new DistributeLockImpl("127.0.0.1:2181");
        for (int i = 0; i < 3; i++) {
            new Thread(()->{
                lock.lock("/lvcai","123");
                for (int j = 0; j < 10; j++) {
                    log.info("加锁成功的线程: "+ Thread.currentThread().getName());
                    num ++;
                }
                lock.unlock();
            },"线程"+i).start();
        }

        //new Thread(()->{
//            lock.lock("/lvcai","123");
//            for (int j = 0; j < 10000; j++) {
//                num ++;
//            }
//            lock.unlock();
        //},"线程1").start();
        while(Thread.activeCount() > 2){}
        System.out.println("最终num的值为: "+num);
    }
}
