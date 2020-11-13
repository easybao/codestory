package tech.codestory.zookeeper.aalvcai;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/11 16:01
 */
@Slf4j
public class DistributeLockImpl extends ZookeeperBaseService implements DistributeLock{
    String rootNodeName ;
    String currentChildrenNodeName;//当前的孩子节点名字
    String fullTemporaryOrderName;//孩子节点的完整名字
    /** 前一个节点被删除的信号 */
    Integer mutex;
    public DistributeLockImpl(String address) {
        super(address);//连接客户端
    }

    /**
     *
     * @param nodeName  节点名
     * @param nodeValue 该节点唯一标识
     * @return
     */
    @Override
    public boolean lock(String nodeName,String nodeValue) {


        /**
         *
         *
         *  这个分布式锁 写失败
         */
        rootNodeName = nodeName;
        String renNodeName;//当前孩子节点名
        boolean result = false;
        try {
            //1: 创建根节点
            Stat stat = getZooKeeper().exists(nodeName, false);
            if(stat == null){
                nodeName = createRootNode(nodeName);
                log.info("根节点是:{}",nodeName);
            }

            //2: 创建持久节点下的临时顺序节点
            String fullChildrenNodeName = nodeName + "/" + "element";
            byte[] bytes = nodeValue == null ? "0".getBytes() : nodeValue.getBytes();
            String temporaryOrderName = getZooKeeper()
                    .create(fullChildrenNodeName, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            log.info("生成的临时顺序节点的 完整名字为:{}",temporaryOrderName);
            this.fullTemporaryOrderName = temporaryOrderName;
            this.currentChildrenNodeName = temporaryOrderName.substring(nodeName.length() +1);
            result = isLockSuccess();
            log.info(temporaryOrderName+ "这个节点加锁成功");
            //3: 判断是否加锁成功
        } catch (KeeperException e) {
            log.error(e.getLocalizedMessage());
        } catch (InterruptedException e) {
            log.error(e.getLocalizedMessage());
        }
        return result;
    }

    private boolean isLockSuccess() {
        // 是否监控子节点变化，会有羊群效应
        boolean monitorChildrenEvent = false;
        boolean result = false;
        while (true) { //这里添加 while(true) 是为了避免羊群效应
            try {
                //1: 获取当前节点的前一个节点
                String beforeNodeName = getBeforeNode();
                if(beforeNodeName == null){
                    //说明该节点就是第一个节点,加锁成功
                    result = true;
                    break;
                }else{
                    log.trace("{} 监控 {} 子节点变化事件", rootNodeName, beforeNodeName);
                    // 有更小的节点，说明当前节点没抢到锁，注册前一个节点的监听。
                    if (monitorChildrenEvent) {
                        getZooKeeper().getChildren(this.rootNodeName, true);//获取到这个节点,添加监听
                    } else {
                        //添加一个监听, 监听前一个节点是否存在
                        getZooKeeper().exists(this.rootNodeName + "/" + beforeNodeName, true);
                    }
                    synchronized (mutex) {
                        // 最多一秒
                        mutex.wait(1000);
                        if (monitorChildrenEvent) {
                            log.trace("{} 监控的 {} 有子节点变化", currentChildrenNodeName, rootNodeName);
                        } else {
                            log.trace("{} 监控的 {} 被删除", currentChildrenNodeName, beforeNodeName);
                        }
                    }
                }
            } catch (KeeperException e) {
                log.error(e.getLocalizedMessage());
            } catch (InterruptedException e) {
                log.error(e.getLocalizedMessage());
            }

        }
        return result ;
    }

    private String getBeforeNode() {
        String resultBeforeNodeName = null;
        try {
            //1.1: 获取所有的孩子节点
            List<String> children = getZooKeeper().getChildren(rootNodeName, false);
            //1.2: 从小到大排序
            Collections.sort(children,new StringComparaor());
            //获取到当前节点的前一个节点
            for (String child : children) {
                if(child.equals(this.currentChildrenNodeName)){
                    break;
                }
                resultBeforeNodeName = child;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultBeforeNodeName;
    }

    @Override
    public boolean unlock() {
        boolean result = true;
        //删除这个创建的子节点
        try {
            getZooKeeper().delete(this.fullTemporaryOrderName,0);
            log.info("解锁成功");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            result = false;
        }
        return result;
    }

    @Override
    public boolean exist(String rootNodeName) {
        boolean result = false;
        Stat stat = new Stat();
        try {
            getZooKeeper().getData(rootNodeName, false, stat);
            if(stat.getNumChildren() > 0){
                result = true;//节点存在,且孩子节点也存在
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
            result = false;
        }
        return result;
    }

    private class StringComparaor implements Comparator<String>{
        @Override
        public int compare(String o1, String o2) {
            //截取后十位进行比较
            return o1.substring(o1.length() - 10)
                    .compareTo(o2.substring(o2.length() - 10));
        }
    }

}
