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


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
