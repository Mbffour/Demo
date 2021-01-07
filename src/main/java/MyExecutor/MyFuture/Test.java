package MyExecutor.MyFuture;

import java.util.concurrent.*;

public class Test {






    public static void main(String[] arrgs) throws InterruptedException {




        Future f  = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                return new Object();
            }
        });
   /*     Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                System.out.println("锁住" + Thread.currentThread().isInterrupted());
                LockSupport.park();

                System.out.println("调用中断 中 "+Thread.currentThread().isInterrupted());

                System.out.println("interrupted1:"+Thread.interrupted());

                System.out.println("interrupted2 :"+Thread.interrupted());

                System.out.println("任务执行结束");
            }
        });

        t.start();


        Thread.sleep(1000);
        System.out.println("调用中断前"+t.isInterrupted());
        t.interrupt();

        Thread.sleep(1000);

        System.out.println("调用中断后"+t.isInterrupted());*/

//
//        System.out.println("interrupted1:"+Thread.interrupted());
//
//        System.out.println("interrupted2 :"+Thread.interrupted());





        final FutureTask futureTask = new FutureTask(new Task());


        Thread thread = new Thread(futureTask);
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                //get响应中断
                Object o = null;
                try {

                    System.out.println(Thread.currentThread().getName()+"获取结果");
                     o = futureTask.get();

                    System.out.println(o);
                }catch (InterruptedException e){
                    System.out.println(Thread.currentThread().getName()+" 自己  获取结果被中断异常");

                }catch (CancellationException e){
                    e.printStackTrace();
                    System.out.println(Thread.currentThread().getName()+"任务取消异常");
                }
                catch (ExecutionException e){
                    e.printStackTrace();
                    System.out.println(Thread.currentThread().getName()+"任务执行中异常");
                }

                System.out.println(Thread.currentThread().getName()+"获取完毕"+o);
            }
        });

        thread.start();

        Thread.sleep(1000);
        thread1.start();


        Thread.sleep(1000);
        System.out.println(Thread.currentThread().getName()+"调用中断:");

        /**
         * 取消任务
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
              System.out.println(futureTask.cancel(true));
            }
        }).start();

        //thread.interrupt();
        //thread1.interrupt();





//        boolean cancel = futureTask.cancel(true);
//
//        System.out.println(Thread.currentThread().getName()+"cancel:"+cancel);


        Thread.sleep(100000);




/*
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                LockSupport.unpark(Thread.currentThread());
                LockSupport.unpark(Thread.currentThread());
                System.out.println("执行任务");

                LockSupport.park(this);

                LockSupport.park();
                System.out.println("任务执行完毕");
            }
        });

        thread.start();



        Thread.sleep(2000);



        System.out.println("中断线程");*/

        //thread.interrupt();



     /*   // 获取异步数据的实现
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.out.println("task 响应中断");
                    return "InterruptedException";
                }
                return "Hello World";
            }
        };

        final FutureTask<String> task = new FutureTask(callable);


        new Thread(task).start();


        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                String s = "null";
                try {
                    s = task.get();
                    System.out.println("结果：" + s);
                } catch (InterruptedException e) {
                    System.out.println("使用者 中断");
                } catch (ExecutionException e) {
                    System.out.println("任务被取消：" + s);
                    e.printStackTrace();
                }

            }
        });

        th.start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName()+"睡一秒中断任务");

        th.interrupt();*/


        //task.cancel(true);


    }

    static class Task implements Callable<String>{


        public void  interrupt(){
            Thread.currentThread().interrupt();
        }
        @Override
        public String call()  {
            System.out.println(Thread.currentThread().getName()+"任务执行");
            try {
                Thread.sleep(10000);
                //int i = 2/0;
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName()+"任务被中断");
                e.printStackTrace();
            }

            //Thread.sleep(40000);
            System.out.println(Thread.currentThread().getName()+"任务执行完毕");
            return "success";
        }
    }
}
