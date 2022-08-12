package MyLock;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class demo {

    public static void main(String[] arsg){

        Thread thread = null;
        Thread finalThread = thread;

        ReentrantLock lock = new ReentrantLock();
        lock.unlock();
         Thread aa= new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try{
                    System.out.println(Thread.currentThread().getName()+"  aaa获取锁");
                    Thread.sleep(500000);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }
            }
        });
         aa.start();


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        thread = new Thread(() -> {

            Random random = new Random();
            System.out.println(Thread.currentThread().getName()+"   bbbb准备获取锁");
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName()+"   bbbb被中断了");
                e.printStackTrace();
            }
         // lock.lock();

            try{
                System.out.println(Thread.currentThread().getName()+"   bbbb获取锁");
            }finally {
                lock.unlock();
            }
//            LockSupport.park(finalThread);
//
//
//            System.out.println("响应中断");
//            for (; ; ) {
//                if (random.nextInt(100) == 55)
//                    System.out.println("中奖");
//            }
        });

        thread.start();




        thread.interrupt();

        while (true){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(thread.getState());
        }

    }
}
