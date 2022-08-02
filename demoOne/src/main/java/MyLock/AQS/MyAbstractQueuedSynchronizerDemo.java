package MyLock.AQS;


import sun.misc.Unsafe;

import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.LockSupport;


/**
 *   AQS如何保证队列活跃
 *   AQS如何保证在节点释放的同时又有新节点入队的情况下，不出现原持锁线程释放锁，后继线程被自己阻塞死的情况,保持同步队列的活跃？
 *   回答这个问题，需要理解shouldParkAfterFailedAcquire和unparkSuccessor这两个方法。
 *   以独占锁为例，后继争用线程阻塞自己的情况是读到前驱节点的等待状态为SIGNAL,只要不是这种情况都会再试着去争取锁。
 *   假设后继线程读到了前驱状态为SIGNAL，说明之前在tryAcquire的时候，前驱持锁线程还没有tryRelease完全释放掉独占锁。
 *   此时如果前驱线程完全释放掉了独占锁，则在unparkSuccessor中还没执行完置waitStatus为0的操作，也就是还没执行到下面唤醒后继线程的代码，否则后继线程会再去争取锁。
 *   那么就算后继争用线程此时把自己阻塞了，也一定会马上被前驱线程唤醒。
 *   那么是否可能持锁线程执行唤醒后继线程的逻辑时，后继线程读到前驱等待状态为SIGNAL把自己给阻塞，再也无法苏醒呢？
 *   这个问题在上面的问题3中已经有答案了，确实可能在扫描后继需要唤醒线程时读不到新来的线程，但只要tryRelease语义实现正确，在true时表示完全释放独占锁，
 *   则后继线程理应能够tryAcquire成功，shouldParkAfterFailedAcquire在读到前驱状态不为SIGNAL会给当前线程再一次获取锁的机会的。
 *   别看AQS代码写的有些复杂，状态有些多，还真的就是没毛病，各种情况都能覆盖
 */
public class MyAbstractQueuedSynchronizerDemo extends AbstractOwnableSynchronizer {


    protected MyAbstractQueuedSynchronizerDemo() {
    }


    static final long spinForTimeoutThreshold = 1000L;


    private transient volatile Node head;


    private transient volatile Node tail;


    /**
     * 锁状态
     */
    private volatile int state;


    protected final int getState() {
        return state;
    }

