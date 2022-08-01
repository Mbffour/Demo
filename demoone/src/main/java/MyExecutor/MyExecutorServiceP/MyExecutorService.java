package MyExecutor.MyExecutorServiceP;


import MyExecutor.MyExecutor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 *
 */
public interface MyExecutorService  extends MyExecutor {


    /**
     * 当线程池调用该方法时,线程池的状态则立刻变成SHUTDOWN状态。
     * 此时，则不能再往线程池中添加任何任务，否则将会抛出RejectedExecutionException异常
     */
    void shutdown();

    /**
     * shutdownNow就会迫使当前执行的所有任务停止工作。
     * 调用Thread.interrupt()
     * 如果线程中没有sleep 、wait、Condition、定时锁等应用, interrupt()方法是无法中断当前的线程的
     * 返回那些未执行的任务
     */
    List<Runnable> shutdownNow();



    //isShutDown当调用shutdown()或shutdownNow()方法后返回为true。
    boolean isShutdown();


    //isTerminated当调用shutdown()方法后，并且所有提交的任务完成后返回为true;
    //isTerminated当调用shutdownNow()方法后，成功停止后返回为true;
    boolean isTerminated();


    /**
     * 当前线程阻塞，直到
     *
     * 等所有已提交的任务（包括正在跑的和队列中等待的）执行完
     * 或者等超时时间到
     * 或者线程被中断，抛出InterruptedException
     * 然后返回true（shutdown请求后所有任务执行完毕）或false（已超时）
     * 过程中线程池任务可以继续添加
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException;

    <T>Future<T> submit(Callable<T> task);


    <T>Future<T> submit(Runnable task, T resut);

    <T>Future<T> submit(Runnable task);


    //批量提交任务并获得他们的future，Task列表与Future列表一一对应 阻塞
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;


    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
            throws InterruptedException;


    //批量提交任务并获得一个已经成功执行的任务的结果
    <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,ExecutionException;


    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

}
