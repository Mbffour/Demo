package test;


import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class Brandon {




    public static void main(String[] args) throws ExecutionException, InterruptedException {


        Thread t = Thread.currentThread();


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName()+" 解锁");
                t.interrupt();
            }
        }).start();


        LockSupport.park(Thread.currentThread());



        System.out.println(Thread.currentThread().isInterrupted());


/*
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LockSupport.park(Thread.currentThread());

                System.out.println("sssss");
            }
        });

        thread.start();

        Thread.sleep(2000);
        System.out.println(thread.isInterrupted());
        System.out.println("执行");
        thread.interrupt();
        System.out.println(thread.isInterrupted());



        System.out.println(Thread.interrupted());*/



       /* ReentrantLock lock = new ReentrantLock();

        Condition condition = lock.newCondition();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try{
                    System.out.println(Thread.currentThread().getName()+" 阻塞");
                    condition.await();
                    System.out.println(Thread.currentThread().getName()+" 被唤醒");
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }
            }
        });
        t1.start();



        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.lock();
                System.out.println(Thread.currentThread().getName()+" 占用锁" );
                try {
                    Thread.sleep(100000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lock.unlock();
            }
        }).start();


        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                condition.signal();
                System.out.println(Thread.currentThread().getName()+" 解锁");
                lock.unlock();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(80000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("执行唤醒");

                System.out.println("中断");
                t1.interrupt();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();*/





//        int high = 10;
//        int low = 2;
//        int mid = low+(10-2)/2;
//
//        System.out.println(mid);
     /*   ExecutorService executorService = Executors.newSingleThreadExecutor();
        ThreadPoolExecutor t =
        ExecutorServiceWithClientTrace myexecutors = new ExecutorServiceWithClientTrace(executorService);
        Future<?> submit = myexecutors.submit(() -> {
            int[] i = {1};

            System.out.println(i[1]);
        });
        System.out.println("执行完毕");*/

    }


    public static  int test1()  {


        System.out.println("开始");

        int i = 1;
            while(i==1){

                try{
                    if(true)
                    throw new InterruptedException();
                    System.out.println("继续执行吗");
               }catch (Exception e){
                    i=2;
                e.printStackTrace();
               }
            }

        System.out.println("之后");

        return i;
    }
}
