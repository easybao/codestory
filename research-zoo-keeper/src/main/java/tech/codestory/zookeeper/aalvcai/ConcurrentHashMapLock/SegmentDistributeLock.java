package tech.codestory.zookeeper.aalvcai.ConcurrentHashMapLock;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.C;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2021/6/10 14:33
 */
public class SegmentDistributeLock {
    /**
     * 这里模拟 分段锁 扣减库存, 考虑库存不足,扣减下一个分段锁
     */
    RedissonClient redissonClient;
    RBucket<RedisStock[]> bucket;
    private ThreadLocal<StockRequest> threadLocal = new ThreadLocal<>();
    static volatile RedisStock[] redisStocks;

    public SegmentDistributeLock() throws IOException {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        this.redissonClient = Redisson.create(config);

        redisStocks = new RedisStock[5];
        redisStocks[0] = new RedisStock("pId_stock_00",20);
        redisStocks[1] = new RedisStock("pId_stock_01",20);
        redisStocks[2] = new RedisStock("pId_stock_02",20);
        redisStocks[3] = new RedisStock("pId_stock_03",20);
        redisStocks[4] = new RedisStock("pId_stock_04",20);

        // 库存预热,存到redis中 ,  这里没有采用因为将库存预热存到redis中,取出来的时候,解析异常, 不想花时间解决,所以将库存 变成一个变量
//        bucket = redissonClient.getBucket("pId_stock");
//        bucket.set(redisStocks);

    }
    public RedissonClient getRedissonClient(){
        return this.redissonClient;
    }

    public int getTotalNum(){
        Stream.of(redisStocks).map(e->e.getNum()).count();
    }
    // 这是简单的实现:  如果当前库存扣减失败,就重新获取节点尝试扣减, 这种方式会出现 死循环,一直扣减不了
    public boolean handlerStock_01(StockRequest request) throws IOException, ClassNotFoundException {
        // 这里使用 ThreadLocal代码逻辑和ConcurrentHashMap的分段锁
        RedissonClient redissonClient = getRedissonClient();
        RedisStock[] tab = redisStocks;
        int len = tab.length;
        int i = request.getMemberId().hashCode() % len-1;

        for(RedisStock e = tab[i]; e != null; e = tab[i = nextIndex(i,len)]){
            RLock segmentLock = null;
            try {
                // 2: 对该元素加分段锁
                segmentLock = redissonClient.getLock(e.getStockName());
                segmentLock.lock();
                if (request.getBuyNum() <= e.getNum()) {
                    //扣减库存
                    e.setNum(e.getNum() - request.getBuyNum());
                    // 扣减成功后,跳出循环,返回结果
                    return true;
                }
            } finally {
                segmentLock.unlock();
            }
        }
        return false;
    }

    //改进: 当前分段锁库存不够,会扣减掉当前的库存,然后去锁下一个分段锁,扣减库存
    public boolean handlerStock_02(StockRequest request) throws IOException, ClassNotFoundException {
        // 先做校验: 判断扣减库存 是否比总库存还大,是的话就直接false,  避免无限循环扣减不了
        if(request.getBuyNum() > getTotalNum()){
            return false;
        }

        // 这里使用 ThreadLocal代码逻辑和ConcurrentHashMap的分段锁
        threadLocal.set(request);
        RedissonClient redissonClient = getRedissonClient();
        RedisStock[] tab = redisStocks;
        int len = tab.length;
        int i = request.getMemberId().hashCode() % len;

        for(RedisStock e = tab[i]; e != null; e = tab[i = nextIndex(i,len)]){

            RLock segmentLock = null;
            try {
                // 2: 对该元素加分段锁
                segmentLock = redissonClient.getLock(e.getStockName());
                segmentLock.lock();

                int buyNum = threadLocal.get().getBuyNum();
                if (buyNum <= e.getNum()) {
                    //扣减库存
                    e.setNum(e.getNum() - buyNum);
                    // 扣减成功后,跳出循环,返回结果
                    return true;
                }else{
                    // 扣减掉当前的 分段锁对应的库存,然后对下一个元素加锁
                    threadLocal.get().setBuyNum( buyNum - e.getNum());
                    e.setNum(0);
                }
            } finally {
                // 3: 解锁
                segmentLock.unlock();
            }
        }
        threadLocal.remove();
        return false;
    }

    private static int nextIndex(int i, int len) {
        return ((i + 1 < len) ? i + 1 : 0);
    }

    // 显示redis中的库存
    public void showStocks(){
        for (RedisStock redisStock : redisStocks) {
            System.out.println(redisStock);
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    class RedisStock implements Serializable {
        // 库存字段
        String stockName;
        // 库存数据
        int num;

        @Override
        public String toString() {
            return "RedisStock{" +
                    "stockName='" + stockName + '\'' +
                    ", num=" + num +
                    '}';
        }
    }
}
@Getter
@Setter
@AllArgsConstructor
class StockRequest implements Serializable{
    //会员id
    String memberId;
    //购买数量
    int buyNum;
}

class SegmentDistributeLockTest{
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // 模拟单线程扣减
        SegmentDistributeLock segmentDistributeLock = new SegmentDistributeLock();
        segmentDistributeLock.handlerStock_02(new StockRequest("memberId_001",30));
        segmentDistributeLock.showStocks();
        /**
         * 成功; 结果为:
         * RedisStock{stockName='pId_stock_00', num=0}  扣减了20个
         * RedisStock{stockName='pId_stock_01', num=10} 扣减了10个
         * RedisStock{stockName='pId_stock_02', num=20}
         * RedisStock{stockName='pId_stock_03', num=20}
         * RedisStock{stockName='pId_stock_04', num=20}
         */
    }
}

class ConcurrentTest implements Runnable{
    // 模拟10个线程并发
    private static CountDownLatch countDownLatch = new CountDownLatch(10);
    public static void main(String[] args) {
        // 模拟并发扣减库存
        for (int i = 0; i < 10; i++) {
            new Thread(new ConcurrentTest()).start();
            countDownLatch.countDown();
        }
    }

    @Override
    public void run() {
        try {
            // 在此阻塞,等到计数器归零之后,再同时开始 扣库存
            countDownLatch.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
