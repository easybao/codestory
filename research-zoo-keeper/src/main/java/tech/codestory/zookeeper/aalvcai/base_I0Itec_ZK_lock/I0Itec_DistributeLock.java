package tech.codestory.zookeeper.aalvcai.base_I0Itec_ZK_lock;

import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/13 16:00
 */
@Slf4j
public class I0Itec_DistributeLock implements Lock {
    ZkClient zkClient ;
    CountDownLatch countDownLatch = new CountDownLatch(1);
    String PersistentNode = "/PersistentLock";
    String currentNodeAllName ; //创建子节点 顺序节点的完整名
    String beforeNodeName; //前一子节点的 完整名

    ThreadLocal<String> currentNodeAllNameThreadLocal = new ThreadLocal<>();

    public I0Itec_DistributeLock() {
        this.zkClient = new ZkClient("127.0.0.1:2181");
        //默认创建持久节点
        if(! this.zkClient.exists(PersistentNode)){
            this.zkClient.createPersistent(PersistentNode);
        }
    }

    public void close(){
        this.zkClient.close();
    }
    @Override
    public void lock() {
        if(tryLock()){
            System.out.println("加锁成功");
        }else{
            //监听前一顺序节点是否删除
            waitPreNodeDelLock();
            //加锁
            lock();
        }
    }

    public String getCurrentNodeNameFromThreadLocal(){
        currentNodeAllName = this.currentNodeAllNameThreadLocal.get();
        System.out.println("ThreadLocal中的节点名字是: " + currentNodeAllName);
        this.currentNodeAllNameThreadLocal.remove();
        return currentNodeAllName;
    }
    @Override
    //public synchronized boolean tryLock() { //注意 这个方法一定要 同步, synchronized, 否则会出现同时创建多个子节点,最后一个子节点和 list中第一个子节点永远不相同,导致监听list中的倒数第二个子节点,但是这个节点永远不会删除,从而一直阻塞在这里,
    public boolean tryLock() { //注意 这个方法一定要 同步, synchronized, 否则会出现同时创建多个子节点,最后一个子节点和 list中第一个子节点永远不相同,导致监听list中的倒数第二个子节点,但是这个节点永远不会删除,从而一直阻塞在这里,
        if(StringUtil.isNullOrEmpty(currentNodeAllName)){
            //1: 创建持久节点下临时顺序节点
            String currentNodeAllName = this.zkClient.createEphemeralSequential(PersistentNode+"/children","lock_sign");
            // 添加到ThreadLocal中,并发时, ThreadLocal为每一个线程 保存该线程创建的临时顺序节点,记得在使用完ThreadLocal后,要删除, 避免内存泄漏
            currentNodeAllNameThreadLocal.set(currentNodeAllName);
            log.info("currentNodeAllName:  {}",currentNodeAllName);
        }
        //2: 获取持久节点下的所有孩子节点
        List<String> children = this.zkClient.getChildren(PersistentNode);
        Collections.sort(children, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                //截取后10位进行 从小到大排序
                return o1.substring(o1.length() - 10).compareTo(o2.substring(o2.length() - 10));
            }
        });
        log.info("children:  {}",children.toString());
        if (getCurrentNodeNameFromThreadLocal().equals(PersistentNode+"/"+children.get(0))) {
            //是第一个节点,
            System.out.println("加锁成功,对应的节点是: " + currentNodeAllName);
            return true;
        }else{
            int index = Collections.binarySearch(children, currentNodeAllName.substring(PersistentNode.length() + 1));
            //找到 当前节点的前一个节点, 监听这个节点是否存在
            //beforeNodeName 保存的是节点的完整名字, 否则会导致,监听子节点删除,监听不到,删除子节点也删除不了,进而出现栈溢出
            beforeNodeName = PersistentNode + "/"+ children.get(index - 1);
            log.info("beforeNodeName:  {}",beforeNodeName);
        }
        return false;
    }


    private void waitPreNodeDelLock() {
        IZkDataListener listener = new IZkDataListener() {
            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {
            }
            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                System.out.println(dataPath + "  节点被删除了 ");
                countDownLatch.countDown();//计数器减一
            }
        };
        //添加监听
        this.zkClient.subscribeDataChanges(beforeNodeName,listener);
        if (this.zkClient.exists(beforeNodeName)){
            try {
                // 在这里阻塞, 知道countDownLatch 等于0 ,才继续往下执行
                System.out.println("加锁失败,在此等待");
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //去除监听
        this.zkClient.unsubscribeDataChanges(beforeNodeName,listener);
    }
    @Override
    public void unlock() {
        this.zkClient.delete(currentNodeAllName);
        System.out.println(currentNodeAllName + "删除掉,,,解锁成功");
    }



    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }


    @Override
    public void lockInterruptibly() throws InterruptedException {
    }
}
