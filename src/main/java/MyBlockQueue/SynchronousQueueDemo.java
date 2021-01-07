package MyBlockQueue;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.LockSupport;

public class SynchronousQueueDemo<E> {

    static final int NCPUS = Runtime.getRuntime().availableProcessors();
    static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;

    static final int maxUntimedSpins = maxTimedSpins * 16;

    static final long spinForTimeoutThreshold = 1000L;


    abstract static class Transferer<E> {
        /**
         * Performs a put or take.
         *
         * @param e if non-null, the item to be handed to a consumer;
         *          if null, requests that transfer return an item
         *          offered by producer.
         * @param timed if this operation should timeout
         * @param nanos the timeout, in nanoseconds
         * @return if non-null, the item provided or received; if null,
         *         the operation failed due to timeout or interrupt --
         *         the caller can distinguish which of these occurred
         *         by checking Thread.interrupted.
         */


        /**
         // 从方法名上大概就知道，这个方法用于转移元素，从生产者手上转到消费者手上
         // 也可以被动地，消费者调用这个方法来从生产者手上取元素
         // 第一个参数 e 如果不是 null，代表场景为：将元素从生产者转移给消费者
         // 如果是 null，代表消费者等待生产者提供元素，然后返回值就是相应的生产者提供的元素
         // 第二个参数代表是否设置超时，如果设置超时，超时时间是第三个参数的值
         // 返回值如果是 null，代表超时，或者中断。具体是哪个，可以通过检测中断状态得到。
         */
        abstract E transfer(E e, boolean timed, long nanos);
    }

    /**
     * 当调用这个方法时，如果队列是空的，或者队列中的节点和当前的线程操作类型一致（如当前操作是 put 操作，而栈中的元素也都是写线程）。
     * 这种情况下，将当前线程加入到等待栈中，等待配对。然后返回相应的元素，或者如果被取消了的话，返回 null。
     * 如果栈中有等待节点，而且与当前操作可以匹配（如栈里面都是读操作线程，当前线程是写操作线程，反之亦然）。
     * 将当前节点压入栈顶，和栈中的节点进行匹配，然后将这两个节点出栈。配对和出栈的动作其实也不是必须的，因为下面的一条会执行同样的事情。
     * 如果栈顶是进行匹配而入栈的节点，帮助其进行匹配并出栈，然后再继续操作。
     *
     * TransferStack
     * @param <E>
     */

    static final class TransferQueue<E> extends Transferer<E>{



        static final class QNode{
            volatile QNode next;            // next node in queue 等待队列是单向链表
            volatile Object item;           // CAS'ed to or from null
            volatile Thread waiter;         // to control park/unpark  将线程对象保存在这里，用于挂起和唤醒
            final boolean isData;           // 用于判断是写线程节点(isData == true)，还是读线程节点

            QNode(Object item,boolean isData){
                this.isData =isData;
                this.item = item;
            }



            boolean casNext(QNode cmp,QNode val){
                return next == cmp && UNSAFE.compareAndSwapObject(this,nextOffset,cmp,val);
            }


            boolean casItem(Object cmp, Object val) {
                return item == cmp &&
                        UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
            }


            /**
             * Tries to cancel by CAS'ing ref to this as item.
             */
            void tryCancel(Object cmp){
                UNSAFE.compareAndSwapObject(this,itemOffset,cmp,this);
            }


            boolean isCancelled() {
                return item == this;
            }


