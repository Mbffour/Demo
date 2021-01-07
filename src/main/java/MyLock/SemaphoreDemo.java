package MyLock;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class SemaphoreDemo {



    abstract  static class Sync extends AbstractQueuedSynchronizer{

        Sync(int permits){
            setState(permits);
        }


        final int getPermits(){
            return getState();
        }


        /**
         * 获取的时候 -1
         * @param acquires
         * @return
         */
        final int nonfairTryAcquireShared(int acquires){
            for(;;){
                int available = getState();

                int remaining = available -acquires;

                if(remaining <0 || compareAndSetState(available,remaining))
                    return remaining;
            }
        }

        /**
         * 释放的时候可用共享锁 + 1
         * @param releases
         * @return
         */
        protected final boolean tryReleaseShared(int releases){
            for(;;){
                int current = getState();

                int next = current +releases;

                if(next<current) //overflow
                    throw new Error("Maximum permit count exceeded");

                /**
                 * 原子操作 state+1 可以获取锁
                 */
                if(compareAndSetState(current,next))
                    return true;
            }
        }

        /**
         * 减少许可
         */
        final void reducePermits(int reductions){
            for(;;){
                int current = getState();
                int next = current - reductions;

                if(next > current)
                    throw new Error("Permit count underflow");
                if(compareAndSetState(current,next));
                    return;
            }
        }

        /**
         * 清理许可
         * @return
         */
        final int drainPermits(){
            for(;;){
                int current = getState();

                if(current==0||compareAndSetState(current,0))
                    return current;
            }
        }


    }


    static final class NonfairSync extends Sync {

        NonfairSync(int permits) {
            super(permits);
        }

        //非公平锁
        protected int tryAcquireShared(int acquires){
            return nonfairTryAcquireShared(acquires);
        }
    }




    static final class FairSync extends  Sync{
        FairSync(int permits) {
            super(permits);
        }

        /**
         * 返回 remaining 大于0 可以获取锁 小于0不可获取锁
         * @param acquires
         * @return
         */
        protected int tryAcquireShared(int acquires){
            for(;;){
                //是否在排队队首要
                //判断“当前线程”是不是CLH队列中的第一个线程线程，
                if(hasQueuedPredecessors())
                    return -1;
                int available  = getState();
                int remaining = available -acquires;

                /**
                 * remaining < 0 或者 remaining 跟新成功
                 */
                if(remaining < 0 || compareAndSetState(available,remaining))
                    return remaining;
            }
        }
    }

    private final SemaphoreDemo.Sync sync;
    public SemaphoreDemo(int permits, boolean fair) {
        sync = fair ? new SemaphoreDemo.FairSync(permits) : new SemaphoreDemo.NonfairSync(permits);
    }

    public SemaphoreDemo(int permits) {
        sync = new NonfairSync(permits);
    }


    /**
     * 响应中断的
     * @throws InterruptedException
     */
    public void acquire() throws InterruptedException{
        sync.acquireSharedInterruptibly(1);
    }

    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    public boolean tryAcquire(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 总返回true
     */
    public void release() {
        sync.releaseShared(1);
    }

}
