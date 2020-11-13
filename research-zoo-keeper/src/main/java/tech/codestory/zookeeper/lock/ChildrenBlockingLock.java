package tech.codestory.zookeeper.lock;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * ZooKeeper中基于子节点功能加锁<br>
 * 用于 阻塞锁 ： 加锁失败会等待其他客户端释放锁
 * 
 * @author code story
 * @date 2019/8/19
 */
@Slf4j
public class ChildrenBlockingLock extends ChildrenNodeLock {
    /** 前一个节点被删除的信号 */
    Integer mutex;

    public ChildrenBlockingLock(String address) throws IOException {
        super(address);
        Random random = new SecureRandom();
        mutex = Integer.valueOf(random.nextInt());
    }

    @Override
    protected void processNodeDeleted(WatchedEvent event) {
        synchronized (mutex) {
            // 节点被删除，通知重新检查锁情况
            String deletedNodeName = event.getPath().substring(guidNodeName.length() + 1);
            log.trace("{} 节点被删除，当前监控的是 {} ", deletedNodeName, elementNodeName);
            mutex.notify();
        }
    }

    @Override
    protected void processNodeChildrenChanged(WatchedEvent event) {
        synchronized (mutex) {
            // 子节点有变化，通知重新检查锁情况。有羊群效应
            log.trace("{} 子节点有变化", this.guidNodeName);
            mutex.notify();
        }
    }

    /**
     * 是否加锁成功
     * 
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    protected boolean isLockSuccess() {
        // 是否监控子节点变化，会有羊群效应
        boolean monitorChildrenEvent = false;

        boolean lockSuccess;
        try {
            while (true) { //这里while(true) 的作用,会一直在这里循环,不停的监听前一个节点的变化, 避免了羊群效应
                String prevElementName = getPrevElementName();//获取当前节点的 前一个节点, 为null 说明自己就是第一个节点
                if (prevElementName == null) {
                    log.trace("{} 没有更靠前的子节点，加锁成功", elementNodeName);
                    lockSuccess = true;
                    break;
                } else {
                    log.trace("{} 监控 {} 子节点变化事件", elementNodeName, guidNodeName);
                    // 有更小的节点，说明当前节点没抢到锁，注册前一个节点的监听。
                    if (monitorChildrenEvent) {
                       getZooKeeper().getChildren(this.guidNodeName, true);//获取到这个节点,添加监听
                    } else {
                        log.trace("{} 监控 {} 的事件", elementNodeName, prevElementName);
                        //添加一个监听, 监听前一个节点是否存在
                        getZooKeeper().exists(this.guidNodeName + "/" + prevElementName, true);
                    }
                    synchronized (mutex) {
                        // 最多一秒
                        mutex.wait(1000);
                        if (monitorChildrenEvent) {
                            log.trace("{} 监控的 {} 有子节点变化", elementNodeName, guidNodeName);
                        } else {
                            log.trace("{} 监控的 {} 被删除", elementNodeName, prevElementName);
                        }
                    }
                }
            }
        } catch (KeeperException e) {
            lockSuccess = false;
        } catch (InterruptedException e) {
            lockSuccess = false;
        }
        return lockSuccess;
    }
}
