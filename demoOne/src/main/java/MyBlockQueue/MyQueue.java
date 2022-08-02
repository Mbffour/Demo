package MyBlockQueue;

public interface MyQueue<E> {


    /**
     * add(E e) : 将元素e插入到队列末尾，如果插入成功，则返回true；如果插入失败（即队列已满），则会抛出异常；
     * remove() ：移除队首元素，若移除成功，则返回true；如果移除失败（队列为空），则会抛出异常；
     * offer(E e) ：将元素e插入到队列末尾，如果插入成功，则返回true；如果插入失败（即队列已满），则返回false；
     * poll() ：移除并获取队首元素，若成功，则返回队首元素；否则返回null；
     * peek() ：获取队首元素，若成功，则返回队首元素；否则返回null
     *
     *
     * @param a
     * @return
     */

    boolean add(E a);

    boolean offer(E e);

    E remove();

    E poll();

    E element();

    E peek();
}
