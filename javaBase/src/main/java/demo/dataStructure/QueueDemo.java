package demo.dataStructure;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author ：mbf
 * @date ：2022/8/2
 */
public class QueueDemo extends AbstractCollection implements  Queue{

    private int size ;
    private int capacity;
    private Object[] array;
    private int head;
    private int tail;



    public QueueDemo(int size) {
        capacity = size;
        this.array = new Object[size];
        head = 0;
        tail = 0;
    }

    @Override
    public String toString() {
        return "QueueDemo{" +
                "size=" + size +
                ", capacity=" + capacity +
                ", array=" + Arrays.toString(array) +
                ", head=" + head +
                ", tail=" + tail +
                '}';
    }

    public static void main(String[] args){
        QueueDemo queue = new QueueDemo(4);
        queue.add(1);
        System.out.println(queue);
        queue.add(2);
        queue.add(3);
        queue.add(4);
        System.out.println(queue);
        Object poll = queue.poll();
        System.out.println(queue);
        queue.poll();
        queue.poll();
        System.out.println(queue);

        queue.add(1);
        queue.add(2);
        System.out.println(queue);


        queue.poll();
        System.out.println(queue);
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public void forEach(Consumer action) {

    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean add(Object o) {
        //初始化
        if(size == 0){
            array[size] = o;
            head = size;
            tail = ++size;
            return true;
        }

        //满了
        if(size ==capacity){
            throw new RuntimeException("满了");
        }


        array[tail] = o;
        //循环
        if(tail+1 == array.length){
            tail = 0;
        }else {
            tail++;
        }
        size++;
        return true;
    }

    @Override
    public boolean removeIf(Predicate filter) {
        return false;
    }

    @Override
    public Spliterator spliterator() {
        return null;
    }

    @Override
    public Stream stream() {
        return null;
    }

    @Override
    public Stream parallelStream() {
        return null;
    }

    @Override
    public boolean offer(Object o) {
        return false;
    }

    @Override
    public Object remove() {
        return null;
    }

    /**
     * 头部出
     * @return
     */
    @Override
    public Object poll() {
        if(size==0){
            return null;
        }
        Object poll = array[head];
        array[head] = null;
        if(head+1==array.length){
            head=0;
        }else{
            head++;
        }
        size--;
        return poll;
    }

    /**
     * 头部查询元素
     * @return
     */
    @Override
    public Object element() {
        return null;
    }

    @Override
    public Object peek() {
        return null;
    }
}
