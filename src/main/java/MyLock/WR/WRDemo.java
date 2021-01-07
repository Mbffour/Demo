package MyLock.WR;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WRDemo {


    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    static   CountDownLatch countDownLatch = new CountDownLatch(1);

    static CyclicBarrier cyclicBarrier = new CyclicBarrier(2);


    private static volatile  int i=1;

    private static volatile  int y=1;

    public static void main(String[] args){


        new Thread(WRDemo::read).start();
        new Thread(WRDemo::read).start();

        new Thread(WRDemo::write).start();

        new Thread(WRDemo::read1).start();


        new Thread(WRDemo::read2).start();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(WRDemo::read3).start();
        new Thread(WRDemo::read4).start();


    }


    public static void write(){
        lock.writeLock().lock();

        System.out.println("write"+y++);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        countDownLatch.countDown();


        lock.writeLock().unlock();
    }
    public static void read()  {
        lock.readLock().lock();

        System.out.println("read"+i++);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.readLock().unlock();
    }


    public static void read1()  {


        lock.readLock().lock();

        System.out.println("read A");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.readLock().unlock();
    }
    public static void read2()  {



        lock.readLock().lock();

        System.out.println("read B");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.readLock().unlock();
    }
    public static void read3()  {

        try {
            cyclicBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }

        lock.readLock().lock();

        System.out.println("read C");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.readLock().unlock();
    }
    public static void read4()  {

        try {
            cyclicBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }


        lock.readLock().lock();

        System.out.println("read D");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        lock.readLock().unlock();
    }

}
