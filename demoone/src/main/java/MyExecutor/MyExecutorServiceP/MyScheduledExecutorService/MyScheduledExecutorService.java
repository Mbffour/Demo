package MyExecutor.MyExecutorServiceP.MyScheduledExecutorService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * 时间表服务扩展
 */
public interface MyScheduledExecutorService  extends ExecutorService {


    /**
     * ScheduledFuture
     * extends Delayed, Future<V>
     *
     * Delayed
     * 获取剩余延时
     * long getDelay(TimeUnit unit);
     *
     */




    /**
     * 延迟时间执行任务
     * @param command
     * @param delay
     * @param unit
     * @return
     */
    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay, TimeUnit unit);


    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit);


    /**
     * 是以上一个任务开始的时间计时，period时间过去后，检测上一个任务是否执行完毕，
     * 如果上一个任务执行完毕，则当前任务立即执行，
     * 如果上一个任务没有执行完毕，则需要等上一个任务执行完毕后立即执行。
     *
     * 上一个任务阻塞会导致后续任务阻塞  参考应用模型 selector
     * @param command
     * @param initialDelay
     * @param period
     * @param unit
     * @return
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit);


    /**
     * 上一个任务结束时开始计时，period时间过去后，立即执行
     * @param command
     * @param initialDelay
     * @param delay
     * @param unit
     * @return
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit);






}
