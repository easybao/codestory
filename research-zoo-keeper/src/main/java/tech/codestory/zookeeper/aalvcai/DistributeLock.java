package tech.codestory.zookeeper.aalvcai;

/**
 * @version 1.0.0
 * @@menu <p>
 * @date 2020/11/11 16:02
 */
public interface DistributeLock {
    boolean  lock(String nodeName,String nodeValue);//加锁
    boolean  unlock();//解锁
    boolean  exist(String rootNodeName);//锁是否存在
}
