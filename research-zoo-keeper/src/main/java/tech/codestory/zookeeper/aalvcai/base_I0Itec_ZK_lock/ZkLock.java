package tech.codestory.zookeeper.aalvcai.base_I0Itec_ZK_lock;

/**
 * @Description: zkTest
 * @Author: 敖丙
 * @date: 2020-04-06
 **/
public class ZkLock {
    static int inventory = 10;
    private static final int NUM = 5;

    //private static Zk zk = new Zk();
    private static I0Itec_DistributeLock zk = new I0Itec_DistributeLock();

    public static void main(String[] args) {
        /**
         * 这种写法,不行 并发下有异常
         */
        try {
            for (int i = 0; i < NUM; i++) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            zk.lock();
                            Thread.sleep(1000);
                            if (inventory > 0) {
                                inventory--;
                            }
                            System.out.println(inventory);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            zk.unlock();
                            System.out.println("释放锁");
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
