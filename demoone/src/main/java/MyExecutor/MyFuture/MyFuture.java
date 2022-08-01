package MyExecutor.MyFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface MyFuture<V> {


    /**
     * 取消  参数指定是否立即中断任务执行，或者等等任务结束
     * @param mayInterruptIfRunning
     * @return
     */
    public boolean cancel(boolean mayInterruptIfRunning);


    /**
     * isCancelled方法表示任务是否被取消成功，如果在任务正常完成前被取消成功，则返回 true。
     * @return
     */
    public boolean isCancelled();

    /**
     * 是否完成
     * @return
     */
    public boolean isDone();


    /**
     * 获取阻塞    可中断阻塞
     * @return
     * @throws InterruptedException 响应中断
     * @throws ExecutionException
     */
    public V get() throws InterruptedException, ExecutionException;


    /**
     * 限时获取 阻塞
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException  响应中断
     * @throws ExecutionException
     * @throws TimeoutException    超时中断
     */
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;



}
