package tech.codestory.zookeeper.aalvcai.redis_db_data_same;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 *
 *  写一个 解决 redis 和 db 数据一致性的demo
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/12 11:11
 */
@Slf4j
public class RedisTest {
    /**
     *  这里 jedis  和redisson 联合使用
     *  数据一致性解决方案, 和 缓存穿透, 缓存击穿, 缓存雪崩  的demo
     * 延时双删,  先删缓存,之后更新数据库,在之后睡1秒(目的是确保库已更新完毕),再删缓存
     *
     * 缓存穿透: 大量请求来请求一个不存在的数据,缓存,数据库都不存在,,解决方案: 查询数据库如果为空就设置缓存为null,并设置过期时间,不超过5分钟
     *
     * 缓存击穿: key对应的数据,库中存在,缓存失效,,导致大量请求都来请求数据库,解决方案: 如果查到缓存为空, 不立即查库,  此时设置一个互斥锁,只有加锁成功的线程才去db查数据, 设置失败的线程 重试
     *
     * 缓存雪崩: 大量key集中同一时间过期, 这种情况下,加锁/或者队列的方式保证了不会有大量线程一次性对库读写,减轻了库的压力,但是并没有提升系统的吞吐量,  解决方案: 将缓存失效时间分散开,1-5分钟随机值
     */
    static Jedis jedis;
    static RedissonClient redissonClient;
    static Map<String,User> map = new HashMap<>();//数据库

    public RedisTest(String host, int port) {
        //连接客户端
        this.jedis = new Jedis(host,port);

        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        redissonClient = Redisson.create(config);
        System.out.println("连接redis成功");
    }

    public static boolean keyIsExist(String key){
        return jedis.exists(key);
    }

    public void delKey(String key){
        jedis.del(key);
    }

    public Object readData(String key){
        if (! keyIsExist(key)) {
            //key 不存在, 此时不立即查询库,而是设置一个互斥锁
            Long setnx = jedis.setnx("OWEN_LOCK", "OWEN_LOCK");//如果key不存在就返回0, key已经存在返回1
            log.info("setnx方法 : "+ Thread.currentThread().getName()+"---"+setnx);
            if(setnx == 0){
                //设置成功, 读取数据库
                User user = map.get(key);
                if(user == null){
                    jedis.set(key,null);
                    jedis.expire(key,new Random().nextInt(5));//空key 设置0-5秒过期
                }else{
                    //设置缓存
                    jedis.set(key,user.toString());
                }
                return user;
            }
            System.out.println("没有设置 互斥锁成功");
        }
        return jedis.get(key);
    }

    /**
     * 写  延时双删
     * @param user
     */
    public void writeData(User user){
        String cacheKey = "key_"+user.getId();
        // 1: 先删缓存
        if(keyIsExist(cacheKey)){
            delKey(cacheKey);
        }
        // 2: 更新数据库
        map.put(cacheKey,user);//耗时小于500毫秒
        // 3: 睡1秒(这里睡一秒是保证 库更新完)
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 4: 再删缓存
        if(keyIsExist(cacheKey)){
            delKey(cacheKey);
        }
    }

    public void getMap(){
        if(map.isEmpty()){
            return;
        }
        for (Map.Entry<String, User> entry : map.entrySet()) {
            System.out.println(entry.getKey() + "---" + entry.getValue());
        }
    }
}


class User {
    int  id;
    String name;

    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

class Test{
    public static void main(String[] args) {
        RedisTest redis = new RedisTest("127.0.0.1", 6379);

        //多线程写数据
        for (int i = 0; i < 10; i++) {
            int j = i;
            new Thread(()->{
                redis.writeData(new User(j,"qiqi"));
            },"线程"+i).start();
        }

        redis.getMap();
    }
}

