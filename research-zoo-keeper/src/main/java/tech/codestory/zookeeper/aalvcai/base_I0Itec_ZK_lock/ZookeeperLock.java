    package tech.codestory.zookeeper.aalvcai.base_I0Itec_ZK_lock;

    import lombok.extern.slf4j.Slf4j;
    import org.I0Itec.zkclient.IZkDataListener;
    import org.I0Itec.zkclient.ZkClient;

    import java.util.List;
    import java.util.concurrent.TimeUnit;
    import java.util.stream.Collectors;

    /**
     * @author 邱润泽
     **/
    @Slf4j
    public class ZookeeperLock {
        private String server = "127.0.0.1:2181";
        private ZkClient zkClient;
        private static final String rootPath = "/qiurunze-lock1";

        public ZookeeperLock() {
            zkClient = new ZkClient(server, 5000, 20000);
            buildRoot();
        }

        // 构建根节点
        public void buildRoot() {
            if (!zkClient.exists(rootPath)) {
                zkClient.createPersistent(rootPath);
            }
        }
        // 获取锁
        public Lock lock(String lockId, long timeout) {
            // 创建临时节点
            Lock lockNode = createLockNode(lockId);
            lockNode = tryActiveLock(lockNode);// 尝试激活锁
            if (!lockNode.isActive()) {
                try {
                    synchronized (lockNode) {
                        lockNode.wait(timeout); // 线程锁住
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (!lockNode.isActive()) {
                throw new RuntimeException(" lock  timeout");
            }
            return lockNode;
        }

        // 释放锁
        public void unlock(Lock lock) {
            if (lock.isActive()) {
                zkClient.delete(lock.getPath());
            }
        }

        // 尝试激活锁
        private Lock tryActiveLock(Lock lockNode) {

            // 获取根节点下面所有的子节点
            List<String> list = zkClient.getChildren(rootPath)
                    .stream()
                    .sorted()
                    .map(p -> rootPath + "/" + p)
                    .collect(Collectors.toList());      // 判断当前是否为最小节点
            log.info("Thread: {}, list : {}",Thread.currentThread().getName(),list);

            String firstNodePath = list.get(0);
            log.info("Thread: {}, firstNodePath: {}",Thread.currentThread().getName(),firstNodePath);
            // 最小节点是不是当前节点
            if (firstNodePath.equals(lockNode.getPath())) {
                lockNode.setActive(true);
            } else {
                String upNodePath = list.get(list.indexOf(lockNode.getPath()) - 1);
                log.info("Thread: {},监听的节点是: {}",Thread.currentThread().getName(),upNodePath);
                zkClient.subscribeDataChanges(upNodePath, new IZkDataListener() {
                    @Override
                    public void handleDataChange(String dataPath, Object data) throws Exception {
                    }
                    @Override
                    public void handleDataDeleted(String dataPath) throws Exception {
                        // 事件处理 与心跳 在同一个线程，如果Debug时占用太多时间，将导致本节点被删除，从而影响锁逻辑。
                        System.out.println("节点删除:" + dataPath);
                         Lock lock = tryActiveLock(lockNode);
                        synchronized (lockNode) {
                            if (lock.isActive()) {
                                lockNode.notify(); // 释放了
                            }
                        }
                        zkClient.unsubscribeDataChanges(upNodePath, this);
                    }
                });
            }
            return lockNode;
        }


        public Lock createLockNode(String lockId) {
            String nodePath = zkClient.createEphemeralSequential(rootPath + "/" + lockId, "w");
            return new Lock(lockId, nodePath);
        }
    }


    class Test01{
        static volatile int num = 0;
        static ZookeeperLock zookeeperLock = new ZookeeperLock();
        public static void main(String[] args){
            for (int i = 0; i < 10; i++) {
                new Thread(()->{
                    try {
                        Lock zkLock = zookeeperLock.lock("lvcai", 5000 );
                        TimeUnit.MILLISECONDS.sleep(100);
                        for (int j = 0; j < 10; j++) {
                            num++;
                        }
                        System.out.println( "num的值是 : "+ num );
                        zookeeperLock.unlock(zkLock);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },"线程"+i).start();
            }
        }
    }


    class Lock {
        private String lockId;
        private String path;
        private boolean active;
        public Lock(String lockId, String path) {
            this.lockId = lockId;
            this.path = path;
        }

        public Lock() {
        }

        public String getLockId() {
            return lockId;
        }

        public void setLockId(String lockId) {
            this.lockId = lockId;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }