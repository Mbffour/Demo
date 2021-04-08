package MyExecutor;

import java.util.concurrent.CountDownLatch;

public class dd2 {
    public static void main(String[] args){
        try {
            CountDownLatch c = new CountDownLatch(1);
            c.await();
            c.countDown();
            //CyclicBarrier c = new CyclicBarrier()；
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(2222);
            });
            thread.start();

            Thread thread2 = new Thread(() -> {
                try {
                    Thread.sleep(7000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(111);
            });
            thread2.start();


            thread.join();
            thread2.join();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("end");
        System.out.println("end");
        System.out.println("end");

    }
}
