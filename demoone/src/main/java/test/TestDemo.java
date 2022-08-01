package test;

import java.util.concurrent.CountDownLatch;


/**
 * synchronized
 *
 * 一个线程在获取到监视器锁以后才能进入 synchronized 控制的代码块，一旦进入代码块，
 * 首先，该线程对于共享变量的缓存就会失效，因此 synchronized 代码块中对于共享变量的读取需要从主内存中重新获取，
 * 也就能获取到最新的值。
 *
 * 退出代码块的时候的，会将该线程写缓冲区中的数据刷到主内存中，
 * 所以在 synchronized 代码块之前或 synchronized 代码块中对于共享变量的操作随着该线程退出
 * synchronized 块，会立即对其他线程可见（这句话的前提是其他读取共享变量的线程会从主内存读取最新值）。
 *
 * 线程 a 对于进入 synchronized 块之前或在 synchronized 中对于共享变量的操作，
 * 对于后续的持有同一个监视器锁的线程 b 可见
 *
 * 注意一点，在进入 synchronized 的时候，并不会保证之前的写操作刷入到主内存中，
 * synchronized 主要是保证退出的时候能将本地内存的数据刷入到主内存。
 *
 *
 *
 * 单例模式
 *
 *  public static Singleton getInstance() {
 *         if (instance == null) { // 1. 第一次检查
 *             synchronized (Singleton.class) { // 2
 *                 if (instance == null) { // 3. 第二次检查
 *                     instance = new Singleton(); // 4
 *                 }
 *             }
 *         }
 *         return instance;
 *     }
 *
 *     
 * instance = new Singleton() 这句代码首先会申请一段空间，
 * 然后将各个属性初始化为零值(0/null)，执行构造方法中的属性赋值[1]，
 * 将这个对象的引用赋值给 instance[2]。在这个过程中，[1] 和 [2] 可能会发生重排序。
 *
 *
 * 此时，线程 b 刚刚进来执行到 1（看上面的代码块），就有可能会看到 instance 不为 null，
 * 然后线程 b 也就不会等待监视器锁，而是直接返回 instance。问题是这个 instance
 * 可能还没执行完构造方法（线程 a 此时还在 4 这一步），所以线程 b 拿到的 instance 是不完整的，
 * 它里面的属性值可能是初始化的零值(0/false/null)，而不是线程 a 在构造方法中指定的值。
 *
 *
 * 1、编译器可以将构造方法内联过来，之后再发生重排序就很容易理解了。
 *
 * 2、即使不发生代码重排序，线程 a 对于属性的赋值写入到了线程 a 的本地内存中，此时对于线程 b 不可见。
 *
 *
 */
public class TestDemo {

    private static int x = 0, y = 0;
    private static int a = 0, b =0;

    public static void main(String[] args) throws InterruptedException {
        int i = 0;
        for(;;) {
            i++;
            x = 0; y = 0;
            a = 0; b = 0;
            CountDownLatch latch = new CountDownLatch(1);

            Thread one = new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                }
                a = 1;
                x = b;
            });

            Thread other = new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                }
                b = 1;
                y = a;
            });
            one.start();
            other.start();

            latch.countDown();

            one.join();
            other.join();

            String result = "第" + i + "次 (" + x + "," + y + "）";
            if(x == 0 && y == 0) {
                System.err.println(result);
                break;
            } else {
                System.out.println(result);
            }
        }
    }
}
