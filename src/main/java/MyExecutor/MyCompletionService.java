package MyExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface MyCompletionService<V> {



    Future<V> submit(Callable<V> task);


    Future<V> submit(Runnable task,V result);


    /**
     * 获取并移除已完成状态的task，如果目前不存在这样的task，则等待；
     * 阻塞 可被 InterruptedException 中断
     * @return  下一个任务
     * @throws InterruptedException
     */
    Future<V> take()throws InterruptedException;


    /**
     * 获取并移除已完成状态的task，如果目前不存在这样的task，返回null；
     * @return
     */
    Future<V> poll();



    Future<V> poll(long timeout, TimeUnit unit)throws InterruptedException;

}
