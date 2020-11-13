package tech.codestory.zookeeper.aalvcai;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/10 11:54
 */
public class ZookeeperBaseService implements Watcher {
    ZooKeeper zooKeeper;
    CountDownLatch countDownLatch = new CountDownLatch(1);
    List<WatchedEvent> eventList = new ArrayList<>();
    Integer INTEGER_LOCK_SIGN = Integer.valueOf(1);


    public ZookeeperBaseService(String address){
        try {
            this.zooKeeper = new ZooKeeper(address,5000,this);
            countDownLatch.await();//执行成功,才继续往下执行
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听实现
     * @param event
     */
    @Override
    public void process(WatchedEvent event) {
        if(event.getType() == Event.EventType.None){//无连接
            if(event.getState().equals(Event.KeeperState.SyncConnected)){//如果状态是连接状态,连接成功
                countDownLatch.countDown();//计数器减一
            }
        }else{
            eventList.add(event);
            if(event.getType() == Event.EventType.NodeCreated){
                processNodeCreated(event);//监听节点增加的处理
            }else if(event.getType() == Event.EventType.NodeDeleted){
                processNodeDeleted(event);//监听节点删除的处理
            }else if(event.getType() == Event.EventType.NodeDataChanged){
                processNodeDataChanged(event);//监听节点数据修改的处理
            }else if(event.getType() == Event.EventType.NodeChildrenChanged){
                processNodeChildrenChanged(event);//监听节点的子节点数据修改的处理
            }
        }
    }

    public ZooKeeper getZooKeeper(){
        return zooKeeper;
    }

    public List<WatchedEvent> getEventList(){
        return this.eventList;
    }

    public String createRootNode(String rootNodeName){
        CreateMode mode = CreateMode.PERSISTENT;
        return createRootNode(rootNodeName,mode);
    }

    public String createRootNode(String rootNodeName,CreateMode mode){
        synchronized (INTEGER_LOCK_SIGN){
            try {
                Stat stat = getZooKeeper().exists(rootNodeName, false);
                if(stat == null){
                    rootNodeName = getZooKeeper().create(rootNodeName, "0".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
                }
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return rootNodeName;
    }


    public void processNodeCreated(WatchedEvent event) {
    }
    public void processNodeDeleted(WatchedEvent event) {
    }
    public void processNodeDataChanged(WatchedEvent event) {
    }
    public void processNodeChildrenChanged(WatchedEvent event) {
    }


}
