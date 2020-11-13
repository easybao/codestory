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
 *  дһ�� ��� redis �� db ����һ���Ե�demo
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/12 11:11
 */
@Slf4j
public class RedisTest {
    /**
     *  ���� jedis  ��redisson ����ʹ��
     *  ����һ���Խ������, �� ���洩͸, �������, ����ѩ��  ��demo
     * ��ʱ˫ɾ,  ��ɾ����,֮��������ݿ�,��֮��˯1��(Ŀ����ȷ�����Ѹ������),��ɾ����
     *
     * ���洩͸: ��������������һ�������ڵ�����,����,���ݿⶼ������,,�������: ��ѯ���ݿ����Ϊ�վ����û���Ϊnull,�����ù���ʱ��,������5����
     *
     * �������: key��Ӧ������,���д���,����ʧЧ,,���´����������������ݿ�,�������: ����鵽����Ϊ��, ���������,  ��ʱ����һ��������,ֻ�м����ɹ����̲߳�ȥdb������, ����ʧ�ܵ��߳� ����
     *
     * ����ѩ��: ����key����ͬһʱ�����, ���������,����/���߶��еķ�ʽ��֤�˲����д����߳�һ���ԶԿ��д,�����˿��ѹ��,���ǲ�û������ϵͳ��������,  �������: ������ʧЧʱ���ɢ��,1-5�������ֵ
     */
    static Jedis jedis;
    static RedissonClient redissonClient;
    static Map<String,User> map = new HashMap<>();//���ݿ�

    public RedisTest(String host, int port) {
        //���ӿͻ���
        this.jedis = new Jedis(host,port);

        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        redissonClient = Redisson.create(config);
        System.out.println("����redis�ɹ�");
    }

    public static boolean keyIsExist(String key){
        return jedis.exists(key);
    }

    public void delKey(String key){
        jedis.del(key);
    }

    public Object readData(String key){
        if (! keyIsExist(key)) {
            //key ������, ��ʱ��������ѯ��,��������һ��������
            Long setnx = jedis.setnx("OWEN_LOCK", "OWEN_LOCK");//���key�����ھͷ���0, key�Ѿ����ڷ���1
            log.info("setnx���� : "+ Thread.currentThread().getName()+"---"+setnx);
            if(setnx == 0){
                //���óɹ�, ��ȡ���ݿ�
                User user = map.get(key);
                if(user == null){
                    jedis.set(key,null);
                    jedis.expire(key,new Random().nextInt(5));//��key ����0-5�����
                }else{
                    //���û���
                    jedis.set(key,user.toString());
                }
                return user;
            }
            System.out.println("û������ �������ɹ�");
        }
        return jedis.get(key);
    }

    /**
     * д  ��ʱ˫ɾ
     * @param user
     */
    public void writeData(User user){
        String cacheKey = "key_"+user.getId();
        // 1: ��ɾ����
        if(keyIsExist(cacheKey)){
            delKey(cacheKey);
        }
        // 2: �������ݿ�
        map.put(cacheKey,user);//��ʱС��500����
        // 3: ˯1��(����˯һ���Ǳ�֤ �������)
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 4: ��ɾ����
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

        //���߳�д����
        for (int i = 0; i < 10; i++) {
            int j = i;
            new Thread(()->{
                redis.writeData(new User(j,"qiqi"));
            },"�߳�"+i).start();
        }

        redis.getMap();
    }
}

