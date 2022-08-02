package MyExecutor.MyExecutorServiceP;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {
    private static final int COUNT_BITS = Integer.SIZE - 3;
    // 线程池的控制状态（用来表示线程池的运行状态（整形的高3位）和运行的worker数量（低29位））
    public  AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING,0));
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;  //111111

    private static final int RUNNING    = -1 << COUNT_BITS; //111
    //都为1为1 否则为0
    private static int workerCountOf(int c)  { return c & CAPACITY; }

    //两个数只要有一个为1则为1，否则就为0。
    private static int ctlOf(int rs, int wc) { return rs | wc; }

    public static void main(String[] args){


        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {

            }
        });
        System.out.println(CAPACITY);
        int ctl =RUNNING+2|0;
        String binaryString = Integer.toBinaryString(ctl & ~CAPACITY);

        String binaryString2 = Integer.toBinaryString(ctl & ~CAPACITY);

        System.out.println((ctl&~CAPACITY)+" "+binaryString);

        //System.out.println(binaryString2);


    }
}