    protected final void setState(int newState) {
        state = newState;
    }


    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }




    public class ConditionObject{

        private transient Node firstWaiter;
        /** Last node of condition queue. */
        private transient Node lastWaiter;



        /** Mode meaning to reinterrupt on exit from wait */

        //代表 await 返回的时候，需要重新设置中断状态   signal 之后中断
        private static final int REINTERRUPT =  1;
        /** Mode meaning to throw InterruptedException on exit from wait */

        //代表 await 返回的时候，需要抛出 InterruptedException 异常  signal 之前已经中断
        private static final int THROW_IE    = -1;

        public ConditionObject() { }

        /**
         * 添加一个等待者
         * // 将当前线程对应的节点入队，插入队尾
         * @return
         */
        private Node addConditionWaiter() {

            Node t = lastWaiter;

            // If lastWaiter is cancelled, clean out.
            //// 如果条件队列的最后一个节点取消了，将其清除出去
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();  //清理节点
                t = lastWaiter;  //确定最后一个节点
            }

            /**
             * 找到最后一个等待者
             */
            Node node = new Node(Thread.currentThread(), Node.CONDITION);

            // 如果队列为空
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;    //单向链表 续上

            lastWaiter = node;
            return node;
        }


        /**
         * 该方法用于清除队列中已经取消等待的节点。
         * 等待队列是一个单向链表，遍历链表将已经取消等待的节点清除出去
         */
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                //// 如果节点的状态不是 Node.CONDITION 的话，这个节点就是被取消的
                if (t.waitStatus != Node.CONDITION) {
                    t.nextWaiter = null;   //help gc
                    //第一个等待者 添加进去
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;  //如果next 为null  时 trail 就是最后一个节点  如果不为null tail 要续上

                    //当前t节点已经是最后一个
                    if (next == null)
                        //最后一个节点为前一个
                        lastWaiter = trail;
                } else
                    trail = t;
                //next 为null 跳出循环
                t = next;
            }
        }


        /**
         * 是否在阻塞队列
         // 在节点入条件队列的时候，初始化时设置了 waitStatus = Node.CONDITION

         // signal 的时候需要将节点从条件队列移到阻塞队列，

         // 这个方法就是判断 node 是否已经移动到阻塞队列了
         */
        final boolean isOnSyncQueue(Node node) {

            // 移动过去的时候，node 的 waitStatus 会置为 0，这个之后在说 signal 方法的时候会说到
            // 如果 waitStatus 还是 Node.CONDITION，也就是 -2，那肯定就是还在条件队列中
            // 如果 node 的前驱 prev 指向还是 null，说明肯定没有在 阻塞队列

            /**
             * node.prev 赋值是在阻塞队列后才有
             */
            if (node.waitStatus == Node.CONDITION || node.prev == null)
                return false;

            // 如果 node 已经有后继节点 next 的时候，那肯定是在阻塞队列了
            if (node.next != null) // If has successor, it must be on queue
                return true;
            /*
             * node.prev can be non-null, but not yet on queue because
             * the CAS to place it on queue can fail. So we have to
             * traverse from tail to make sure it actually made it.  It
             * will always be near the tail in calls to this method, and
             * unless the CAS failed (which is unlikely), it will be
             * there, so we hardly ever traverse much.
             */

            //node.next = null
            //node.waitStatus!=CONDITION
            //node.prev!=null
            /**
             * 这个方法从阻塞队列的队尾开始从后往前遍历找，如果找到相等的，说明在阻塞队列，否则就是不在阻塞队列
             *
             * 可以通过判断 node.prev() != null 来推断出 node 在阻塞队列吗？答案是：不能。
             * 这个可以看上篇 AQS 的入队方法，
             *
             * 首先入队的时候设置的是 node.prev 指向 tail，
             * 然后是 CAS 操作将自己设置为新的 tail，可是这次的 CAS 是可能失败的。
             *
             * 调用这个方法的时候，往往我们需要的就在队尾的部分，所以一般都不需要完全遍历整个队列的
             */
            return findNodeFromTail(node);
        }


        private boolean findNodeFromTail(Node node) {
            Node t = tail;
            for (;;) {
                if (t == node)
                    return true;
                if (t == null)
                    return false;
                t = t.prev;
            }
        }


        /**
         * // 1. 如果在 signal 之前已经中断，返回 THROW_IE
         * // 2. 如果是 signal 之后中断，返回 REINTERRUPT
         * // 3. 没有发生中断，返回 0
         *
         * signal 之前还是之后 中断
         */
        private int checkInterruptWhileWaiting(Node node) {

            //REINTERRUPT： 代表 await 返回的时候，需要重新设置中断状态
            //THROW_IE： 代表 await 返回的时候，需要抛出 InterruptedException 异常
            //0 ：说明在 await 期间，没有发生中断
            return Thread.interrupted() ?
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) :
                    0;
        }


        /**
         * // 只有线程处于中断状态，才会调用此方法
         * // 如果需要的话，将这个已经取消等待的节点转移到阻塞队列
         * // 返回 true：如果此线程在 signal 之前被取消，
         * @param node
         * @return
         */
        final boolean transferAfterCancelledWait(Node node) {
            // 用 CAS 将节点状态设置为 0
            // 如果这步 CAS 成功，说明是 signal 方法之前发生的中断，因为如果 signal 先发生的话，signal 中会将 waitStatus 设置为 0
            if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {

                /**
                 *  将节点放入阻塞队列
                 *  这里我们看到，即使中断了，依然会转移到阻塞队列
                 */
                enq(node);
                return true;
            }
            /*
             * If we lost out to a signal(), then we can't proceed
             * until it finishes its enq().  Cancelling during an
             * incomplete transfer is both rare and transient, so just
             * spin.
             */

            /**
             *  到这里是因为 CAS 失败，肯定是因为 signal 方法已经将 waitStatus 设置为了 0
             *  signal 方法会将节点转移到阻塞队列，但是可能还没完成，这边自旋等待其完成
             *  当然，这种事情还是比较少的吧：signal 调用之后，没完成转移之前，发生了中断
             */

            //要么中断，要么转移成功
            while (!isOnSyncQueue(node))
                Thread.yield();
            return false;

        }

        public final void awaitUninterruptibly(){
            Node  node = addConditionWaiter();

            int savedState = fullyRelease(node);

            boolean interrupted = false;

            while(!isOnSyncQueue(node)){
                LockSupport.park(this);
                if(Thread.interrupted())
                    interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted)
                selfInterrupt();
        }

        public final long awaitNanos(long nanosTimeout)
                throws InterruptedException{
            if (Thread.interrupted())
            throw new InterruptedException();

            Node node = addConditionWaiter();

            int savedState = fullyRelease(node);

            // 当前时间 + 等待时长 = 过期时间
            final long deadline =  System.nanoTime()+nanosTimeout;

            int interruptMode = 0;

            while (!isOnSyncQueue(node)) {

                if(nanosTimeout<=0){

                    // 这里因为要 break 取消等待了。取消等待的话一定要调用 transferAfterCancelledWait(node) 这个方法
                    // 如果这个方法返回 true，在这个方法内，将节点转移到阻塞队列成功
                    // 返回 false 的话，说明 signal 已经发生，signal 方法将节点转移了。也就是说没有超时嘛
                    transferAfterCancelledWait(node);
                    break;
                }

                // 如果不到 1 毫秒了，那就不要选择 parkNanos 了，自旋的性能反而更好
                if(nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this,nanosTimeout);

                if((interruptMode = checkInterruptWhileWaiting(node))!=0)
                    break;
                nanosTimeout = deadline - System.nanoTime();
            }

            if(acquireQueued(node,savedState) && interruptMode !=THROW_IE)
                interruptMode = REINTERRUPT;
            if(node.nextWaiter != null)
                unlinkCancelledWaiters();
            if(interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }


        /**
         * // 首先，这个方法是可被中断的，不可被中断的是另一个方法 awaitUninterruptibly()
         * // 这个方法会阻塞，直到调用 signal 方法（指 signal() 和 signalAll()，下同），或被中断
         */
        public final void await() throws InterruptedException {
            if (Thread.interrupted())
                throw new InterruptedException();


            // 添加到 condition 的条件队列中
            Node node = addConditionWaiter();

            // 释放锁，返回值是释放锁之前的 state 值
            int savedState = fullyRelease(node);


            /**
             * interruptMode。interruptMode 可以取值为 REINTERRUPT（1），THROW_IE（-1），0
             *
             * REINTERRUPT： 代表 await 返回的时候，需要重新设置中断状态
             * THROW_IE： 代表 await 返回的时候，需要抛出 InterruptedException 异常
             * 0 ：说明在 await 期间，没有发生中断
             * 有以下三种情况会让 LockSupport.park(this); 这句返回继续往下执行：
             *
             * 常规路劲。signal -> 转移节点到阻塞队列 -> 获取了锁（unpark）
             * 线程中断。在 park 的时候，另外一个线程对这个线程进行了中断
             * signal 的时候我们说过，转移以后的前驱节点取消了，或者对前驱节点的CAS操作失败了
             * 假唤醒。这个也是存在的，和 Object.wait() 类似，都有这个问题
             *
             */
            int interruptMode = 0;


            /** 这里退出循环有两种情况，之后再仔细分析
             *
             1. isOnSyncQueue(node) 返回 true，即当前 node 已经转移到阻塞队列了
             2. checkInterruptWhileWaiting(node) != 0 会到 break，然后退出循环，代表的是线程中断

             是否在阻塞队列中
             */
            while (!isOnSyncQueue(node)) {
                /**
                 * 不再阻塞队列中 挂起线程
                 * 这边会自旋，如果发现自己还没到阻塞队列，那么挂起，等待被转移到阻塞队列
                 */
                //锁住
                LockSupport.park(this);


                /**
                 * 阻塞队列中的前驱节点取消等待，或者 CAS 失败  唤醒线程
                 *
                 * 1 常规路劲。signal -> 转移节点到阻塞队列 -> 获取了锁（unpark）
                 * 2 线程中断。在 park 的时候，另外一个线程对这个线程进行了中断
                 * 3 signal 的时候我们说过，转移以后的前驱节点取消了，或者对前驱节点的CAS操作失败了
                 *   再次循环 锁住
                 * 4 假唤醒。这个也是存在的，和 Object.wait() 类似，都有这个问题
                 */
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }

            /**
             * 1 在signal转移阻塞队列成功 前节点没有被取消  阻塞在 while (!isOnSyncQueue(node))中 等待唤醒
             * 2 在 signal 初 转移到阻塞队列后 前节点状态没设置成功-1  会跳出循环 走到这
             * acquireQueued 获取锁 中 把前节点状态设置为-1 然后再次抢锁
             */


            // 被唤醒后，将进入阻塞队列，等待获取锁

            /**
             * 不管有没有发生中断，都会进入到阻塞队列，
             * 而 acquireQueued(node, savedState) 的返回值就是代表线程是否被中断。
             * 如果返回 true，说明被中断了，而且 interruptMode != THROW_IE，
             * 说明在 signal 之前就发生中断了，这里将 interruptMode 设置为 REINTERRUPT，用于待会重新中断。
             */

            // 阻塞队列的自旋   重新获取锁
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;
            /**
             *  signal 的时候会将节点转移到阻塞队列，有一步是 node.nextWaiter = null，将断开节点和条件队列的联系
             *
             *  如果 signal 之前就中断了，也需要将节点进行转移到阻塞队列，这部分转移的时候，
             *  是没有设置 node.nextWaiter = null 的
             */
            if (node.nextWaiter != null) // clean up if cancelled
                unlinkCancelledWaiters();
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }



        // 唤醒等待了最久的线程
        // 其实就是，将这个线程对应的 node 从条件队列转移到阻塞队列
        public final void signal() {
            // 调用 signal 方法的线程必须持有当前的独占锁
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();


            Node first = firstWaiter;

            if (first != null)
                doSignal(first);
        }


        /**
         // 从条件队列队头往后遍历，找出第一个需要转移的 node
         // 因为前面我们说过，有些线程会取消排队，但是还在队列中
         */
        private void doSignal(Node first) {
            do {
                // 将 firstWaiter 指向 first 节点后面的第一个
                // 如果将队头移除后，后面没有节点在等待了，那么需要将 lastWaiter 置为 null
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;

                // 因为 first 马上要被移到阻塞队列了，和条件队列的链接关系在这里断掉
                first.nextWaiter = null;
            }
            /**
             * 这里 while 循环，如果 first 转移不成功，那么选择 first 后面的第一个节点进行转移，依此类推
             */
            while (!transferForSignal(first) &&
                    (first = firstWaiter) != null);
        }


        /**
         * // 将节点从条件队列转移到阻塞队列
         * // true 代表成功转移
         * // false 代表在 signal 之前，节点已经取消了
         */
        final boolean transferForSignal(Node node) {
            /*
             * If cannot change waitStatus, the node has been cancelled.
             */

            // CAS 如果失败，说明此 node 的 waitStatus 已不是 Node.CONDITION，说明节点已经取消，
            // 既然已经取消，也就不需要转移了，方法返回，转移后面一个节点
            // 否则，将 waitStatus 置为 0
            if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
                return false;

            /*
             * Splice onto queue and try to set waitStatus of predecessor to
             * indicate that thread is (probably) waiting. If cancelled or
             * attempt to set waitStatus fails, wake up to resync (in which
             * case the waitStatus can be transiently and harmlessly wrong).
             */

            /**
             * // enq(node): 自旋进入阻塞队列的队尾
             * // 注意，这里的返回值 p 是 node 在阻塞队列的前驱节点
             */
            Node p = enq(node);
            int ws = p.waitStatus;

            // ws > 0 说明 node 在阻塞队列中的前驱节点取消了等待锁，直接唤醒 node 对应的线程。唤醒之后会怎么样，后面再解释

            // 如果 ws <= 0, 那么 compareAndSetWaitStatus 将会被调用，上篇介绍的时候说过，节点入队后，需要把前驱节点的状态设为 Node.SIGNAL(-1)


            /**
             * 正常情况下，ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL) 这句中，
             * ws <= 0，而且 compareAndSetWaitStatus(p, ws, Node.SIGNAL) 会返回 true，
             * 所以一般也不会进去 if 语句块中唤醒 node 对应的线程
             *
             *
             *
             * wc>0 ==false 会执行 compareAndSetWaitStatus(p, ws, Node.SIGNAL) 设置前节点为唤醒节点
             *
             */
            if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
                // 如果前驱节点取消或者 CAS 失败，会进到这里唤醒线程
                LockSupport.unpark(node.thread);

            return true;
        }


        private void reportInterruptAfterWait(int interruptMode)
                throws InterruptedException {
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            else if (interruptMode == REINTERRUPT)
                selfInterrupt();
        }


        /**
         * // 首先，我们要先观察到返回值 savedState 代表 release 之前的 state 值
         * // 对于最简单的操作：先 lock.lock()，然后 condition1.await()。
         * //         那么 state 经过这个方法由 1 变为 0，锁释放，此方法返回 1
         * //         相应的，如果 lock 重入了 n 次，savedState == n
         * // 如果这个方法失败，会将节点设置为"取消"状态，并抛出异常 IllegalMonitorStateException
         */
        final int fullyRelease(Node node) {
            boolean failed = true;
            try {
                //获取当前锁状态 数量
                int savedState = getState();

                /**
                 * 释放锁 // 这里使用了当前的 state 作为 release 的参数，也就是完全释放掉锁，将 state 置为 0
                 *
                 * 唤醒后续节点竞争
                 */
                if (release(savedState)) {
                    failed = false;
                    return savedState;
                } else {

                    //大致意思就是说抛出这个异常表明线程尝试等待一个对象的监视器或者去通知其他正在等待这个对象监视器的线程时，但是没有拥有这个监视器的所有权。
                    throw new IllegalMonitorStateException();
                }
            } finally {
                //更新失败
                if (failed)
                    node.waitStatus = Node.CANCELLED;
            }
        }
    }

    /**
     * 子类实现
     * @return
     */
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

     static  final  class Node {

        Node() {    // Used to establish initial head or SHARED marker
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }

        Node(Thread thread, Node node) {
            nextWaiter = node;
            this.thread = thread;
        }

         static final Node SHARED = new Node();

         static final Node EXCLUSIVE = null;


        /**
         * 结点状态
         * CANCELLED，值为1，表示当前的线程被取消
         * SIGNAL，值为-1，表示当前节点的后继节点包含的线程需要运行，也就是unpark
         * CONDITION，值为-2，表示当前节点在等待condition，也就是在condition队列中
         * PROPAGATE，值为-3，表示当前场景下后续的acquireShared能够得以执行
         * 值为0，表示当前节点在sync队列中，等待着获取锁
         */
        static final int CANCELLED = 1;

        static final int SIGNAL = -1;

        static final int CONDITION = -2;

        static final int PROPAGATE = -3;


        //等待状态
        volatile int waitStatus;

        //前节点
        volatile Node prev;

        //后节点
        volatile Node next;

        //线程
        volatile Thread thread;

        // 下一个等待者
        public Node nextWaiter = null;


        // 结点是否在共享模式下等待
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        //获取前驱节点
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

    }


    /**
     * 获取独占锁，对中断不敏感。
     * 首先尝试获取一次锁，如果成功，则返回；
     * 否则会把当前线程包装成Node插入到队列中，在队列中会检测是否为head的直接后继，并尝试获取锁,
     * 如果获取失败，则会通过LockSupport阻塞当前线程，直至被释放锁的线程唤醒或者被中断，随后再次尝试获取锁，如此反复。
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }


    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    private boolean acquireQueued(Node node, int arg) {
        boolean failed = true;

        try{
            //是否中断
            boolean interrupted = false;
            for(;;){
                //1 获取前节点
                final Node p = node.predecessor();

                //2 前节点是头部节点  就尝试获取锁
                if(p == head && tryAcquire(arg)){

                    // 前继出队，node成为head
                    setHead(node);

                    p.next = null;

                    failed = false;

                    return  interrupted;
                }

                /**
                 *  p != head 或者 p == head但是tryAcquire失败了，那么
                 *  应该阻塞当前线程等待前继唤醒。阻塞之前会再重试一次，还需要设置前继的waitStaus为SIGNAL。
                 *
                 *  线程会阻塞在parkAndCheckInterrupt方法中。
                 *  parkAndCheckInterrupt返回可能是前继unpark或线程被中断。
                 */

                //没有得到锁时：
                //shouldParkAfterFailedAcquire方法：返回是否需要阻塞当前线程
                //parkAndCheckInterrupt方法：阻塞当前线程，当线程再次唤醒时，返回是否被中断

                /**
                 * 如果未成功获取锁则根据前驱节点判断是否要阻塞。
                 * 如果阻塞过程中被中断，则置interrupted标志位为true。
                 * shouldParkAfterFailedAcquire方法在前驱状态不为SIGNAL的情况下都会循环重试获取锁。
                 */


                /**
                 *  // 解释下为什么shouldParkAfterFailedAcquire(p, node)返回false的时候不直接挂起线程：
                 *     // => 是为了应对在经过这个方法后，node已经是head的直接后继节点了。剩下的读者自己想想吧。
                 */
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())

                /**
                 * parkAndCheckInterrupt() 返回true 说明是中断唤醒的
                 * // 说明当前线程是被中断唤醒的。
                 * //
                 * 注意：线程被中断之后会继续走到if处去判断，也就是会忽视中断。
                 * // 除非碰巧线程中断后acquire成功了，那么根据Java的最佳实践，
                 * // 需要重新设置线程的中断状态（acquire.selfInterrupt）。
                 */
                    interrupted = true;
            }

        }finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    private void cancelAcquire(Node node) {
        if(node == null)
            return;
        node.thread = null;

        Node pred = node.prev;

        /**
         * 前节点被取消 就过滤掉
         * // 遍历并更新节点前驱，把node的prev指向前部第一个非取消节点。
         */
        while(pred.waitStatus > 0)
            node.prev = pred = pred.prev;


        //找到前节点的 next节点 cas 操作
        // 记录pred节点的后继为predNext，后续CAS会用到。
        Node predNext = pred.next;


        // 直接把当前节点的等待状态置为取消,后继节点即便也在cancel可以跨越node节点。
        node.waitStatus = Node.CANCELLED;


        /**
         * 如果当前node 是 tail 节点 就设置 pred节点为 tail节点
         */

        /**
         * 如果CAS将tail从node置为pred节点了
         * 则剩下要做的事情就是尝试用CAS将pred节点的next更新为null以彻底切断pred和node的联系。
         * 这样一来就断开了pred与pred的所有后继节点，这些节点由于变得不可达，最终会被回收掉。
         * 由于node没有后继节点，所以这种情况到这里整个cancel就算是处理完毕了。
         *
         * 这里的CAS更新pred的next即使失败了也没关系，说明有其它新入队线程或者其它取消线程更新掉了。
         */

        if(node == tail && compareAndSetTail(node,pred)){
            //成果后 设置pred节点next 为null    pred 为tail 节点
            compareAndSetNext(pred,predNext,null);
        }else{
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.


            /**
             *  如果node还有后继节点，这种情况要做的事情是把pred和后继非取消节点拼起来。
              */


            /**
             * 1  设置尾节点失败       说明有其他线程设置为节点了
             *
             * 2  当前节点不是尾节点    需要把当前节点的next 移到pred 节点的next
             *
             */
            int ws;

            //前一个节点不是头节点
            if(pred != head
                    //pred节点状态改为SIGNAL 成功
                    && ((ws = pred.waitStatus) == Node.SIGNAL
                    ||(ws<=0 && compareAndSetWaitStatus(pred,ws, Node.SIGNAL)))
                && pred.thread !=null){

                /**
                 * next节点 续在pred节点后
                 */
                Node next = node.next;
                /**
                 * 如果node的后继节点next非取消状态的话，则用CAS尝试把pred的后继置为node的后继节点
                 * 这里if条件为false或者CAS失败都没关系，这说明可能有多个线程在取消，总归会有一个能成功的。
                 */
                if(next !=null&&next.waitStatus<=0)
                    //设置pred节点 next 为 node.next
                    compareAndSetNext(pred,predNext,next);
            }
            //1 前一节点是头节点     2  pred节点状态不为SIGNAL 或者设置失败

            else {

                /**
                 * 这时说明pred == head或者pred状态取消或者pred.thread == null
                 * 在这些情况下为了保证队列的活跃性，需要去唤醒一次后继线程。
                 *
                 * 举例来说pred == head完全有可能实际上目前已经没有线程持有锁了，
                 * 自然就不会有释放锁唤醒后继的动作。如果不唤醒后继，队列就挂掉了。
                 *
                 * 这种情况下看似由于没有更新pred的next的操作，队列中可能会留有一大把的取消节点。
                 * 实际上不要紧，因为后继线程唤醒之后会走一次试获取锁的过程，
                 * 失败的话会走到shouldParkAfterFailedAcquire的逻辑。
                 * 那里面的if中有处理前驱节点如果为取消则维护pred/next,踢掉这些取消节点的逻辑。
                 */
                unparkSuccessor(node);
            }


            /*
             * 取消节点的next之所以设置为自己本身而不是null,
             * 是为了方便AQS中Condition部分的isOnSyncQueue方法,
             * 判断一个原先属于条件队列的节点是否转移到了同步队列。
             *
             * 因为同步队列中会用到节点的next域，取消节点的next也有值的话，
             * 可以断言next域有值的节点一定在同步队列上。
             *
             * 在GC层面，和设置为null具有相同的效果。
             */
            node.next = node; // help GC
        }

    }


    /**
     * 时刻1: node -> tail && tail.waitStatus == Node.CANCELLED (node的下一个节点为tail，并且tail处于取消状态)
     * 时刻2: unparkSuccessor读到s.waitStatus > 0
     * 时刻3: unparkSuccessor从tail开始遍历
     * 时刻4: tail节点对应线程执行cancelAcquire方法中的if (node == tail && compareAndSetTail(node, pred)) 返回true,
     * 此时tail变为pred(也就是node)
     * 时刻5: 有新线程进队列tail变为新节点
     * 时刻6: unparkSuccessor没有发现需要唤醒的节点
     * 最终新节点阻塞并且前驱节点结束调用，新节点再也无法被unpark
     *
     * 这种情况不会发生,确实可能出现从tail向前扫描，没有读到新入队的节点，
     * 但别忘了acquireQueued的思想就是不断循环检测是否能够独占获取锁，
     *
     *
     * 否则再进行判断是否要阻塞自己，而release的第一步就是tryRelease，它的语义为true表示完全释放独占锁，
     * 完全释放之后才会执行后面的逻辑，也就是unpark后继线程。在这种情况下，新入队的线程应当能获取到锁。
     * 如果没有获取锁，则必然是在覆盖tryAcquire/tryRelease的实现有问题，导致前驱节点成功释放了独占锁，
     * 后继节点获取独占锁仍然失败。也就是说AQS框架的可靠性还在
     * 某些程度上依赖于具体子类的实现，子类实现如果有bug，那AQS再精巧也扛不住。
     *
     *  ，确实可能在扫描后继需要唤醒线程时读不到新来的线程，但只要tryRelease语义实现正确，在true时表示完全释放独占锁，
     *  则后继线程理应能够tryAcquire成功，shouldParkAfterFailedAcquire在读到前驱状态不为SIGNAL会给当前线程再一次获取锁的机会的
     */
    /**
     * 唤醒下一个线程
     * @param node
     */
    private void unparkSuccessor(Node node) {
        int ws = node.waitStatus;
        // 尝试将node的等待状态置为0,这样的话,后继争用线程可以有机会再尝试获取一次锁。
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);


        //后继节点
        Node s = node.next;

        //如果s被取消，跳过被取消节点

        /**
         * 这里的逻辑就是如果node.next存在并且状态不为取消，则直接唤醒s即可
         * 否则需要从tail开始向前找到node之后最近的非取消节点。
         *
         * 这里为什么要从tail开始向前查找也是值得琢磨的:
         * 如果读到s == null，不代表node就为tail，参考addWaiter以及enq函数中的我的注释。
         * 不妨考虑到如下场景：
         * 1. node某时刻为tail
         * 2. 有新线程通过addWaiter中的if分支或者enq方法添加自己
         * 3. compareAndSetTail成功
         * 4. 此时这里的Node s = node.next读出来s == null，但事实上node已经不是tail，它有后继了!
         */
        if(s ==null ||s.waitStatus>0){
            s = null;  //gc

            /**
             * 从尾节点往前找
             * 一直找到 node 节点
             * 找到第一个没有取消的节点
             */
            for(Node t = tail; t!= null&&t!=node;t =t.prev)
                if(t.waitStatus <=0)
                     s =t;
        }

        /**
         * 唤醒一个节点
         */
        if(s!=null){
            LockSupport.unpark(s.thread);
        }



    }

    /**
     * 线程被唤醒只可能是：被unpark，被中断或伪唤醒。被中断会设置interrupted，
     * acquire方法返回前会 selfInterrupt重置下线程的中断状态，如果是伪唤醒的话会for循环re-check
     * @return
     *
     * 1、如果一个线程 park 了，那么调用 unpark(thread) 这个线程会被唤醒；
     *
     * 2、如果一个线程先被调用了 unpark，那么下一个 park(thread) 操作不会挂起线程。
     *
     */
    private boolean parkAndCheckInterrupt() {
        /**
         * LockSupport 响应中断
         * LockSupport.unpark 也解锁   但无中断  此时线程中断状态为false
         */
        LockSupport.park(this);

        return Thread.interrupted();
    }

    private boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        //获取前一个节点的状态
        int ws = pred.waitStatus;

        if(ws == Node.SIGNAL){
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             * SIGNAL状态的节点，释放锁后，会唤醒其后继节点。
             * 因此，此线程可以安全的阻塞（前驱节点释放锁时，会唤醒此线程)。
             */
            return true;
        }

        //前驱节点对应的线程被取消
        if(ws > 0 ){
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */

            do{
                //跳过此前驱节点
                node.prev = pred = pred.prev;
//               node.prev = pred.prev;
//               pred.prev.next = node;
            }while(pred.waitStatus>0);

            pred.next = node;
        }else {
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             * 设置为SIGNAL -1
             */

            /**
             * 是设置为 PROPAGATE，那么新的节点在 shouldParkAfterFailedAcquire 回来的时候是 false，
             * 也就不会挂起，而会进到下一个 for 循环
             *
             * 设置为 sinnal 后 返回false  不阻塞
             */

            //
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        return false;
    }

    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }

    /**
     * 添加一个节点到尾节点
     * @param mode
     * @return
     */
    private Node addWaiter(Node mode){
        Node node = new Node(Thread.currentThread(), mode);

        Node pred = tail;

        /**
         *  尾节点不为null 续在尾节点后边
         */
        if(pred != null){
            node.prev = pred;

            /**
             * ddWaiter和enq方法中新增一个节点时为什么要先将新节点的prev置为tail再尝试CAS，
             * 而不是CAS成功后来构造节点之间的双向链接?
             *
             * 这是因为，双向链表目前没有基于CAS原子插入的手段，
             * 如果我们将node.prev = t和t.next = node（t为方法执行时读到的tail，引用封闭在栈上）
             * 放到compareAndSetTail(t, node)成功后执行
             *
             *
             * 会导致这一瞬间的tail也就是t的prev为null，这就使得这一瞬间队列处于一种不一致的中间状态。
             *
             * 新的尾节点  pre 还没有建立连接 null
             */
            if(compareAndSetTail(pred,node)){
                pred.next = node;
                return node;
            }
        }

        enq(node);

        return node;
    }


    /**
     *  //返回之前的一个节点
     * @param node
     * @return
     */
    private Node enq(Node node) {

        for(;;){
            Node t = tail;

            if(t == null){
                /**
                 * 初始化头节点
                 */
                if(compareAndSetHead(new Node()))
                    tail = head;
            }else {
                node.prev = t;

                if(compareAndSetTail(t,node)){
                    t.next = node;
                    //返回之前的一个节点
                    return t;
                }

            }




        }


    }


    /**
     * 公平锁
     * @param acquires
     * @return
     */
    protected final boolean fairtryAcquire(int acquires) {
        final Thread current = Thread.currentThread();
        int c = getState();

        /**
         * 当锁释放的时候判断
         */
        if (c == 0) {
            if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0)
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }

    /**
     * 判断 是否在队首
     *
     * 判断“当前线程”是不是CLH队列中的第一个线程
     * @return
     */
    public final boolean hasQueuedPredecessors() {
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        return h != t &&
                ((s = h.next) == null || s.thread != Thread.currentThread());
    }

    /**
     * 留给子类实现   不公平实现
     * @param acquires
     * @return
     */
    protected boolean tryAcquire(int acquires) {
        final Thread current = Thread.currentThread();

        int c = getState();
        /**
         * 0 的时候可以获取
         */
        if (c == 0) {
            if (compareAndSetState(0, acquires)) {
                setExclusiveOwnerThread(current);
                return true;
            }
        }
        /**
         * 是独占线程 设置aqs的锁的数量
         */
        else if (current == getExclusiveOwnerThread()) {
            int nextc = c + acquires;
            if (nextc < 0) // overflow
                throw new Error("Maximum lock count exceeded");
            setState(nextc);
            return true;
        }
        return false;
    }





    public final boolean release(int arg) {

        //修改锁计数器，如果计数器为0,说明锁被释放
        if (tryRelease(arg)) {

            /**
             * 此时的head节点可能有3种情况:
             * 1. null (AQS的head延迟初始化+无竞争的情况)
             * 2. 当前线程在获取锁时new出来的节点通过setHead设置的
             * 3. 由于通过tryRelease已经完全释放掉了独占锁，有新的节点在acquireQueued中获取到了独占锁，并设置了head

             * 第三种情况可以再分为两种情况：
             * （一）时刻1:线程A通过acquireQueued，持锁成功，set了head
             *          时刻2:线程B通过tryAcquire试图获取独占锁失败失败，进入acquiredQueued
             *          时刻3:线程A通过tryRelease释放了独占锁
             *          时刻4:线程B通过acquireQueued中的tryAcquire获取到了独占锁并调用setHead
             *          时刻5:线程A读到了此时的head实际上是线程B对应的node
             *                然后unparkSuccessor head
             * （二）时刻1:线程A通过tryAcquire直接持锁成功，head为null
             *          时刻2:线程B通过tryAcquire试图获取独占锁失败失败，入队过程中初始化了head，进入acquiredQueued
             *          时刻3:线程A通过tryRelease释放了独占锁，此时线程B还未开始tryAcquire
             *          时刻4:线程A读到了此时的head实际上是线程B初始化出来的傀儡head
             */

            /**
             * head 为当前持有锁的节点
             */
            Node h = head;

            /**
             * 0 为等待状态
             * //head节点的waitStatus不等于0，说明head节点的后继节点对应的线程，正在阻塞，等待被唤醒
             *
             *  head节点状态不会是CANCELLED，所以这里h.waitStatus != 0相当于h.waitStatus < 0
             */
            if(h!=null && h.waitStatus!=0)
                unparkSuccessor(h);
            return true;
        }

        return false;
    }

    private boolean tryRelease(int releases) {
        int c = getState() - releases;

        /**
         * 是否是独占线程
         */
        if (Thread.currentThread() != getExclusiveOwnerThread())
            throw new IllegalMonitorStateException();


        boolean free = false;
        /**
         * 设为状态0 释放独占
         */
        if (c == 0) {
            free = true;
            setExclusiveOwnerThread(null);
        }
        setState(c);
        return free;
    }

    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
                doAcquireNanos(arg, nanosTimeout);
    }



    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException{
        if(nanosTimeout <= 0L)
            return false;
        final long deadline = System.currentTimeMillis() +nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;

        try{


            /**
             * acquireQueued
             *
             * for (;;) {
             *                 final Node p = node.predecessor();
             *                 if (p == head && tryAcquire(arg)) {
             *                     setHead(node);
             *                     p.next = null; // help GC
             *                     failed = false;
             *                     return interrupted;
             *                 }
             *                 if (shouldParkAfterFailedAcquire(p, node) &&
             *                     parkAndCheckInterrupt())
             *                     interrupted = true;
             *             }
             *
             *
             *  doAcquireInterruptibly
             *
             *
             *       for (;;) {
             *                 final Node p = node.predecessor();
             *                 if (p == head && tryAcquire(arg)) {
             *                     setHead(node);
             *                     p.next = null; // help GC
             *                     failed = false;
             *                     return;
             *                 }
             *                 if (shouldParkAfterFailedAcquire(p, node) &&
             *                     parkAndCheckInterrupt())
             *                     throw new InterruptedException();
             *             }
             *
             */
            for(;;){
                //前驱节点
                final Node p = node.predecessor();

                if(p == head && tryAcquire(arg)){
                    setHead(node);

                    p.next = null; //gc

                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();

                if(nanosTimeout <= 0l)
                    return false;
                if(shouldParkAfterFailedAcquire(p,node)&&
                        nanosTimeout>spinForTimeoutThreshold)
                    LockSupport.parkNanos(this,nanosTimeout);
                if(Thread.interrupted())
                    throw new InterruptedException();
            }
        }finally {
            if(failed)
                cancelAcquire(node);
        }
    }


//************************************************共享锁*******************************************************************


    /**
     * 实现tryAcquireShared方法时需要注意，返回负数表示获取失败;返回0表示成功，但是后继争用线程不会成功;返回正数表示
     * 获取成功，并且后继争用线程也可能成功。
     * @param arg
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }



    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            // doReleaseShared的实现上面获取共享锁已经介绍
            doReleaseShared();
            return true;
        }
        return false;
    }

    /**
     * 子类实现
     * @param arg
     * @return
     */
    private boolean tryReleaseShared(int arg) {
        return true;
    }


    private void doAcquireShared(int arg) {

        final Node node = addWaiter(Node.SHARED);

        boolean failed = true;

        try{
            boolean interrupted = false;
            for(;;){
                final Node p = node.predecessor(); //前节点

                if(p == head ){

                    int r =tryAcquireShared(arg);

                    //>0获取成功  // 一旦共享获取成功，设置新的头结点，并且唤醒后继线程
                    if(r>0){
                        setHeadAndPropagate(node,r);
                        p.next =null; //gc
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;

                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    interrupted = true;
            }


        }finally {
            if (failed)
                cancelAcquire(node);
        }

    }

    /**
     * 这个函数做的事情有两件:
     * 1. 在获取共享锁成功后，设置head节点
     * 2. 根据调用tryAcquireShared返回的状态以及节点本身的等待状态来判断是否要需要唤醒后继线程。
     */
    private void setHeadAndPropagate(Node node, int propagate) {
        // 把当前的head封闭在方法栈上，用以下面的条件检查。
        Node h = head; // Record old head for check below
        setHead(node);


        /**
         * propagate是tryAcquireShared的返回值，这是决定是否传播唤醒的依据之一。
         *
         * h.waitStatus为SIGNAL或者PROPAGATE时也根据node的下一个节点共享来决定是否传播唤醒，
         *
         * 这里为什么不能只用propagate > 0来决定是否可以传播在本文下面的思考问题中有相关讲述。
         *
         *
         */
        if(propagate > 0||h == null|| h.waitStatus <0||
                (h = head) == null||h.waitStatus <0){
            Node s = node.next;
            /**
             * 释放共享锁
             */
            if(s ==null || s.isShared())
                doReleaseShared();
        }

    }

    /**
     * 这是共享锁中的核心唤醒函数，主要做的事情就是唤醒下一个线程或者设置传播状态。
     * 后继线程被唤醒后，会尝试获取共享锁，如果成功之后，则又会调用setHeadAndPropagate,将唤醒传播下去。
     *
     * 这个函数的作用是保障在acquire和release存在竞争的情况下，保证队列中处于等待状态的节点能够有办法被唤醒。
     */


    /**
     * releaseShared有竞争的情况下，可能会有队列中处于等待状态的节点因为第一个线程完成释放唤醒，第二个线程获取到锁，但还没设置好head，又有新线程释放锁，但是读到老的head状态为0导致释放但不唤醒，最终后一个等待线程既没有被释放线程唤醒，也没有被持锁线程唤醒。
     *
     * 所以，仅仅靠tryAcquireShared的返回值来决定是否要将唤醒传递下去是不充分的。

     */
    private void doReleaseShared() {
        /**
         * 以下的循环做的事情就是，在队列存在后继线程的情况下，唤醒后继线程；
         *
         * 或者由于多线程同时释放共享锁由于处在中间过程，
         *
         * 读到head节点等待状态为0的情况下，
         *
         * 虽然不能unparkSuccessor，但为了保证唤醒能够正确稳固传递下去，设置节点状态为PROPAGATE。
         * 这样的话获取锁的线程在执行setHeadAndPropagate时可以读到PROPAGATE，从而由获取锁的线程去释放后继等待线程。
         */

        for (;;) {
            Node h = head;
            // 如果队列中存在后继线程。

            // 1. h == null: 说明阻塞队列为空
            // 2. h == tail: 说明头结点可能是刚刚初始化的头节点，
            //   或者是普通线程节点，但是此节点既然是头节点了，那么代表已经被唤醒了，阻塞队列没有其他节点了
            // 所以这两种情况不需要进行唤醒后继节点

            // 如果队列中存在后继线程。
            if (h != null && h != tail) {

                int ws = h.waitStatus;

                /**
                 * 有需要唤醒的线程  设置为0
                 */
                if (ws == Node.SIGNAL) {
                    //cas成功 解锁
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;
                    unparkSuccessor(h);
                }
                // 如果h节点的状态为0，需要设置为PROPAGATE用以保证唤醒的传播。
                //  // 这个 CAS 失败的场景是：执行到这里的时候，刚好有一个节点入队，入队会将这个 ws 设置为 -1

                /**
                 * 多个线程释放  导致head 状态 ==0
                 *
                 * 如果h节点的状态为0，需要设置为PROPAGATE用以保证唤醒的传播。
                 */
                else if (ws == 0 &&
                        !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;
            }
            // 检查h是否仍然是head，如果不是的话需要再进行循环。

            // 如果到这里的时候，前面唤醒的线程已经占领了 head，那么再循环
            // 否则，就是 head 没变，那么退出循环，
            // 退出循环是不是意味着阻塞队列中的其他节点就不唤醒了？当然不是，唤醒的线程之后还是会调用这个方法的

            if (h == head)
                break;
        }



    }

    /**
     * 共享锁实现
     * @param arg
     * @return
     */
    private int tryAcquireShared(int arg) {
        return 0;
    }

    public final void acquireSharedInterruptibly (int arg) throws InterruptedException{

        if(Thread.interrupted())
            throw new InterruptedException();

        if(tryAcquireShared((arg))<0)
            doAcquireSharedInterruptibly(arg);


    }


    private void doAcquireSharedInterruptibly(int arg)  throws InterruptedException{

        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;

        try{
            for(;;){
                final Node p = node.predecessor();

                if(p == head){

                    // 只要 state 不等于 0，那么这个方法返回 -1
                    int r = tryAcquireShared(arg);

                    if(r>=0){
                        setHeadAndPropagate(node,r);
                        p.next = null;
                        failed =false;
                        return;
                    }
                }

                if(shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        }finally {
            if (failed)
                cancelAcquire(node);
        }


    }


    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                    (MyAbstractQueuedSynchronizerDemo.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                    (MyAbstractQueuedSynchronizerDemo.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                    (MyAbstractQueuedSynchronizerDemo.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                expect, update);
    }

    /**
     * CAS next field of a node.
     */
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
