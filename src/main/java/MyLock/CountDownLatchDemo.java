package MyLock;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class CountDownLatchDemo {


    private static final class Sync extends AbstractQueuedSynchronizer {
        Sync(int count){
            setState(count);
        }


        int getCount(){
            return getState();
        }

        /**
         * // 只有当 state == 0 的时候，这个方法才会返回 1
         *
         * state 0 可以获取锁
         * 非0 进入阻塞队列
         * @param arg
         * @return
         */
        @Override
        protected int tryAcquireShared(int arg) {
            return (getState() == 0 )? 1:-1;
        }


        /**
         * 每次减1 0的时候成功释放
         * @param arg
         * @return
         */
        @Override
        protected boolean tryReleaseShared(int arg) {
            for(;;){
                int c = getState();
                if(c == 0)
                    return false;
                int nextc = c -1;


                /**
                 * cas操作 0 的时候 返回true 唤醒线
                 */
                if(compareAndSetState(c,nextc))
                    return nextc==0;
            }
        }
    }


    private final Sync sync;



    public CountDownLatchDemo(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new CountDownLatchDemo.Sync(count);
    }


    /**
     * 获取   state  不为0 返回-1 进入阻塞队列
     *
     * @throws InterruptedException
     */
    public void await() throws InterruptedException{
        sync.acquireSharedInterruptibly(1);
    }


    /**
     * tryReleaseShared  将state 设为0时  唤醒阻塞队列线程
     */
    public void countDown() {
        sync.releaseShared(1);
    }
}
