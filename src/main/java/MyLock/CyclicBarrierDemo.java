package MyLock;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CyclicBarrierDemo {


    /*
    CyclicBarrier 是可以重复使用的，我们把每次从开始使用到穿过栅栏当做"一代"
     */
    private static class Generation {
        boolean broken = false;
    }


    private final ReentrantLock lock = new ReentrantLock();

    private final Condition trip = lock.newCondition();

    // 参与的线程数
    private final int parties;

    // 如果设置了这个，代表越过栅栏之前，要执行相应的操作
    private final Runnable barrierCommand;

    // 当前所处的“代”
    private CyclicBarrierDemo.Generation generation = new CyclicBarrierDemo.Generation();


    // 还没有到栅栏的线程数，这个值初始为 parties，然后递减
    // 还没有到栅栏的线程数 = parties - 已经到栅栏的数量
    private int count;


    // 开启新的一代，当最后一个线程到达栅栏上的时候，调用这个方法来唤醒其他线程，同时初始化“下一代”
    private void nextGeneration() {

        // 首先，需要唤醒所有的在栅栏上等待的线程

        trip.signalAll();

        // 更新 count 的值

        count = parties;

        // 重新生成“新一代”
        generation = new Generation();
    }


    //打破栅栏
    private void breakBarrier() {
        // 设置状态 broken 为 true
        generation.broken = true;
        // 重置 count 为初始值 parties
        count = parties;
        // 唤醒所有已经在等待的线程
        trip.signalAll();
    }



    public int await () throws InterruptedException,BrokenBarrierException {
        try{
            return dowait(false, 0L);
        }catch (TimeoutException toe){
            throw new Error(toe); //cannit happen
        }
    }


    /**
     * 重置
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }



    /**
     * 栅栏是否打破
     *
     * 1 中断，我们说了，如果某个等待的线程发生了中断，那么会打破栅栏，同时抛出 InterruptedException 异常；
     * 2 超时，打破栅栏，同时抛出 TimeoutException 异常；
     * 3 指定执行的操作抛出了异常，这个我们前面也说过。
     * @return
     */
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }


    /**
     * 获取还有多少个线程
     * @return
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }



    private int dowait(boolean timed, long nanos)throws InterruptedException,
            BrokenBarrierException,
            TimeoutException {
        final ReentrantLock lock = this.lock;
        // 先要获取到锁，然后在 finally 中要记得释放锁
        // 如果记得 Condition 部分的话，我们知道 condition 的 await 会释放锁，signal 的时候需要重新获取锁

        lock.lock();

        try{
            final Generation g = generation;

            // 检查栅栏是否被打破，如果被打破，抛出 BrokenBarrierException 异常

            if(g.broken)
                throw new BrokenBarrierException();
            //检查中断状态
            if(Thread.interrupted()){
                breakBarrier();
                throw new InterruptedException();
            }

            // index 是这个 await 方法的返回值
            // 注意到这里，这个是从 count 递减后得到的值

            int index = --count;

            // 如果等于 0，说明所有的线程都到栅栏上了，准备通过
            if(index == 0) {
                boolean ranAction = false;

                try {
                    // 如果在初始化的时候，指定了通过栅栏前需要执行的操作，在这里会得到执行
                    final Runnable command = barrierCommand;
                    if (command != null) {
                        command.run();
                    }
                    // 如果 ranAction 为 true，说明执行 command.run() 的时候，没有发生异常退出的情况
                    ranAction = true;
                    // 唤醒等待的线程，然后开启新的一代
                    nextGeneration();
                } finally {
                    if (!ranAction)
                        // 进到这里，说明执行指定操作的时候，发生了异常，那么需要打破栅栏
                        // 打破栅栏意味着唤醒所有等待的线程，设置 broken 为 true，重置 count 为 parties
                        breakBarrier();
                }

            }
                // loop until tripped, broken, interrupted, or timed out
                // 如果是最后一个线程调用 await，那么上面就返回了
                // 下面的操作是给那些不是最后一个到达栅栏的线程执行的

                for(;;){
                    try{
                        // 如果带有超时机制，调用带超时的 Condition 的 await 方法等待，直到最后一个线程调用 await
                        if(!timed)
                            trip.await();
                        else if(nanos>0L)
                            nanos = trip.awaitNanos(nanos);
                    }catch (InterruptedException e){
                    // 如果到这里，说明等待的线程在 await（是 Condition 的 await）的时候被中断
                        if(g == generation && !g.broken){
                            // 打破栅栏
                            breakBarrier();
                            // 打破栅栏后，重新抛出这个 InterruptedException 异常给外层调用的方法
                            throw e;
                        }else{
                            // 到这里，说明 g != generation, 说明新的一代已经产生，即最后一个线程 await 执行完成，
                            // 那么此时没有必要再抛出 InterruptedException 异常，记录下来这个中断信息即可
                            // 或者是栅栏已经被打破了，那么也不应该抛出 InterruptedException 异常，
                            // 而是之后抛出 BrokenBarrierException 异常
                            Thread.currentThread().interrupt();

                        }
                    }

                    // 唤醒后，检查栅栏是否是“破的”
                    if (g.broken)
                        throw new BrokenBarrierException();


                    /** 这个 for 循环除了异常，就是要从这里退出了
                        我们要清楚，最后一个线程在执行完指定任务(如果有的话)，会调用 nextGeneration 来开启一个新的代

                        然后释放掉锁，其他线程从 Condition 的 await 方法中得到锁并返回，然后到这里的时候，其实就会满足 g != generation 的

                        那什么时候不满足呢？barrierCommand 执行过程中抛出了异常，那么会执行打破栅栏操作，

                        设置 broken 为true，然后唤醒这些线程。这些线程会从上面的 if (g.broken) 这个分支抛 BrokenBarrierException 异常返回

                        当然，还有最后一种可能，那就是 await 超时，此种情况不会从上面的 if 分支异常返回，也不会从这里返回，会执行后面的代码
                     */

                    if (g != generation)
                        return index;



                    // 如果醒来发现超时了，打破栅栏，抛出异常
                    if (timed && nanos <= 0L) {
                        breakBarrier();
                        throw new TimeoutException();
                    }
                }
        }finally {
            lock.unlock();
        }


    }


    public CyclicBarrierDemo(int parties,Runnable barrierAction){
        if(parties <=0 ) throw new IllegalArgumentException();
        this.parties = parties;

        this.count = parties;

        this.barrierCommand = barrierAction;


    }

    public CyclicBarrierDemo(int parties) {
        this(parties, null);
    }


}
