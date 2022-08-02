package MyBlockQueue;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyLinkedBlockingQueue<E>  extends AbstractQueue<E>
        implements BlockingQueue<E> {


    /**
     * Linked list node class
     */
    static class Node<E> {
        E item;

        /**
         * One of:
         * - the real successor Node
         * - this Node, meaning the successor is head.next
         * - null, meaning there is no successor (this is the last node)
         */
        Node<E> next;

        Node(E x) { item = x; }
    }

    /** The capacity bound, or Integer.MAX_VALUE if none */
    private final int capacity;

    private final AtomicInteger count = new AtomicInteger();


    /**
     * Head of linked list.
     * Invariant: head.item == null
     *
     * 队列头节点，始终满足head.item==null
     */
    transient Node<E> head;


    /**
     * 队列的尾节点，始终满足last.next==null
     */
    transient Node<E> last;



    private final ReentrantLock takeLock = new ReentrantLock();

    /**
     *  //当队列为空时，保存执行出队的线程
     */
    private final Condition notEmpty = takeLock.newCondition();

    private final ReentrantLock putLock = new ReentrantLock();

    /**
     *  //当队列满时，保存执行入队的线程
     */
    private final Condition notFull = putLock.newCondition();





    public MyLinkedBlockingQueue(){
        this(Integer.MAX_VALUE);
    }

    public MyLinkedBlockingQueue(int capacity){
        if(capacity<=0)throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }

    public MyLinkedBlockingQueue(Collection<? extends E> c){
        this(Integer.MAX_VALUE);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();

        try{
            int n = 0 ;
            for(E e : c){
                if(e == null)
                    throw new NullPointerException();
                if(n == capacity)
                    throw new IllegalArgumentException("Queue full");
                enqueue(new Node<E>(e));
                ++n;
            }
            count.set(n);
        }finally {
            putLock.unlock();
        }
    }


    /**
     * // 入队的代码非常简单，就是将 last 属性指向这个新元素，并且让原队尾的 next 指向这个元素
     * // 这里入队没有并发问题，因为只有获取到 putLock 独占锁以后，才可以进行此操作
     * @param node
     */
    private void enqueue(Node<E> node){
        last = last.next =node;
    }



    /**
     * 将给定元素在给定的时间内设置到队列中，如果设置成功返回true, 否则返回false.
     * @param e
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {


        if(e == null)throw new NullPointerException();

        long nanos = unit.toNanos(timeout);

        int c = -1;

        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lock();
        try{
            while(count.get() == capacity){
                if(nanos <= 0)
                    return false;

                //超时，则返回一个小于等于 0 的值
                //等待 多长时间
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(new Node<E>(e));
            c = count.getAndIncrement();

            if(c+1<capacity)
                notFull.signal();
        }finally {
            putLock.unlock();
        }

        return true;
    }

    /**
     * add调用
     * 将给定元素设置到队列中，如果设置成功返回true,
     * 否则返回false。如果是往限定了长度的队列中设置值，推荐使用offer()方法。
     * @param e
     * @return
     */
    @Override
    public boolean offer(E e) {
        if( e == null) throw new NullPointerException();
        final AtomicInteger count =this.count;
        if(count.get() == capacity)
            return false;
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try{

            if(count.get() < capacity){
                //添加node
                enqueue(node);

                //拿到当前未添加新元素时的队列长度
                c = count.getAndIncrement();
                /**
                 * 加入队列的值小于 capacity
                 */
                if(c+1 < capacity){
                    //唤醒下一个添加线程，执行添加操作
                    notFull.signal();
                }
            }

        }finally {
            putLock.unlock();
        }

        /**
         * 如果当前队列为null 添加元素后 唤醒 notFull
         * 由于存在添加锁和消费锁，而消费锁和添加锁都会持续唤醒等到线程，因此count肯定会变化。
         * 这里的if条件表示如果队列中还有1条数据
         */

        if(c==0)
            //如果还存在数据那么就唤醒消费锁
            signalNotEmpty();

        // 添加成功返回true，否则返回false
        return c >=0;
    }

    /**
     * Signals a waiting put. Called only from take/poll.
     */
    public void signalNotEmpty(){
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }

    }

    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }
    public boolean remove(Object o){
        if(o == null) return false;
        //获取两锁
        fullyLock();

        try{
            for(Node<E> trail = head,p = trail.next;
                p!=null;
                trail = p, p = p.next){
                if(o.equals(p.item)){

                    unlink(p,trail);//调用unlink删除此节点
                    return true;
                }
            }
            return false;
        }finally {
            fullyUnlock();
        }
    }

    private void unlink(Node<E> p,Node<E> trail){
        p.item = null;
        trail.next = p.next;
        /**
         * 删除节点为尾节点 更新尾节点
         */
        if(last == p)
            last = trail;
        /**
         *  队列满了 阻塞写入线程
         */
        if(count.getAndDecrement() == capacity)
            notFull.signal();
    }



    /**
     * Signals a waiting put. Called only from take/poll.
     */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }


    /**
     * 添加元素  会阻塞
     * @param e
     * @throws InterruptedException
     */
    @Override
    public void put(E e) throws InterruptedException {
        if( e == null)throw new NullPointerException();

        //可以看看 offer 方法。这就是个标识成功、失败的标志而已。
        int c = -1;
        Node<E> node = new Node<E>(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;

        /**
         *  获取可中断锁
         *
         *  ReentrantLock.lockInterruptibly允许在等待时由其它线程调用等待线程的Thread.interrupt方法来中断等待线程的等待而直接返回，
         *  这时不用获取锁，而会抛出一个InterruptedException。
         *  ReentrantLock.lock方法不允许Thread.interrupt中断,即使检测到Thread.isInterrupted,
         *  一样会继续尝试获取锁，失败则继续休眠。只是在最后获取锁成功后再把当前线程置为interrupted状态,然后再中断线程
         */
        putLock.lockInterruptibly();

        try{

            /**
             * 可被中断
             * 队列若满线程将处于等待状态。while循环可避免“伪唤醒”（线程被唤醒时队列大小依旧达到最大值）
             */
            while(count.get() == capacity){

                // notFull：入队条件
                notFull.await();
            }
            //添加
            enqueue(node);

            //获取当前的 并+1
            c = count.getAndIncrement();

            //小于容量
            // 如果这个元素入队后，还有至少一个槽可以使用，调用 notFull.signal() 唤醒等待线程
            // 唤醒其他线程 put线程
            if(c+1 < capacity)
                //唤醒
                notFull.signal();
        }finally {
            //// 入队后，释放掉 putLock
            putLock.unlock();
        }
        /**
         * 之前为0  唤醒消费者线程
         * // 如果 c == 0，那么代表队列在这个元素入队前是空的（不包括head空节点），
         * // 那么所有的读线程都在等待 notEmpty 这个条件，等待唤醒，这里做一次唤醒操作
         */
        if(c == 0)
            //唤醒一个线程
            signalNotEmpty();
    }


    @Override
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger cout = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try{
            // 如果队列为空，等待 notEmpty 这个条件满足再继续执行
            while(count.get() ==0){
                notEmpty.await();
            }
            // 出队
            x = dequeue();
            c = count.getAndDecrement();
            // 如果这次出队后，队列中至少还有一个元素，那么调用 notEmpty.signal() 唤醒其他的读线程
            if(c>1)
                notEmpty.signal();
        }finally {
            // 出队后释放掉 takeLock
            takeLock.unlock();
        }
        // 如果 c == capacity，那么说明在这个 take 方法发生的时候，队列是满的
        // 既然出队了一个，那么意味着队列不满了，唤醒写线程去写
        if (c == capacity)
            signalNotFull();
        return x;
    }

    private E dequeue(){
        //头结点是空的
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h; //gc
        head = first;

        // 设置这个为新的头结点
        E x = first.item;

        //头结点 置为null
        first.item = null;

        //返回头节点的项
        return x;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }







    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }


    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return 0;
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return 0;
    }


    @Override
    public E poll() {
        return null;
    }

    @Override
    public E peek() {
        return null;
    }
}
