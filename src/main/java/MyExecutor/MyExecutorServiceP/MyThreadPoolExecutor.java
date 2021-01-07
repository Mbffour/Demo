package MyExecutor.MyExecutorServiceP;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyThreadPoolExecutor extends MyAbstractExecutorService {

    // 线程池的控制状态（用来表示线程池的运行状态（整形的高3位）和运行的worker数量（低29位））
    private AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING,0));

    // 29位的偏移量
    private static final int COUNT_BITS = Integer.SIZE - 3;

    // 最大容量（2^29 - 1）
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;


    /**
     * RUNNING     :    接受新任务并且处理已经进入阻塞队列的任务
     * SHUTDOWN    :    不接受新任务，但是处理已经进入阻塞队列的任务
     * STOP        :    不接受新任务，不处理已经进入阻塞队列的任务并且中断正在运行的任务
     * TIDYING     :    所有的任务都已经终止，workerCount为0， 线程转化为TIDYING状态并且调用terminated钩子函数
     * TERMINATED  :    terminated钩子函数已经运行完成
     *
     * // 线程运行状态，总共有5个状态，需要3位来表示（所以偏移量的29 = 32 - 3） 4字节
     **/
    // runState is stored in the high-order bits
    private static final int RUNNING    = -1 << COUNT_BITS; //111  00000000000
    private static final int SHUTDOWN   =  0 << COUNT_BITS; //000  00000000000
    private static final int STOP       =  1 << COUNT_BITS; //001  00000000000
    private static final int TIDYING    =  2 << COUNT_BITS; //010  00000000000
    private static final int TERMINATED =  3 << COUNT_BITS; //011  00000000000


    // Packing and unpacking ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; }  //111 保留高3位
    //都为1为1 否则为0
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    //两个数只要有一个为1则为1，否则就为0。
    private static int ctlOf(int rs, int wc) { return rs | wc; }



    /*
     * Bit field accessors that don't require unpacking ctl.
     * These depend on the bit layout and on workerCount being never negative.
     */

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }


    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }


    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }


    /**
     * 是否在运行中
     * @param c
     * @return
     */
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    //阻塞队列
    private final BlockingQueue<Runnable> workQueue;

    //可重入锁
    private final ReentrantLock mainLock = new ReentrantLock();

    //存放工作线程集合
    private final HashSet<Worker> workers = new HashSet<>();

    //终止条件
    private final Condition termination = mainLock.newCondition();

    //最大线程池容量
    private int largestPoolSize;

    //已完成任务数量
    private long completedTaskCount;

    //线程工厂
    private volatile ThreadFactory threadFactory;

    //拒绝执行处理器
    private volatile RejectedExecutionHandler handler;

    //线程等待运行时间
    private volatile long keepAliveTime;

    /**
      *如果为false（默认值），即使空闲，核心线程也会保持活动状态。
      *如果为true，则核心线程使用keepAliveTime超时等待
      *工作。
      */
    private volatile boolean allowCoreThreadTimeOut;

    //核心池大小
    private volatile int corePoolSize;

    //最大线程池大小
    private volatile int maximumPoolSize;

    //默认拒绝策略
    private static final RejectedExecutionHandler defaultHandler = new ThreadPoolExecutor.AbortPolicy();


    //
    private static final RuntimePermission shutdownPerm =
            new RuntimePermission("modifyThread");

    private final AccessControlContext acc;


    /**
     * 默认线程工厂 和拒绝策略
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     */
    public MyThreadPoolExecutor(int corePoolSize,
                                int maximumPoolSize,
                                long keepAliveTime,
                                TimeUnit unit,
                                BlockingQueue<Runnable> workQueue){
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);
    }


    public MyThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.acc = System.getSecurityManager() == null ?
                null :
                AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    public long getTaskCount(){
        final ReentrantLock mainlock = this.mainLock;
        mainlock.lock();
        try{
            // 已经完成的数量
            long n = completedTaskCount;

            //每个执行线程完成的任务
            for(Worker w:workers){
                n+= w.completedTasks;

                /**
                 * 任务正在运行中 加一个任务
                 */
                if(w.isLocked()){
                    ++n;
                }
            }
            //阻塞队列的任务
            return n + workQueue.size();
        }finally {
            mainlock.unlock();
        }
    }

    /**
     * 获取已经完成的任务数
     * @return
     */
    public long getCompletedTaskCount(){
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try{
            long n = completedTaskCount;
            for(Worker w: workers){
                n+=w.completedTasks;
            }
            return n;
        }finally {
            mainLock.unlock();
        }
    }


    /**
     * 线程池最大的容量
     * @return
     */
    public long getLargestPoolSize(){
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try{
            return largestPoolSize;
        }finally {
            mainLock.unlock();
        }

    }

    public  long getPoolSize(){
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try{
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(),TIDYING)?0:workers.size();
        }finally {
            mainLock.unlock();
        }

    }

    /**
     * 获取活动线程数
     * @return
     */
    public long getActiveCount(){
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try{
            int n = 0;
            for(Worker w:workers){
                if(w.isLocked()){
                    ++n;
                }
            }
            return n;
        } finally {
            mainLock.unlock();
        }

    }


    /**
     * 当在客户端调用submit时，之后会间接调用到execute函数，其在将来某个时间执行给定任务，
     * 此方法中并不会直接运行给定的任务。此方法中主要会调用到addWorker函数
     * @param command
     */
    @Override
    public void execute(Runnable command) {
        if(command == null)
            throw new NullPointerException();
        /*
         * 进行下面三步
         *
         * 1. 如果运行的线程小于corePoolSize,则尝试使用用户定义的Runnalbe对象创建一个新的线程
         *     调用addWorker函数会原子性的检查runState和workCount，通过返回false来防止在不应
         *     该添加线程时添加了线程
         *
         * 2. 如果一个任务能够成功入队列，在添加一个线城时仍需要进行双重检查（因为在前一次检查后
         *     该线程死亡了），或者当进入到此方法时，线程池已经shutdown了，所以需要再次检查状态，
         *    若有必要，当停止时还需要回滚入队列操作，或者当线程池没有线程时需要创建一个新线程
         *
         * 3. 如果无法入队列，那么需要增加一个新线程，如果此操作失败，那么就意味着线程池已经shut
         *     down或者已经饱和了，所以拒绝任务
         */

        //获取线程池控制状态
        int c = ctl.get();

        /**
         *1. 如果运行的线程小于corePoolSize,则尝试使用用户定义的Runnalbe对象创建一个新的线程
         *   调用addWorker函数会原子性的检查runState和workCount，通过返回false来防止在不应
         *   该添加线程时添加了线程
         */
        if(workerCountOf(c)<corePoolSize){  //worker小于corePoolSize

            if(addWorker(command,true))
                return;  //成功返回
            //不成功再次获取状态
            c = ctl.get();
        }

        // 线程池处于RUNNING状态，将命令（用户自定义的Runnable对象）添加进workQueue队列
        // offer 队列满 返回false


        /**
         * 无界队列 超过核心线程数 A任务 添加进去
         * B 任务添加时 添加不进去   addWorker(command,false)  加入一个线程  该线程获取到 A 任务
         * C 任务就又可以添加进去
         */

        //offer方法当队列满，而且放入时间超过设定时间时，返回false;
        if(isRunning(c) && workQueue.offer(command)){

          /*  workQueue.take();
            workQueue.put();

            //异常
            workQueue.add();
            workQueue.remove();

            //返回

            workQueue.offer();
            workQueue.poll();*/
            int recheck = ctl.get();

            //再次检查
            if(!isRunning(recheck)&&remove(command))  // 线程池不处于RUNNING状态，将命令从workQueue队列中移除
                // 拒绝执行命令
            /**
             * 队列满 拒绝策略
             */
                reject(command);

            /**
             * 工作者线程 为0 添加一个 工作者
              */
            else if(workerCountOf(recheck) == 0)
            /**
             *  command 已经在队列中了
             *  添加worker
             */
            addWorker(null,false);
            //任务添加到队列中了
        }
        /**
         * 队列满了。
         * 这时，如果当前线程池中的线程数还没有超过最大线程数，就创建一个新的线程去执行这个任务，
         * 如果失败就拒绝这个任务  （线程超出最大线程）
         *
         */
        else if(!addWorker(command,false)){
            reject(command);
        }
    }

    /**
     * 拒绝策略
     * @param command
     */
    final void reject(Runnable command) {
        /**
         * TODO null
         */
       handler.rejectedExecution(command, null);
    }

    /**
     * 移除一个
     * @param task
     * @return
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }



    /**
     * 1 原子性的增加workerCount。
     *
     * 2 将用户给定的任务封装成为一个worker，并将此worker添加进workers集合中。
     *
     * 3 启动worker对应的线程，并启动该线程，运行worker的run方法。
     *
     * 4 回滚worker的创建动作，即将worker从workers集合中删除，并原子性的减少workerCount。
     * @param firstTask
     * @param core
     * @return
     */
    private boolean addWorker(Runnable firstTask, boolean core) {

        retry:
        //外层无限循环
        for(;;) {
            int c = ctl.get();

            int rs = runStateOf(c);

            /*
             * 这个if判断
             * 如果rs >= SHUTDOWN，则表示此时不再接收新任务；
             *
             * 接着判断以下3个条件，只要有1个不满足，则返回false：
             * 1. rs == SHUTDOWN，这时表示关闭状态，不再接受新提交的任务，但却可以继续处理阻塞队列中已保存的任务
             * 2. firsTask为空
             * 3. 阻塞队列不为空
             *
             * 首先考虑rs == SHUTDOWN的情况
             * 这种情况下不会接受新提交的任务，所以在firstTask不为空的时候会返回false；
             * 然后，如果firstTask为空，并且workQueue也为空，则返回false，
             * 因为队列中已经没有任务了，不需要再添加线程了执行了
             */
            if (rs >= SHUTDOWN &&      //状态大于 SHUTDOWN  初始ctl为RUNNING 小于 SHUTDOWN
                    !(rs == SHUTDOWN &&  //状态等于 SHUTDOWN 时
                            firstTask == null &&  //第一个任务 为null    不为null 返回false  ！false = true  ->renturn false
                            !workQueue.isEmpty())) // 工作队列 不为空     firstTask==null workQueue==null  ->renturn false
                //返回 不添加工作者
                return false;


            for (;;) {
                //woker数量

                int wc = workerCountOf(c);

                // 如果wc超过CAPACITY，也就是ctl的低29位的最大值（二进制是29个1），返回false；  536870911
                // 这里的core是addWorker方法的第二个参数，如果为true表示根据corePoolSize来比较，
                // 如果为false则根据maximumPoolSize来比较。

                /**
                 * 添加核心线程就比较核心线程数
                 * 添加额外线程就比较最大线程数
                 * core 核心线程添加
                 */
                if (wc >= CAPACITY ||    //worker数量大于等于最大容量
                        wc >= (core ? corePoolSize : maximumPoolSize))  //worker数量大于等于核心线程池大小或者最大线程池大小
                    return false;

                /**
                 * 尝试增加workerCount，如果成功，则跳出第一个for循环
                 * 同时多个线程 调用addWorker  只有一个成功
                 * 其余的再次循环进行判定
                 */
                if (compareAndIncrementWorkerCount(c))

                    //设置成功跳出循环
                    break retry;

                // 如果增加workerCount失败，则重新获取ctl的值
                /**
                 * 比如其他线程增加 workerCount
                 * 但只要状态不变 就继续在内层循环
                 */
                c = ctl.get();

                if (runStateOf(c) != rs) // 此次的状态与上次获取的状态不相同
                    // 跳过剩余部分，继续循环
                /**
                 * 重新获取一下状态
                 * int c = ctl.get();
                 *
                 * int rs = runStateOf(c);
                 */
                    continue retry;
            }
        }

        /**
         * 这里 状态信息c 中已经添加成功一个工作者了  在下面可以添加了 不影响上面 新工作者加入
         */
        // worker开始标识
            boolean workerStarted = false;
            // worker被添加标识
            boolean workerAdded = false;


            Worker w = null;

            try{
                // 根据firstTask来创建Worker对象   直接执行任务
                w = new Worker(firstTask);

                // 每一个Worker对象都会创建一个线程
                final Thread t = w.thread;

                //线程不为空
                if(t !=null){

                    //线程池锁
                    final ReentrantLock mainLock = this.mainLock;

                    //获取锁
                    /**
                     * 锁住来更新池子的状态
                     */
                    mainLock.lock();

                    try{
                        // Recheck while holding lock.
                        // Back out on ThreadFactory failure or if
                        // shut down before lock acquired.

                        /**
                         *   rs < SHUTDOWN表示是RUNNING状态；
                         *   如果rs是RUNNING状态  或者 rs是SHUTDOWN状态并且firstTask为null，向线程池中添加线程。
                         *
                         *   因为在SHUTDOWN时不会在添加新的任务，但还是会执行workQueue中的任务
                         *
                         *   firstTask为null 表示添加线程去阻塞队列  执行任务
                         */

                        int rs = runStateOf(ctl.get());  //获取运行时状态

                        if(rs<SHUTDOWN||  //RUNNING
                                (rs == SHUTDOWN && firstTask == null)){  // 等于SHUTDOWN并且firstTask为null  只添加线程来执行队列里的任务


                            /**
                             * 未启动线程
                             */
                            if(t.isAlive())   // 线程刚添加进来，还未启动就存活
                                throw new IllegalThreadStateException();

                            workers.add(w);

                            int s = workers.size();

                            //最大的线程数
                            if(s>largestPoolSize)     // 队列大小大于largestPoolSize
                                largestPoolSize = s;   // 重新设置largestPoolSize  到达的最大线程数

                            // 设置worker已被添加标识
                            workerAdded = true;
                        }
                    }finally {
                        mainLock.unlock();
                    }

                    if(workerAdded){
                        /**
                         * 启动线程
                         */
                        t.start();
                        workerStarted = true;
                    }
                }
            }finally {
                /**
                 * 添加线程失败
                 */
                if (! workerStarted)
                    addWorkerFailed(w);
            }
        return workerStarted;
    }


    /**
     * 添加工作者线程失败
     * @param w
     */
    private void addWorkerFailed(Worker w){
        final ReentrantLock mainLock = this.mainLock;

        mainLock.lock();
        try{
            if(w != null)
                workers.remove(w);
            // 减少工作者的数量
            decrementWorkerCount();

            /**
             * 尝试停止
             */
            tryTerminate();
        }finally {
            mainLock.unlock();
        }
    }




    /**
     *
     * 如果线程池的状态为SHUTDOWN并且线程池和阻塞队列都为空或者状态为STOP并且线程池为空，
     *
     * 则将线程池控制状态转化为TERMINATED；否则，将中断一个空闲的worker
     *
     *
     *
     * shutdown方法中，会中断所有空闲的工作线程，如果在执行shutdown时工作线程没有空闲，然后又去调用了getTask方法，
     * 这时如果workQueue中没有任务了，调用workQueue.take()时就会一直阻塞。
     * 所以每次在工作线程结束时调用tryTerminate方法来尝试中断一个空闲工作线程，避免在队列为空时取任务一直阻塞的情况。
     */
    final  void tryTerminate(){
        for(;;){
            int c = ctl.get();
            /**
             * 当前线程池的状态为以下几种情况时，直接返回：
             * 1. RUNNING，因为还在运行中，不能停止；
             * 2. TIDYING或TERMINATED，因为线程池中已经没有正在运行的线程了；
             * 3. SHUTDOWN并且等待队列非空，这时要执行完workQueue中的task；  所以不中断线程
             *    为null
             */
            if(isRunning(c)||
                    runStateAtLeast(c, TIDYING)||      //所有线程已经终止
                    (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty())) //SHUTDOWN 阶段  队列不为null 要执行任务
                //返回
                return;


            /**
             * 需要中断线程了～
             */
            // 如果线程数量不为0，则中断一个空闲的工作线程，并返回
            if(workerCountOf(c) != 0){
                //只取消一个 中断一个空闲的worker  保证可以中断 在shutdown时 没有中断的线程 中断一个线程 又会调用 中断下一个 一直到0
                interruptIdleWorkers(ONLY_ONE);
                return;
            }



            /**
             * 工作线程为0
             */
            final ReentrantLock mainLock = this.mainLock;

            mainLock.lock();

            /**
             * 直接设置线程池状态
             *
             * 线程执行任务时会判定状态
             */
            try{

                // 比较并设置线程池控制状态为TIDYING
                /// 这里尝试设置状态为TIDYING，如果设置成功，则调用terminated方法
                if(ctl.compareAndSet(c, ctlOf(TIDYING, 0))){
                    try{
                        // 终止，钩子函数
                        terminated();
                    }finally {
                        ctl.set((ctlOf(TERMINATED,0)));
                        // 释放在termination条件上等待的所有线程
                        termination.signalAll();
                    }
                    return;
                }
            }finally {
                mainLock.unlock();
            }

        }
    }

    protected void terminated() { }
    /**
     * 打断工作者线程
     * 函数将会中断正在等待任务的空闲worker。
     *
     * interruptIdleWorkers持有mainLock，会遍历workers来逐个判断工作线程是否空闲
     * @param onlyOne
     */
    private void interruptIdleWorkers(boolean onlyOne){
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        //HashSet 不保证线程安全 所以需要锁
        try{
            for(Worker w: workers){
                Thread t  = w.thread;
                /**
                 * 如果没有被打断并且成功获得锁
                 *
                 * w.tryLock()  能获取到锁 说明是该线程没有执行任务
                 *
                 * 有可能在循环过程中某线程还在执行任务 之后getTask() 死锁
                 */
                if(!t.isInterrupted() && w.tryLock()){
                    try{
                        t.interrupt();
                    }catch (SecurityException ignore){
                    }finally {
                        /**
                         * release(1)  释放占用线程
                         */
                        w.unlock();
                    }
                }
                if (onlyOne)// 若只中断一个，则跳出循环
                break;
            }

        }finally {
            mainLock.unlock();
        }

    }


    /**
     * Common form of interruptIdleWorkers, to avoid having to
     * remember what the boolean argument means.
     */
    private void interruptIdleWorkers() {
        //中断所有线程
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;




    private void decrementWorkerCount() {
        do {} while (! compareAndDecrementWorkerCount(ctl.get()));
    }


    /**
     * 此函数会按过去执行已提交任务的顺序发起一个有序的关闭，但是不接受新任务。
     * 先会检查是否具有shutdown的权限，然后设置线程池的控制状态为SHUTDOWN，
     * 之后中断空闲的worker，最后尝试终止线程池。
     *
     *
     *
     * shutdown方法中，会中断所有空闲的工作线程，如果在执行shutdown时工作线程没有空闲，然后又去调用了getTask方法，
     * 这时如果workQueue中没有任务了，调用workQueue.take()时就会一直阻塞。
     * 所以每次在工作线程结束时调用tryTerminate方法来尝试中断一个空闲工作线程，避免在队列为空时取任务一直阻塞的情况。
     */
    @Override
    public void shutdown() {
        final  ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try{
            checkShutdownAccess();

            //设置为SHUTDOWN状态  不接受新任务 线程执行完在队列中的任务
            advanceRunState(SHUTDOWN);

            // 中断空闲worker  false  不保证全中断
            interruptIdleWorkers();

            //钩子函数
            onShutdown(); // hook for ScheduledThreadPoolExecutor

        }finally {
            mainLock.unlock();
        }
        /// 尝试结束线程池
        tryTerminate();
    }

    void onShutdown() {

    }

    private void advanceRunState(int targetState) {
        for (;;) {
            //CAS操作
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                    //设置状态以及更新工作者线程  高3位和低27位
                    ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * 检查是否有关闭线程的权限
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {

        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;

        mainLock.lock();
        try{
            checkShutdownAccess();

            advanceRunState(STOP);

            //执行中断所有线程 无论是否空闲
            interruptWorkers();


            tasks = drainQueue();

        }finally {
            mainLock.unlock();
        }

        /**
         * 尝试终止所有线程
         * 线程池的状态设置为TERMINATED
         */
        tryTerminate();
        return tasks;
    }

    /**
     * 直接中断所有 不区分是否在执行任务中
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }


    /**
     * 取出阻塞队列中没有被执行的任务并返回
     * @return
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<>();

        /**
         * drainTo():一次性从BlockingQueue获取所有可用的数据对象（还可以指定获取数据的个数），
         * 　　　通过该方法，可以提升获取数据效率；不需要多次分批加锁或释放锁。
         */
        q.drainTo(taskList);

        if(!q.isEmpty()){

            /**
             * 阻塞队列变为一个数组  不懂为什么要这样处理
             * 复制一份 防止同步更改异常
             */
            for(Runnable r:q.toArray(new Runnable[0])){
                if(q.remove(r)){
                    taskList.add(r);
                }
            }
        }
        return  taskList;
    }

    @Override
    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }

    @Override
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (;;) {
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }



    /**
     * 自定线程工厂
     * @return
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }


    /**
     * 执行任务
     * runWorker方法的执行过程：
     *
     * 1 while循环不断地通过getTask()方法获取任务；
     * 2 getTask()方法从阻塞队列中取任务；
     * 3 如果线程池正在停止，那么要保证当前线程是中断状态，否则要保证当前线程不是中断状态；
     * 4 调用task.run()执行任务；
     * 5 如果task为null则跳出循环，执行processWorkerExit()方法；
     * 6 runWorker方法执行完毕，也代表着Worker中的run方法执行完毕，销毁线程。
     *
     * @param w
     */
    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        /**
         * 取出第一个任务
         */
        Runnable task = w.firstTask;

        w.firstTask = null ;

        //允许中断 （设置state为0，允许中断） 可以tryAcq
        w.unlock();   //allow interrupts

        boolean completedAbruptly = true;
        try{
            /**
             * 任务不为null 就执行任务
             * 任务不为null或者阻塞队列还存在任务   task
             * 当某个任务完成后，会从workQueue阻塞队列中取下一个任务。
             *
             * 在getTask()中卡住的线程没有占有锁 使得 可以响应中断
             *
             *
             *    worker 推出
             * 　1 阻塞队列已经为空，即没有任务可以运行了。
             *
             *   2 调用了shutDown或shutDownNow函数
             */
            while(task != null||(task = getTask()) != null){

                // 独占锁 说明获取到了任务  线程执行中

                /**
                 * 获取任务如果被阻塞 可以通过获取锁来判断  来进行中断
                 */
                w.lock();  //tryAcquire

                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                /**
                 * 获取任务后检查 线程池状态如果至少在STOP以上 检查该线程是否被中断 没有中断就中断
                 * 保持一致
                 *
                 * 如果线程池正在停止，那么要保证当前线程是中断状态；
                 * 如果不是的话，则要保证当前线程不是中断状态；
                 *
                 * 使用Thread.interrupted()来判断是否中断是为了确保在RUNNING或者SHUTDOWN状态时线程是非中断状态的，
                 * 因为Thread.interrupted()方法会复位中断的状态。
                 */
                if((runStateAtLeast(ctl.get(),STOP)||           // 线程池的运行状态至少应该高于STOP
                        // 线程被中断 并且 再次检查，线程池的运行状态至少应该高于STOP 并且 wt线程（当前线程）没有被中断
                        (Thread.interrupted()
                                &&runStateAtLeast(ctl.get(),STOP))) &&
                        !wt.isInterrupted())
                    //中断wt线程（当前线程）

                    //取任务 stop阶段 直接中断
                    wt.interrupt();

                try{

                    //执行之前调用钩子函数
                    beforeExecute(wt,task);

                    /**
                     * 执行线程
                     */
                    Throwable thrown = null;
                    try{
                        task.run();   //默认FutrueTask
                    }catch (RuntimeException x){   //从上往下匹配
                        thrown = x; throw x;
                    }catch (Error x){
                        thrown = x; throw x;
                    }catch (Throwable x){
                        thrown = x; throw new Error(x);
                    }finally {
                        //处理完成后,调用钩子函数
                        afterExecute(task, thrown);
                    }
                }finally {
                    task = null;   //gc
                    w.completedTasks++;

                    //任务执行完 将再次去获取任务
                    w.unlock();
                }
            }


            completedAbruptly = false;
        }finally {

            //捕获任务执行中的异常  中断 之类的
            //中断异常有异常 completedAbruptly变量来表示在执行任务过程中是否出现了异常
            // 工作线程任务处理完销毁 ，  调用钩子函数
            processWorkerExit(w, completedAbruptly);

        }


    }

    /**
     * Performs cleanup and bookkeeping for a dying worker. Called
     * only from worker threads. Unless completedAbruptly is set,
     * assumes that workerCount has already been adjusted to account
     * for exit.  This method removes thread from worker set, and
     * possibly terminates the pool or replaces the worker if either
     * it exited due to user task exception or if fewer than
     * corePoolSize workers are running or queue is non-empty but
     * there are no workers.
     *
     * @param w the worker
     * @param completedAbruptly if the worker died due to user exception
     *
     *
     * 此函数会根据是否中断了空闲线程来确定是否减少workerCount的值，
     * 并且将worker从workers集合中移除并且会尝试终止线程池
     *
     * 没有获取到任务也会到这里
     *
     *                                                  是否中断
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        /**
         * 中断        如果被中断，则需要减少workCount
         *
         * 如果completedAbruptly值为true，则说明线程执行时出现了异常，需要将workerCount减1；
         *
         * 如果线程执行时没有出现异常，说明在getTask()方法中已经已经对workerCount进行了减1操作，这里就不必再减了。
         */

        // 中断的减少
        // 正常结束的 已经在getTask 中减少
        //先减少一个线程
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();


        final ReentrantLock mainLock = this.mainLock;

        mainLock.lock();
        /**
         * 锁住更新 完成任务数
         */
        try{
            completedTaskCount +=w.completedTasks;
            // 从workers中移除，也就表示着从线程池中移除了一个工作线程
            workers.remove(w);
        }finally {
            mainLock.unlock();
        }

        /**
         *  根据线程池状态进行判断是否结束线程池
         *  每次线程结束调用
         */

        tryTerminate();


        int c = ctl.get();


        /**
         * 当线程池是RUNNING或SHUTDOWN状态时，如果worker是异常结束，那么会直接addWorker；
         *
         * 如果allowCoreThreadTimeOut=true，并且等待队列有任务，至少保留一个worker 执行任务
         *
         * 如果allowCoreThreadTimeOut=false，workerCount不少于corePoolSize。
         *
         * 状态至少是STOP
         */
        if(runStateAtLeast(c,STOP)){
            // 没有中断
            if(!completedAbruptly){
                /**
                 *如果为false（默认值），即使空闲，核心线程也会保持活动状态。   线程不能少于核心线程数
                 *如果为true，则核心线程使用keepAliveTime超时等待  线程最少为1个线程
                 *
                 */
                int min = allowCoreThreadTimeOut?0:corePoolSize;

                //当核心线程等待任务 并且  任务队列不为null 最小线程数为1
                if(min == 0 && ! workQueue.isEmpty())
                    min = 1;
                /*
                   工作线程大于最小线程
                   1 0  核心线程空闲等待 且 任务队列0
                   2 1  核心线程空闲等待 且 任务队列不为0
                   3 corePoolSize    大于所有核心线程数
                 */
                if(workerCountOf(c) >= min)
                    return;
            }


            //需要再增加一个工作线程
            addWorker(null,false);
        }


    }

    /**
     * 获取任务
     *
     * 在getTask方法中，如果这时线程池的状态是SHUTDOWN并且workQueue为空，那么就应该返回null来结束这个工作线程，
     * 而使线程池进入SHUTDOWN状态需要调用shutdown方法；
     *
     * shutdown方法会调用interruptIdleWorkers来中断空闲的线程，interruptIdleWorkers持有mainLock，
     * 会遍历workers来逐个判断工作线程是否空闲。但getTask方法中没有mainLock；
     *
     * 在getTask中，如果判断当前线程池状态是RUNNING，并且阻塞队列为空，那么会调用workQueue.take()进行阻塞；
     *
     * 如果在判断当前线程池状态是RUNNING后，这时调用了shutdown方法把状态改为了SHUTDOWN，这时如果不进行中断，那么当前的工作线程在调用了workQueue.take()后会一直阻塞而不会被销毁，因为在SHUTDOWN状态下不允许再有新的任务添加到workQueue中，这样一来线程池永远都关闭不了了；
     *
     * 由上可知，shutdown方法与getTask方法（从队列中获取任务时）存在竞态条件；
     *
     * 解决这一问题就需要用到线程的中断，也就是为什么要用interruptIdleWorkers方法。在调用workQueue.take()时，如果发现当前线程在执行之前或者执行期间是中断状态，则会抛出InterruptedException，解除阻塞的状态；
     *
     * 但是要中断工作线程，还要判断工作线程是否是空闲的，如果工作线程正在处理任务，就不应该发生中断；
     *
     * 所以Worker继承自AQS，在工作线程处理任务时会进行lock，interruptIdleWorkers在进行中断时会使用tryLock来判断该工作线程是否正在处理任务，如果tryLock返回true，说明该工作线程当前未执行任务，这时才可以被中断。
     *
     *
     *
     * shutdown方法与getTask方法（从队列中获取任务时）存在竞态条件；
     * @return
     */
    private Runnable getTask(){
        /**
         * 最后的一次 poll 是否超时
         */
        boolean timedOut = false; // Did the last poll() time out?

        for(;;){
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.

            /**
             * 如果线程池状态SHUTDOWN 不接受新任务
             * 并且 状态大于 STOP() 或者   工作队列任务为null  任务处理完了
             *
             * 减少工作者线程数量  没有任务可以执行了
             *
             * 因为如果当前线程池状态的值是SHUTDOWN或以上时，不允许再向阻塞队列中添加任务。
             *
             * 队列不能于null 且 状态shutdown  就继续处理队列的任务 直到 null  销毁
             */
            if(rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())){
                //减少工作者线程数量
                decrementWorkerCount();

                //返回null 来减少     processWorkerExit()
                return null;
            }

            int wc = workerCountOf(c);

            //   *如果为false（默认值），即使空闲，核心线程也会保持活动状态。
            //   *如果为true，则核心线程使用keepAliveTime超时等待
            //    当前线程是否大于核心线程

            /** Are workers subject to culling?
             * timed变量用于判断是否需要进行超时控制。
             *
             * allowCoreThreadTimeOut默认是false，也就是核心线程不允许进行超时；核心线程也会保持活动状态
             *
             * wc > corePoolSize，表示当前线程池中的线程数量大于核心线程数量；
             *
             *
             * 对于超过核心线程数量的这些线程，需要进行超时控制  超时的额外线程删掉
            **/
            //是否超时控制
            // 1 超过核心线程数的线程   2  allowCoreThreadTimeOut 开关 开启 true
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            /**
             *
             *   * wc > maximumPoolSize的情况是因为可能在此方法执行阶段同时执行了setMaximumPoolSize方法；
             *
             *   * timed (true ： 核心线程也会超时取消 ) && timedOut 如果为true，表示当前操作需要进行超时控制，
             *   并且上次从阻塞队列中获取任务发生了超时
             *
             *   * 接下来判断，如果有效线程数量大于1，或者阻塞队列是空的，那么尝试将workerCount减1；
             *   * 如果减1失败，则返回重试。
             *   * 如果wc == 1时，也就说明当前线程是线程池中唯一的一个线程了。
             *
             *
             *
             *
             * 当前工作线程大于 最大线程 ||  (timed &&  timedOut)) 当前线程小于最大线程  核心线程使用keepalivetime 超时等待  或者 工作线程大于核心线程
             * 当前工作线程 大于1 或者 工作队列为null
             */
            if((wc > maximumPoolSize || (timed &&  timedOut))
                    //如果进行下边判断 说明线程超过最大线程    或者当前线程已经超时 没有获得任务了
                    // 只要保证最少一个核心线程在工作  或者 队列null 就减少线程

                    //保证最少一个核心线程在工作  没任务可以去执行了 减少工作线程
                    && (wc > 1 || workQueue.isEmpty())){
                //减少一个工作线程数量  设置一次 不成功就继续循环
                if (compareAndDecrementWorkerCount(c))
                    return null;   //  processWorkerExit()

                continue; //继续循环
            }


            try{

                /**
                 * timed 是否要超时控制  true  超时后返回null 移除线程
                 *                     false 当前线程小于等于核心线程数 且不需要减少
                 *
                 * 根据timed来判断，如果为true，则通过阻塞队列的poll方法进行超时控制，
                 * 如果在keepAliveTime时间内没有获取到任务，则返回null；
                 * 否则通过take方法，如果这时队列为空，则take方法会阻塞直到队列不为空。
                 */
                Runnable r = timed?
                        // 核心线程使用keepAliveTime超时等待 或者 当前线程大于核心线程
                        workQueue.poll(keepAliveTime,TimeUnit.NANOSECONDS):

                        //核心线程空闲也保持活动状态  或者当前线程小于 工作线程  拿去线程 并阻塞  一直到获取任务

                        /**
                         * 通过中断来唤醒
                         */
                        workQueue.take();
                        ;

                /**
                 * 拿取到一个任务
                 */
                if(r != null)
                    return r;


                /**
                 * 超时控制  没拿到线程
                 * 超时  在下次循环里
                 * 如果  boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;
                 * 为 true 就销毁线程
                 */
                // 如果 r == null，说明已经超时，timedOut设置为true
                // timedOut为true  workQueue已经为空了，也就说明了当前线程池中不需要那么多线程来执行任务了
                //  再次循环中返回 null  销毁
                // 可以把多于corePoolSize数量的线程销毁掉，保持线程数量在corePoolSize即可
                timedOut = true;
            }catch (InterruptedException retry){
                // 如果获取任务时当前线程发生了中断，则设置timedOut为false并返回循环重试
                timedOut =false;
            }
        }


    }


    /**
     * Worker类继承了AQS，并实现了Runnable接口，注意其中的firstTask和thread属性：firstTask用它来保存传入的任务；
     * thread是在调用构造方法时通过ThreadFactory来创建的线程，是用来处理任务的线程。
     *
     * Worker继承了AQS，使用AQS来实现独占锁的功能。为什么不使用ReentrantLock来实现呢？可以看到tryAcquire方法，它是不允许重入的，而ReentrantLock是允许重入的：
     *
     * lock方法一旦获取了独占锁，表示当前线程正在执行任务中；
     *
     * 如果正在执行任务，则不应该中断线程；
     * 如果该线程现在不是独占锁的状态，也就是空闲的状态，说明它没有在处理任务，这时可以对该线程进行中断；
     *
     * 线程池在执行shutdown方法或tryTerminate方法时会调用interruptIdleWorkers方法来中断空闲的线程，
     * interruptIdleWorkers方法会使用tryLock方法来判断线程池中的线程是否是空闲状态；
     *
     * 之所以设置为不可重入，是因为我们不希望任务在调用像setCorePoolSize这样的线程池控制方法时重新获取锁。
     * 如果使用ReentrantLock，它是可重入的，这样如果在任务中调用了如setCorePoolSize这类线程池控制的方法，会中断正在运行的线程。
     *
     *
     */
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable{


        final Thread thread;

        Runnable firstTask;

        //表示已完成的任务数量
        volatile long completedTasks;

        /**
         * Creates with given first task and thread from ThreadFactory.
         * @param firstTask the first task (null if none)
         */

        Worker(Runnable firstTask){
            /**
             * tryAcquire方法是根据state是否是0来判断的，
             * 所以，setState(-1);将state设置为-1是为了禁止在执行任务前对线程进行中断。
             *
             *
             * tryAcquire compareAndSetState(0,1)
             *
             */
            setState(-1);
            //初始化第一个任务
            this.firstTask = firstTask;
            //生成一个新线程  并未启动
            this.thread = getThreadFactory().newThread(this::run);
        }

        /** Delegates main run loop to outer runWorker
         *  委托给外部的runWorker;
         * */
        @Override
        public void run() {
            runWorker(this);
        }

        // Lock methods
        //
        // The value 0 represents the unlocked state.
        // The value 1 represents the locked state.

        protected  boolean isHeldExclusively(){
            return getState()!=0;
        }

        /**
         * 获取一个信号量
         * @param unused
         * @return
         */
        protected boolean tryAcquire(int unused){
            if(compareAndSetState(0,1)){
                //设置独占线程
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unuser){
            setExclusiveOwnerThread(null);
            setState(0);
            return  true;
        }

        public void lock(){acquire(1);}

        public boolean tryLock() { return tryAcquire(1);}

        public void unlock(){release(1);}

        public boolean isLocked() { return isHeldExclusively(); }

        void interruptIfStarted(){
            Thread t ;
            //任务状态 是有线程 执行 并且执行线程是当前线程 并且不为null 和 未打断过
            //AQS状态大于等于0并且worker对应的线程不为null并且该线程没有被中断
            if(getState() >= 0 && (t = thread) != null && !t.isInterrupted()){
                try{
                    t.interrupt();
                }catch (SecurityException ignore){
                    //忽视安全异常
                }
            }
        }



    }




    protected void beforeExecute(Thread t, Runnable r) { }

    protected void afterExecute(Runnable r, Throwable t) { }
}
