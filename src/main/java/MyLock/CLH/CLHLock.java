package MyLock.CLH;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.UnaryOperator;



/**
 * CLH也是一种基于单向链表(隐式创建)的高性能、公平的自旋锁，申请加锁的线程只需要在其前驱节点的本地变量上自旋，
 * 从而极大地减少了不必要的处理器缓存同步的次数，降低了总线和内存的开销。
 */
public class CLHLock {

    /**
     * CLH锁节点状态 - 每个希望获取锁的线程都被封装为一个节点对象
     */
    private static class CLHNode {

        /**
         * 默认状态为true - 即处于等待状态或者加锁成功(换言之，即此节点处于有效的一种状态)
         */
        volatile boolean active = true;

    }




    /**
     * 隐式链表最末等待节点
     */
    private volatile CLHNode  tail = null ;

    private volatile  String  name = null;

    /**
     * 线程对应的CLH节点
     */
    private ThreadLocal<CLHNode>    clhNodeThreadLocal = new ThreadLocal<>();



    /**
     * 原子更新器
     */
    private static final AtomicReferenceFieldUpdater UPDATER           = AtomicReferenceFieldUpdater
            .newUpdater(
                    CLHLock.class,    //CLHLock 类的
                    CLHNode.class,    //CLHNode 属性
                    "tail");  // tail 名

    private static final AtomicReferenceFieldUpdater Name  =  AtomicReferenceFieldUpdater.newUpdater(
            CLHLock.class,
            String.class,
            "name");



    public void demoName(){

        Object andUpdate = Name.getAndUpdate(this, new UnaryOperator<String>() {
            @Override
            public String apply(String o) {
                return o + ":1";
            }
        });
        System.out.println("旧值："+andUpdate);

        Object new_new = Name.getAndSet(this, "new new");

        System.out.println("旧值2："+new_new);

        Object o = Name.get(this);

        System.out.println("当前值："+o);

    }
    /**
     * CLH 加锁
     */

    public void lock(){
        CLHNode clhNode = clhNodeThreadLocal.get();



        if(clhNode == null ){
            clhNode = new CLHNode();
            clhNodeThreadLocal.set(clhNode);
        }
        // 通过这个操作完成隐式链表的维护，后继节点只需要在前驱节点的locked状态上自旋

        /**
         * tail 节点设置为 当前clhNode
         */
        CLHNode predecessor  = (CLHNode) UPDATER.getAndSet(this, clhNode);


        /**
         * 只在一个本地变量上自旋
         */
        if (predecessor != null) {
            // 自旋等待前驱节点状态变更 - unlock中进行变更
            while (predecessor.active) {

            }
        }

        // 没有前驱节点表示可以直接获取到锁，由于默认获取锁状态为true，此时可以什么操作都不执行

        // 能够执行到这里表示已经成功获取到了锁

        System.out.println(Thread.currentThread().getName()+":获取到锁");
    }


    /**
     * CLH释放锁
     */

    public void unlock(){
        CLHNode cNode = clhNodeThreadLocal.get();

        // 只有持有锁的线程才能够释放
        if (cNode == null || !cNode.active) {
            return;
        }


        //从映射关系中移除当前线程对应的节点
        clhNodeThreadLocal.remove();


        // 尝试将tail从currentThread变更为null，因此当tail不为currentThread时表示还有线程在等待加锁
        if (!UPDATER.compareAndSet(this, cNode, null)) {
            // 不仅只有当前线程，还有后续节点线程的情况 - 将当前线程的锁状态置为false，因此其后继节点的lock自旋操作可以退出
            cNode.active = false;
        }
    }


    public static void main(String[] args) {

        final CLHLock lock = new CLHLock();

        lock.demoName();


       /* for (int i = 1; i <= 10; i++) {
            new Thread(generateTask(lock, String.valueOf(i))).start();
        }*/
    }

    private static Runnable generateTask(final CLHLock lock, final String taskId) {
        return () -> {

            System.out.println(Thread.currentThread().getName()+": 执行任务" );
            lock.lock();

            try {
                Thread.sleep(3000);
            } catch (Exception e) {

            }

            System.out.println(String.format("Thread %s Completed", taskId));
            lock.unlock();
        };
    }





}
