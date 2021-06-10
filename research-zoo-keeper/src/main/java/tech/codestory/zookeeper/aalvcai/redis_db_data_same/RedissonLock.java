package tech.codestory.zookeeper.aalvcai.redis_db_data_same;

import org.redisson.Redisson;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2021/6/10 13:42
 */
public class RedissonLock {
    public static void main(String[] args) {
        /**
         * �ֶ����ۼ���� ��Ƚ�����д��
         * �ۼ����:  https://blog.csdn.net/weixin_35657239/article/details/113495415
         */
        try {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://127.0.0.1:6379");
            config.useSingleServer().setPassword("test");
            RedissonClient finalRedisson = Redisson.create(config);
            //��������
            int requireQty = 9;
            for (int i = 0; i < 30; i++) {
                Thread t = new Thread(() -> {
                    try {
                        RLock rLock = finalRedisson.getLock("myLock");
                        System.out.println(Thread.currentThread().getName() + "��ʼ");
                        // �������
                        RAtomicLong stockQty = finalRedisson.getAtomicLong("stockQty");
                        // ռ�п������
                        RAtomicLong stockOccupy = finalRedisson.getAtomicLong("stockOccupy");
                        rLock.lock();
                        long l1 = stockQty.get();
                        long l2 = stockOccupy.get();
                        System.out.println("l1: " + l1 + "  l2: " + l2);
                        long l = l1 - l2;
                        System.out.println(Thread.currentThread().getName() + "�����");
                        System.out.println(Thread.currentThread().getName() + "do something");
                        if (l >= requireQty) {
                            stockOccupy.set(stockOccupy.get() + requireQty);
                        }
                        rLock.unlock();
                        //�����������ۼ����
                        Thread.sleep(200);
                        if (l >= requireQty) {
                            System.out.println(Thread.currentThread().getName() + "done�����ʣ�£�" + l);
                        } else {
                            System.out.println(Thread.currentThread().getName() + "��治�㣬���ʣ�£�" + l);
                        }
                        // System.out.println(Thread.currentThread().getName() + "׼���ͷ���");
                        System.out.println(Thread.currentThread().getName() + "����");
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                });
                t.start();
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } finally {

        }
    }

}
