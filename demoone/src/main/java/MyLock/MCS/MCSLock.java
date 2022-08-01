package MyLock.MCS;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;


/**
 * MCS锁解决了简单自旋锁的一个最大痛点：频繁地缓存同步操作会导致繁重的系统总线和内存的流量，从而大大降低了系统整体的性能。
 *
 * 解决这个问题的思路是将自旋操作限制在一个本地变量上，从而在根本上避免了频繁地多CPU之间的缓存同步。但是MCS锁的实现并不简单，需要注意的事项主要有以下几点：
 *
 * MCS锁的节点对象需要有两个状态，next用来维护单向链表的结构，blocked用来表示节点的状态，true表示处于自旋中；false表示加锁成功
 *
 * MCS锁的节点状态blocked的改变是由其前驱节点触发改变的
 *
 * 加锁时会更新链表的末节点并完成链表结构的维护
 *
 * 释放锁的时候由于链表结构建立的时滞(getAndSet原子方法和链表建立整体而言并非原子性)，可能存在多线程的干扰，需要使用忙等待保证链表结构就绪
 *
 */
public class MCSLock {


    /**
     * 节点
     */
    private static class MCSNode{

        private volatile boolean isLock = true;

        private volatile  MCSNode  next;
    }


    private MCSNode node;   //表示当前链表的尾部，每个新加入排队的线程都会被放到这个位置


    /**
     * 共享变量
     */
    private static final AtomicReferenceFieldUpdater UNPDATE = AtomicReferenceFieldUpdater.newUpdater
            (MCSLock.class,
             MCSNode.class,
             "node");


    private ThreadLocal<MCSNode> mcsNodeThreadLocal = new ThreadLocal<>();


    /**
     * 获取当前线程和对应的节点对象(不存在则初始化)
     * 将queue通过getAndSet这一原子操作更新为第一步中得到的节点对象，
     * 返回可能存在的前驱节点，如果前驱存在跳转到Step 3；不存在跳转到Step 4
     *
     *
     * 建立单向链表关系，由前驱节点指向当前节点；
     * 当前线程开始在当前节点对象的blocked字段上自旋等待(等待前驱节点改变其blocked的状态)
     * 没有前驱节点表示此时并没有除当前线程外的线程拥有锁，因此可以直接改变节点的blocked为false，lock方法执行完毕表示加锁成功
     */
    public void lock(){
        MCSNode mcsNode = mcsNodeThreadLocal.get();

        if(mcsNode==null){
            mcsNode = new MCSNode();
        }

        /**
         * 非原子性    getAndSet set进去要排队  返回之前的一个 原子性
         */
        MCSNode predecessor = (MCSNode)UNPDATE.getAndSet(this, mcsNode); // step 1


        // 不为null 或者 前节点islock 为false
        if(predecessor!=null){

            // 形成链表结构(单向)
            predecessor.next = mcsNode; // step 2

            //自旋等待

            while(mcsNode.isLock){

            }
        }else{
            // 只有一个线程在使用锁，没有前驱来通知它，所以得自己标记自己为非阻塞 - 表示已经加锁成功
            mcsNode.isLock=false;
        }



    }


    /**
     * 1 获取当前线程和对应的节点对象；如果节点不存在或者节点状态为等待的话直接返回，因为只有拥有锁的线程才有资格进行释放锁的操作
     * 2 清空当前线程对应的节点信息
     * 3 判断当前节点是否拥有后继节点，如果没有的话跳转到Step 4；没有的话跳转到Step 5
     *
     *
     * 4 利用原子更新器的CAS操作尝试将queue设置为null。设置成功的话表示锁释放成功，unlock方法执行完毕返回；
     * 设置失败的话表示Step 3和Step 4的CAS的操作之间有别的线程来捣乱了，queue此时并非指向当前节点，
     * 因此需要忙等待确保链表结构就绪(参考代码注释：lock操作的getAndSet操作和链表建立并非是原子性的)
     *
     *
     * 5 此时当前节点的后继节点已经就绪了，所以可以改变后继节点的blocked状态，另在其上等待的线程退出自旋。最后还会更新当前节点的next指向为null辅助垃圾回收
     */
    public void unlock(){

        MCSNode mcsNode = mcsNodeThreadLocal.get();

        //没有持有锁
        if(mcsNode==null||mcsNode.isLock){
            return;
        }

        /**
         * 如果等于null 说明没有后续节点
         * 但同时会有线程添加后续节点
         */
        if(mcsNode.next==null
                //cas 设为null
                // 新加入的node 会把当前node的next 指向新增node
                &&!UNPDATE.compareAndSet(this,mcsNode,null)){
            //设置失败  说明有更新
            // 没有后继节点的情况，将queue置为空
            // 如果CAS操作失败了表示突然有节点排在自己后面了，可能还不知道是谁，下面是等待后续者
            // 这里之所以要忙等是因为上述的lock操作中step 1执行完后，step 2可能还没执行完

            //节点不为null
            while(mcsNode.next == null){

            }

        }


        //执行唤醒下个节点

        if(mcsNode.next!=null){

            mcsNode.next.isLock=false;

            // 将当前节点从链表中断开，方便对当前节点进行GC
            mcsNode.next = null;
        }


        //清理节点
        mcsNodeThreadLocal.remove();

    }
}
