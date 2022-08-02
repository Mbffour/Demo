package MyExecutor.MyFuture;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

public class MyFutureTask<V> implements MyRunnableFuture<V>{


    //线程可见性
    private volatile int state;
    /* Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;


    /** The underlying callable; nulled out after running */
    private Callable<V> callable;
    /** The result to return or exception to throw from get() */
    private Object outcome; // non-volatile, protected by state reads/writes
    /** The thread running the callable; CASed during run() */
    private volatile Thread runner;
    /** Treiber stack of waiting threads */
    private volatile WaitNode waiters;


    public MyFutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();
        this.callable = callable;

        this.state = NEW;
    }

    @Override
    public void run() {

        if(state!=NEW||
                /**
                 * AQS 设置当前线程为执行线程   不成功说明有其他线程执行
                 */
                !UNSAFE.compareAndSwapObject(this,runnerOffset,null,Thread.currentThread())){
            return;
        }

        try{
            Callable<V> c =callable;
            if(c!=null&&state==NEW){
                V result;
                boolean ran;

                try {
                    //返回结果
                    result = c.call();
                    //正常执行
                    ran = true;
                }catch (Throwable ex) {
                    // 出现异常 设置异常
                    result = null;
                    ran = false;
                    setException(ex);
                }

                /**
                 * 执行完之后 设置状态为 COMPLETING
                 */
                if (ran)
                    set(result);
            }
        }finally {

            /**
             * 捕获中断  发起中断命令
             */
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner =null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s =state;
            if(s>=INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }

    }

    /**
     * 正常执行
     * @param v
     */
    protected void set(V v) {

        /**
         * 设置完成状态
         */
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            //设置最终状态
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state

            /**
             * 完成操作 unpark所有等待线程
             */
            finishCompletion();
        }
    }
    /**
     * 线程执行过程中出现异常
     * @param t
     */
    private void setException(Throwable t) {
        //设置 任务 从新建状态 -》 执行中
        if(UNSAFE.compareAndSwapInt(this,stateOffset,NEW,COMPLETING)){
            //设置成功
            outcome = t;
            //将 state 直接设置为 EXCEPTIONAL
            UNSAFE.putOrderedInt(this,stateOffset,EXCEPTIONAL); // final state
            finishCompletion();
        }

    }


    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     *
     * 等待线程
     */
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;
        WaitNode() { thread = Thread.currentThread(); }
    }


    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     *
     * 被唤醒的线程会各自从awaitDone()方法中的LockSupport.park*()阻塞中返回，
     * 然后会进行新一轮的循环。在新一轮的循环中会返回执行结果(或者更确切的说是返回任务的状态)。
     *
     */
    private void finishCompletion() {
        for(WaitNode q;(q = waiters)!=null;){
            //设置等待节点置为null
            /**
             * 等待链表
             * 其他线程也可能同时在增加列表
             */
            if(UNSAFE.compareAndSwapObject(this,waitersOffset,q,null)){
                for(;;){
                    //释放锁
                    Thread t = q.thread;


                    if(t!=null){
                        q.thread = null;
                        //解锁等待线程  waiters中等待的线程依次解锁
                        LockSupport.unpark(t);
                    }

                    WaitNode next = q.next;
                    if(next==null){
                        break;
                    }

                    q.next = null; //当前节点 gc掉

                    q = next;
                }
                break;
            }
        }

        /**
         * 钩子函数  在ExecutorCompletionService 中 把完成的任务放入队列
         */
        done();

        callable = null;        // to reduce footprint


    }

    protected void done() {
    }

    /**
     * 处理中断结果
     * @param s
     */
    private void handlePossibleCancellationInterrupt(int s) {

        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                //挂起线程等待中断检测处理
                Thread.yield(); // wait out pending interrupt
    }

    /**
     * 取消
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {

        //1 如果任务结束  直接返回false
        //
        // COMPLETING状态说明已经执行完任务返回结果了
        if(state !=NEW){
            return false;
        }

        //2 如果需要中断任务执行线程  true /false
        //  任务有可能在执行中       result = c.call();  阻塞在这

        /**
         * 中断执行任务的线程
         * 这个线程可能还有很多其他任务
         */
        if(mayInterruptIfRunning){
            // 从新任务->中断状态  CAS  如果失败说明其他线程已经执行了任务      COMPLETING -》EXCEPTIONAL/COMPLETING -》NORML
            if(!UNSAFE.compareAndSwapInt(this,stateOffset,NEW,INTERRUPTING))
                return false;

            //运行任务的线程
            Thread t = runner;
            /**
             * 中断任务
             */
            if(t!=null)
                t.interrupt();  // 线程 中断置为 可中断状态  但不是立马中断   只是设置中断标志位 让任务可以在某节点响应中断
            UNSAFE.putOrderedInt(this,stateOffset,INTERRUPTED);   //直接设为最终状态     INTERRUPTING-》INTERRUPTED
        }

        //如果不需要中断任务执行线程, 状态列为取消   还在队列的任务 run 方法时会直接返回
        /**
         * 仅仅是把任务设置为CANCELLED  线程还是在自己工作
         *
         * 任务执行中 call()调用  状态还是 new
         */
        else if(!UNSAFE.compareAndSwapInt(this,stateOffset,NEW,CANCELLED))  // new -》CANCELLED
            //其他线程设置状态 或者 任务已经执行什么的

        /**
         * 说明任务已经执行 取消没有用   false
         */
            return false;


        /**
         * 成功设置取消后  唤醒其他 get 阻塞的线程
         */

        finishCompletion();
        return true;
    }

    @Override
    public boolean isCancelled() {

        return state >= CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != NEW;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        //小于等于运行状态
        if(s<=COMPLETING){
            s=awaitDone(false, 0L);
        }

        return report(s);
    }

    private V report(int s) throws ExecutionException{
        Object x = outcome;

        /**
         * 正常结束
         */
        if(s==NORMAL){
            return (V)x;
        }
        /**
         * 调用future.cancel 中断会抛出的异常  被取消
         * 掩盖 任务中的异常
         */
        if(s>=CANCELLED)
            throw new CancellationException();

        /**
         * 包括 其他线程调用执行任务线程的 Theard.Interrupt 引起的 中断异常 也在该异常返回
         *
         */
        throw new ExecutionException((Throwable)x);

    }


    /**
     * awaitDone()中有个死循环，每一次循环都会
     *
     * 判断调用get()的线程是否被其他线程中断，如果是的话则在等待队列中删除对应节点然后抛出InterruptedException异常。
     * 获取任务当前状态，如果当前任务状态大于COMPLETING则表示任务执行完成，则把thread字段置null并返回结果。
     * 如果任务处于COMPLETING状态，则表示任务已经处理完成(正常执行完成或者执行出现异常)，但是执行结果或者异常原因还没有保存到outcome字段中。这个时候调用线程让出执行权让其他线程优先执行。
     * 如果等待节点为空，则构造一个等待节点WaitNode。
     * 如果第四步中新建的节点还没如队列，则CAS的把该节点加入waiters队列的首节点。
     * 阻塞等待。
     * 假设当前state=NEW且waiters为NULL,也就是说还没有任何一个线程调用get()获取执行结果，
     * 这个时候有两个线程threadA和threadB先后调用get()来获取执行结果。
     * 再假设这两个线程在加入阻塞队列进行阻塞等待之前任务都没有执行完成且threadA和threadB都没有被中断的情况下
     * (因为如果threadA和threadB在进行阻塞等待结果之前任务就执行完成或线程本身被中断的话，awaitDone()就执行结束返回了)，
     * 执行过程是这样的，以threadA为例:
     *
     * 第一轮for循环，执行的逻辑是q == null,所以这时候会新建一个节点q。第一轮循环结束。
     *
     * 第二轮for循环，执行的逻辑是!queue，这个时候会把第一轮循环中生成的节点的netx指针指向waiters，
     * 然后CAS的把节点q替换waiters。也就是把新生成的节点添加到waiters链表的首节点。如果替换成功，queued=true。第二轮循环结束。
     *
     * 第三轮for循环，进行阻塞等待。要么阻塞特定时间，要么一直阻塞知道被其他线程唤醒。
     *
     *
     */


    /**
     * 等待处理
     * @param timed  是否定时等待
     * @param nanos
     * @return
     */
    private int awaitDone(boolean timed, long nanos) throws InterruptedException{

        //设置延时时间
        final long deadline = timed ? System.nanoTime() + nanos : 0L;

        WaitNode q = null;

        boolean queued = false;

        for(;;){
            /**
             * 如果获取线程被中断了
             * 移除等待节点 并且抛出中断异常
             * 可响应中断
             *
             *  LockSupport.park 响应中断 继续循环到这
             * Thread.interrupted() 如果中断 返回 true 并设置false
             *
             *
             */
            if(Thread.interrupted()){
                removeWaiter(q);
                throw new InterruptedException();
            }

            int s = state;

            //已经有结果
            if(s>COMPLETING){
                if(q !=null){
                    q.thread = null;
                }
                return s;  //可能是 取消/完成/异常
            }
            /**
             * 3 表示任务已经结束但是任务执行线程还没来得及给outcome赋值
             *   这个时候让出执行权让其他线程优先执行
              */
            else if(s==COMPLETING){// cannot time out yet
                Thread.yield();  //让出执行时间
            }
            /**
             *  4  如果等待节点为空，则构造一个等待节点  任务没有执行NEW状态
              */
            else if(q == null){
                q = new WaitNode();    //thread = Thread.currentThread()
            }
            /**
             *   5. 如果还没有入队列，则把当前节点加入waiters首节点并替换原来waiters
             *   首节点
             */
            else if(!queued){
                /**
                 * 多个线程入队列
                 */
                //如果失败 就再次循环 直到竞争成功
                queued = UNSAFE.compareAndSwapObject(this,waitersOffset,q.next=waiters,q);
            }
            else if(timed){
                nanos =deadline -System.nanoTime();
                // 如果需要等待特定时间，则先计算要等待的时间
                // 如果已经超时，则删除对应节点并返回对应的状态
                if(nanos<=0L){
                    //阻塞到时后 移除等待着 并返回状态
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);  //阻塞一段时间
            }
            else
            /**
             * 能够响应中断     Thread.currentThread().interrupt();
             */
            LockSupport.park(this); //阻塞当前线程
        }
    }

    /**
     * 移除等待节点  没懂
     * @param node
     */
    private void removeWaiter(WaitNode node) {
        //第一个获取节点的线程为null
        if(node!=null){
            //清理线程
            node.thread = null;
            retry:
            for(;;){
                //waiters 等待链表的头节点
                for(WaitNode pred = null, q=waiters, s ;q !=null ; q = s){

                    //下一个节点
                    s = q.next;

                    //等待队列头节点线程非空
                    if(q.thread !=null){
                        pred = q;
                    }else if(pred!=null){

                    }
                    //设置头节点
                    else if(!UNSAFE.compareAndSwapObject(this,waitersOffset,q,s))
                        continue retry;
                }
                break ;
            }
        }

    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        if(unit == null){
            throw new NullPointerException();
        }
        int s = state;
        if(s<=COMPLETING&&(s = awaitDone(true,unit.toNanos(timeout)))<=COMPLETING)
            throw  new TimeoutException();

        return report(s);
    }






    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;

    //执行线程偏移量
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }


}
