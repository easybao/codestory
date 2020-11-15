package tech.codestory.zookeeper.aalvcai.base_curator_ZK_lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;

public class CuratorZkLock {

    CuratorFramework client;
    {
        //设置重试策略, 重试5次
        ExponentialBackoffRetry retry = new ExponentialBackoffRetry(1000,5);
        client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", retry);
        client.start();
    }

    public CuratorFramework getClient(){
        return this.client;
    }

    public void unlock(){
        CloseableUtils.closeQuietly(this.client);//释放锁
    }



    public static void main(String[] args){

        CuratorZkLock curatorZkLock = new CuratorZkLock();

    }
}
