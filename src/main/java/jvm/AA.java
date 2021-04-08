package jvm;

import java.util.concurrent.*;

public class AA {

    static {
        // 给变量赋值可以正常编译通过
        i = 0;
        // 这句编译器会提示"非法向前引用"
        //System.out.println(i);
    }

    static int i = 1;

    public static int a = 10;
    public static int aaaa= 10;

    public int  kaka(String sss){
        System.out.println("ssss");
        return 0;
    }

    public static void main(String[] args){
        ExecutorService executorService = Executors.newFixedThreadPool(10);


        FutureTask futureTask = new FutureTask(()->{
            try {
                TimeUnit.SECONDS.sleep(3);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            return 1;
        });


        executorService.submit(futureTask);
    }
}
