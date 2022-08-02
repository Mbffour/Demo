package MyExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.LockSupport;

public class Demo {

    static int a  = 0;
    static int bb  = 0;

    public static void main(String[] args) throws InterruptedException {


        ExecutorService executorService =
                Executors.newFixedThreadPool(10);

        Future<Integer> submit = executorService.submit(new Runnable() {
            @Override
            public void run() {
               /* try {
                    while (true) {

                        System.out.println("睡眠1秒");
                        Thread.sleep(1000);
//                        ThreadPoolExecutor aa = (ThreadPoolExecutor)executorService;
//                        aa.setCorePoolSize(10);
                    }
                } catch (Exception e) {
                    System.out.println("线程中断 直接关闭");
                    e.printStackTrace();
                }*/

                LockSupport.park();

                System.out.println(Thread.currentThread().getName()+"||执行完了");
            }
        },0);


        new Thread(()->{

            int aa=88;
            try {
                System.out.println(Thread.currentThread().getName()+"开始获取结果");
                aa = submit.get();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("integer:"+aa);
            }

            System.out.println(Thread.currentThread().getName()+" get出来了");
        }).start();

        Thread.sleep(3000);

        System.out.println("主线程开始唤醒");

        System.out.println("主线程 cancelled:"+submit.isCancelled()+"||done:"+submit.isDone());
        boolean cancel = submit.cancel(true);
        System.out.println(cancel);

        System.out.println("主线程 cancelled:"+submit.isCancelled()+"||done:"+submit.isDone());

        ThreadPoolExecutor t = (ThreadPoolExecutor)executorService;

        System.out.println(t);
//        System.out.println(Thread.currentThread().getName()+":shutdown");
//        executorService.shutdown();
//        Thread.sleep(5000);

//        executorService.execute(new Runnable() {
////            @Override
////            public void run() {
////                System.out.println("第二个任务");
////            }
////        });


//        System.out.println("线程添加新任务");



    }
    static int cc  = 0;
}
