package MyLock;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;

public class ReentrantLockDemo {


    private final Sync sync;




    abstract static class Sync extends AbstractQueuedSynchronizer{
        abstract void lock();


        /**
         * 获取独占锁的时候
         * @param acquires
         * @return
         */
        final boolean nonfairTryAcquire(int acquires){
            final Thread current = Thread.currentThread();

            int c = getState();

            if(c == 0){
                /**
                 * cas 更改状态
                 */
                if(compareAndSetState(0,acquires)){
                    //设置独占线程
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            //可冲入锁
            else if(current == getExclusiveOwnerThread()){
                int nextc = c +acquires;
                if(nextc<0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
            }
            return false;
        }



        protected final boolean tryRelease(int releases){
            int c = getState() -releases;
            if(Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;

            /**
             * 完全释放锁的时候 先清理 再设置状态
             */
            if(c == 0){
                free =true;
                setExclusiveOwnerThread(null);
            }

            setState(c);

            return free;
        }

        /**
         * 是否独占线程
         * @return
         */
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition(){
            return  new ConditionObject();
        }

        final Thread getOwner(){
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        //获取冲入次数
        final int getHoldCount(){
            return isHeldExclusively()?getState():0;
        }

        final boolean  isLocked(){
            return getState() != 0;
        }


        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }



    static final class FairSync extends  Sync{
        @Override
        void lock() {
            acquire(1);
        }


        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if(c ==0) {
                /**
                 // 虽然此时此刻锁是可以用的，但是这是公平锁，既然是公平，就得讲究先来后到，
                 // 看看有没有别人在队列中等

                 hasQueuedPredecessors 判断当前阻塞队列有没有节点
                 如有有 当前线程是不是首节点
                 */
                if (!hasQueuedPredecessors() &&
                        // 如果没有线程在等待，那就用CAS尝试一下，成功了就获取到锁了，
                        // 不成功的话，只能说明一个问题，就在刚刚几乎同一时刻有个线程抢先了
                        compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }

            }else if(current == getExclusiveOwnerThread()){
                int nextc = c +acquires;
                if(nextc < 0 )
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }

            // 如果到这里，说明前面的if和else if都没有返回true，说明没有获取到锁
            // 回到上面一个外层调用方法继续看:
            // if (!tryAcquire(arg)
            //        && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            //     selfInterrupt();

            /**
             * state ！=0
             * current ！= getExclusiveOwnerThread
             */
            return false;
        }
    }
    static final class NonfairSync extends  Sync{

        @Override
        void lock() {
            /**
             * 先尝试一次
             */
            if(compareAndSetState(0,1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }


        //非公平锁实现
        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }


    public ReentrantLockDemo(){
        sync = new NonfairSync();
    }

    public ReentrantLockDemo(boolean fair){
        sync = fair?new FairSync():new NonfairSync();
    }


    public void lock(){
        sync.lock();
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }


    /**
     * 尝试获取锁 立即返回
     * 打破公平性
     * @return
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 在一定时间类获取锁
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }


    public void unlock(){
        sync.release(1);
    }


    public Condition newCondition() {
        return sync.newCondition();
    }

    public int getHoldCount() {
        return sync.getHoldCount();
    }



    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }



    public boolean isLocked() {
        return sync.isLocked();
    }


    public final boolean isFair() {
        return sync instanceof FairSync;
    }


    protected Thread getOwner() {
        return sync.getOwner();
    }


    /**
     * 阻塞队列是否有节点排队
     * @return
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }


    /**
     * 是不是排队线程
     * @param thread
     * @return
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }


    /**
     * 获取队列长度
     * @return
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }



    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }


    /**
     * 获取条件队列是否有等待者
     * @param condition
     * @return
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    public int getWaitQueueLength(Condition conditionObject){
        if(conditionObject == null)
            throw new NullPointerException();
        if(!(conditionObject instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject) conditionObject);
    }



    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }


    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }














}
