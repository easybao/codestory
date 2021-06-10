package tech.codestory.zookeeper.aalvcai;

import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/13 11:24
 */
public class RedissonDistributeLock {

    RedissonClient redissonClient;

    public RedissonDistributeLock() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        this.redissonClient = Redisson.create(config);
    }

    public RedissonClient getRedissonClient(){
        return this.redissonClient;
    }

    static int num = 0;
    public static void main(String[] args) {

        /**
         * ��� Ҳû����� ��bug,ԭ������־��û�е���
         */
        //���ӿͻ���. ����ȡclient
        RedissonClient redissonClient = new RedissonDistributeLock().getRedissonClient();
        RLock lock = redissonClient.getLock("DISTRIBUTE_LOCK");
        for (int i = 0; i < 100; i++) {
            new Thread(()->{
                lock.lock(1,TimeUnit.SECONDS);
                for (int j = 0; j < 100000; j++) {
                    num++;
                    System.out.println(Thread.currentThread().getName()+"���ӵ�����Ϊ: "+num);
                }
                lock.unlock();
            },"�߳���:"+i).start();
        }
        while (Thread.activeCount() > 2){}
        System.out.println("num ������ֵ��: " + num);
    }

    private static void handlerMethod() {
        for (int i = 0; i < 100; i++) {
            num++;
        }
    }
}
