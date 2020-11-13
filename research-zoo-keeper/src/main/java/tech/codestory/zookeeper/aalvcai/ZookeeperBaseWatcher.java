package tech.codestory.zookeeper.aalvcai;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.data.Stat;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/10 12:25
 */
@Slf4j
public class ZookeeperBaseWatcher extends ZookeeperBaseService implements Runnable{

    String znode;
    Integer WATCHER_LOCK_NODE_DELETE = Integer.valueOf(-1);//节点删除的锁

    public ZookeeperBaseWatcher(String address,String znode) {
        super(address);//连接客户端
        this.znode = znode;//节点完整名
        readNodeData();
    }

    private void readNodeData() {
        try {
            Stat stat = new Stat();
            byte[] data = getZooKeeper().getData(znode, true, stat);
            String readData = new String(data);
            log.info("读取到的数据是:{},version是{}",readData,stat.getVersion());
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        try {
            synchronized (WATCHER_LOCK_NODE_DELETE) {
                WATCHER_LOCK_NODE_DELETE.wait();
                log.info("{} 被删除，退出", znode);
            }
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
        }
    }


    public void processNodeCreated(WatchedEvent event) {
        String path = event.getPath();
        if(path != null && path.equals(znode)){
            log.info("节点创建:{}",znode);
            readNodeData();
        }
    }
    public void processNodeDeleted(WatchedEvent event) {
        String path = event.getPath();
        if(path != null && path.equals(znode)){
            log.info("节点被删除:{},通知等待线程",znode);

            synchronized (WATCHER_LOCK_NODE_DELETE){
                WATCHER_LOCK_NODE_DELETE.notify();
            }
        }
    }
    public void processNodeDataChanged(WatchedEvent event) {
        String path = event.getPath();
        if(path != null && path.equals(znode)){
            log.info("节点数据被修改:{}",znode);
            readNodeData();
        }
    }
}
