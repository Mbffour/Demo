package test;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AbstractExecutorServiceTest {
    @Test
    public void ttt(){
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                System.out.println("ssssss");
                return null;
            }
        });

    }
    @Test
    public void test1(){

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<Callable<String>> list = new ArrayList<>();

        /**
         * 100万 590
         */
        for(int i=0;i<1000000;i++){
            int finalI = i;
            list.add(()->{
                System.out.println(Thread.currentThread().getName()+" task"+finalI+" 正在执行");
                Thread.sleep(10000);
                int a = 2/0;
                return "Success"+ finalI;
            });
        }

        long l = 0l,end;
        try {

            l = System.currentTimeMillis();
            String s = executorService.invokeAny(list,10,TimeUnit.NANOSECONDS);

            end = System.currentTimeMillis();

            System.out.println(s+"||" + (end-l));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            end = System.currentTimeMillis();

            //10043
            System.out.println("全都失败 异常 耗时间 ： "+ (end-l));
            e.printStackTrace();
        } catch (TimeoutException e) {
            end = System.currentTimeMillis();
            System.out.println("超时 异常  耗时："+(end-l));
            e.printStackTrace();
        }
    }


    /**
     * executorService.invokeAll()
     * 在另一个线程调用executorService.shutdown()
     *
     * 线程阻塞在awaitDone(FutureTask.java:429)
     */
    @Test
    public void test2() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        List<Callable<String>> list = new ArrayList<>();

        /**
         * 100万 590
         */
        for(int i=0;i<100;i++){
            int finalI = i;
            list.add(()->{
                    System.out.println(Thread.currentThread().getName() + " task" + finalI + " 正在执行");
                    Thread.sleep(100);
                    int a = 2 / 0;
                return "Success"+ finalI;
            });
        }

        long l = 0L,end;

            l = System.currentTimeMillis();

            new Thread(()->{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                List<Runnable> runnables = executorService.shutdownNow();

                System.out.println("中断任务数量："+runnables.size());

                System.out.println(executorService.isShutdown()+"||isTerminated:"+executorService.isTerminated());
            }).start();


            Thread thread = new Thread(() -> {
                List<Future<String>> futures = null;
                try {

                    //线程阻塞在       get() awaitDone(FutureTask.java:429)
                    System.out.println("executorService.invokeAll 0");
                    futures = executorService.invokeAll(list);
                    System.out.println("executorService.invokeAll 1");
                } catch (Exception e){
                    e.printStackTrace();
                }

                System.out.println("executorService.invokeAll 2");
                for (Future<String> f : futures) {
                    System.out.println("f isDone:" + f.isDone() + " || isCancelled:" + f.isCancelled());
                }
            });

            thread.start();
            Thread.sleep(100000);
//            System.out.println("线程被阻塞：调用中断");
//            thread.interrupt();






    }


    public static void  main(String[] args){





    }
}