            /**
             * Returns true if this node is known to be off the queue
             * because its next pointer has been forgotten due to
             * an advanceHead operation.
             */
            boolean isOffList() {
                return next == this;
            }




            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long itemOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = SynchronousQueueDemo.TransferQueue.QNode.class;
                    itemOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("item"));
                    nextOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }

        }



        /** Head of queue */
        transient volatile TransferQueue.QNode head;
        /** Tail of queue */
        transient volatile TransferQueue.QNode tail;



        transient volatile TransferQueue.QNode cleanMe;

        TransferQueue() {
            TransferQueue.QNode h = new TransferQueue.QNode(null, false); // initialize to dummy node.
            head = h;
            tail = h;
        }

        void advanceHead(QNode h, QNode nh) {
            if (h == head &&
                    UNSAFE.compareAndSwapObject(this, headOffset, h, nh))
                h.next = h; // forget old next
        }

        /**
         * Tries to cas nt as new tail.
         */
        void advanceTail(QNode t, QNode nt) {
            if (tail == t)
                UNSAFE.compareAndSwapObject(this, tailOffset, t, nt);
        }

        /**
         * Tries to CAS cleanMe slot.
         */
        boolean casCleanMe(QNode cmp, QNode val) {
            return cleanMe == cmp &&
                    UNSAFE.compareAndSwapObject(this, cleanMeOffset, cmp, val);
        }




        /**
         * Puts or takes an item.
         */
        @Override
        E transfer(E e, boolean timed, long nanos) {


            QNode s = null;

            //put o  take null
            boolean isData = (e!=null);


            for(;;){
                QNode t = tail;
                QNode h = head;

                if(t ==null||h ==null)
                    continue;

                // 队列空，或队列中节点类型和当前节点一致，
                // 即我们说的第一种情况，将节点入队即可。  if 里面方法其实就是入队
                if(h == t || t.isData ==isData){
                    QNode tn = t.next;

                    //其他节点入队
                    if (t != tail)                  // inconsistent read
                        continue;

                    // 有其他节点入队，但是 tail 还是指向原来的，此时设置 tail 即可
                    if(tn != null){
                        // 这个方法就是：如果 tail 此时为 t 的话，设置为 tn
                        advanceTail(t, tn);
                        continue;
                    }


                    if (timed && nanos <= 0)        // can't wait
                        return null;
                    /**
                     * 第一次 创建一个新节点
                     *
                     * 当前即诶单
                     */
                    if (s == null)
                        //isdata 是否数据节点
                        s = new QNode(e, isData);

                    // 将当前节点，插入到 tail 的后面
                    // 失败就再次循环
                    if (!t.casNext(null, s))        // failed to link in
                        continue;

                    // 将当前节点设置为新的 tail
                    advanceTail(t, s);              // swing tail and wait

                    // 看到这里，请读者先往下滑到这个方法，看完了以后再回来这里，思路也就不会断了
                    Object x = awaitFulfill(s, e, timed, nanos);


                    // 到这里，说明之前入队的线程被唤醒了，准备往下执行

                    if (x == s) {                   // wait was cancelled
                        clean(t, s);
                        return null;
                    }

                    if (!s.isOffList()) {           // not already unlinked
                        advanceHead(t, s);          // unlink if head
                        if (x != null)              // and forget fields
                            s.item = s;
                        s.waiter = null;
                    }
                    return (x != null) ? (E)x : e;
                }else {
                    /**
                     * 上面说的第二种情况，有相应的读或写相匹配的情况
                     */
                    QNode m = h.next;               // node to fulfill
                    if (t != tail || m == null || h != head)
                        continue;                   // inconsistent read

                    Object x = m.item;
                    if (isData == (x != null) ||    // m already fulfilled
                            x == m ||                   // m cancelled
                            !m.casItem(x, e)) {         // lost CAS
                        advanceHead(h, m);          // dequeue and retry
                        continue;
                    }

                    advanceHead(h, m);              // successfully fulfilled
                    LockSupport.unpark(m.waiter);
                    return (x != null) ? (E)x : e;


                }
            }

        }

        void clean(QNode pred, QNode s) {
            s.waiter = null; // forget thread
            /*
             * At any given time, exactly one node on list cannot be
             * deleted -- the last inserted node. To accommodate this,
             * if we cannot delete s, we save its predecessor as
             * "cleanMe", deleting the previously saved version
             * first. At least one of node s or the node previously
             * saved can always be deleted, so this always terminates.
             */
            while (pred.next == s) { // Return early if already unlinked
                QNode h = head;
                QNode hn = h.next;   // Absorb cancelled first node as head
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                QNode t = tail;      // Ensure consistent read for tail
                if (t == h)
                    return;
                QNode tn = t.next;
                if (t != tail)
                    continue;
                if (tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                if (s != t) {        // If not tail, try to unsplice
                    QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn))
                        return;
                }
                QNode dp = cleanMe;
                if (dp != null) {    // Try unlinking previous cancelled node
                    QNode d = dp.next;
                    QNode dn;
                    if (d == null ||               // d is gone or
                            d == dp ||                 // d is off list or
                            !d.isCancelled() ||        // d not cancelled or
                            (d != t &&                 // d not tail and
                                    (dn = d.next) != null &&  //   has successor
                                    dn != d &&                //   that is on list
                                    dp.casNext(d, dn)))       // d unspliced
                        casCleanMe(dp, null);
                    if (dp == pred)
                        return;      // s is already saved node
                } else if (casCleanMe(null, pred))
                    return;          // Postpone cleaning s
            }
        }

        // 自旋或阻塞，直到满足条件，这个方法返回
        private Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {

            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();

            // // 判断需要自旋的次数，
            int spins = ((head.next == s) ?
                    (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {

                // 如果被中断了，那么取消这个节点
                if (w.isInterrupted())
                    // 就是将当前节点 s 中的 item 属性设置为 this
                    s.tryCancel(e);


                Object x = s.item;

                // 这里是这个方法的唯一的出口
                if (x != e)
                    return x;

                // 如果需要，检测是否超时
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                if (spins > 0)
                    --spins;

                    // 如果自旋达到了最大的次数，那么检测
                else if (s.waiter == null)
                    s.waiter = w;

                    // 如果自旋到了最大的次数，那么线程挂起，等待唤醒
                else if (!timed)
                    LockSupport.park(this);
                    // spinForTimeoutThreshold 这个之前讲 AQS 的时候其实也说过，剩余时间小于这个阈值的时候，就
                    // 不要进行挂起了，自旋的性能会比较好
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }

        }


        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long cleanMeOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferQueue.class;
                headOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("head"));
                tailOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("tail"));
                cleanMeOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("cleanMe"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    private Transferer transferer;


    public SynchronousQueueDemo() {
        this(false);
    }

    /**
     * Creates a {@code SynchronousQueue} with the specified fairness policy.
     *
     * @param fair if true, waiting threads contend in FIFO order for
     *        access; otherwise the order is unspecified.
     */
    public SynchronousQueueDemo(boolean fair) {
        transferer = fair ? new SynchronousQueueDemo.TransferQueue<E>() : new SynchronousQueueDemo.TransferQueue<E>();
    }

    // 写入值
    public void put(E o) throws InterruptedException {
        if (o == null) throw new NullPointerException();
        if (transferer.transfer(o, false, 0) == null) { // 1
            Thread.interrupted();
            throw new InterruptedException();
        }
    }
    // 读取值并移除
    public E take() throws InterruptedException {
        Object e = transferer.transfer(null, false, 0); // 2
        if (e != null)
            return (E)e;
        Thread.interrupted();
        throw new InterruptedException();
    }


}
