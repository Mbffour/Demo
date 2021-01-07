package MyExecutor.MyExecutorServiceP;

import MyExecutor.MyCompletionService;
import MyExecutor.MyExecutor;

import java.util.concurrent.*;

public class MyExecutorCompletionService<V> implements MyCompletionService<V> {

    private final MyExecutor executor;
    private final MyAbstractExecutorService aes;

    private final BlockingQueue<Future<V>> completionQueue ;






    /**
     * 自定义阻塞队列
     * @param executor
     * @param completionQueue
     */
    public MyExecutorCompletionService(MyExecutor executor,
                                       BlockingQueue<Future<V>> completionQueue) {
        if(executor == null || completionQueue == null)
            throw new NullPointerException();
        this.executor =executor;
        this.aes = (executor instanceof MyAbstractExecutorService) ?
                (MyAbstractExecutorService) executor : null;
        this.completionQueue =completionQueue;

    }
    public MyExecutorCompletionService(MyExecutor executor){
        if(executor == null)
            throw new NullPointerException();
        this.executor=executor;
        //判断是否是AbstractExecutorService 子类
        this.aes = (executor instanceof MyAbstractExecutorService)?
                (MyAbstractExecutorService) executor :null;
        //默认使用 LinkedBlockingQueue
        this.completionQueue = new LinkedBlockingQueue<Future<V>>();

    }



    /**
     * 扩展 FutureTask
     * done()  方法   任务完成操作的时候调用（cancel,异常(包括中断异常),完成）   finishCompletion()
     */
    private class QueueingFuture extends FutureTask<Void>{
        private final Future<V> task;

        // 完成队列添加任务 可被获取
        protected void done() { completionQueue.add(task); }

        QueueingFuture(RunnableFuture<V> task) {
            super(task, null);
            this.task = task;
        }
    }


//***********************************************代理**********************************************************************

    private RunnableFuture<V> newTaskFor(Callable<V> task) {
        //没有就使用默认的 FutureTask
        if (aes == null)
            return new FutureTask<V>(task);
        else
            return aes.newTaskFor(task);
    }


    private RunnableFuture<V> newTaskFor(Runnable task, V result) {
        if (aes == null)
            return new FutureTask<V>(task, result);
        else
            return aes.newTaskFor(task, result);
    }


    /**
     * 重写 submit  使用QueueingFuture 包装 FutureTask 或自定 RunnableFuture
     * @param task
     * @return
     */
    @Override
    public Future<V> submit(Callable<V> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task);
        executor.execute(new MyExecutorCompletionService.QueueingFuture(f));
        return f;
    }

    @Override
    public Future<V> submit(Runnable task, V result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<V> f = newTaskFor(task,result);
        executor.execute(new MyExecutorCompletionService.QueueingFuture(f));
        return f;
    }



    @Override
    public Future<V> take() throws InterruptedException {
        return completionQueue.take();
    }

    /**
     * 移除并返问队列头部的元素    如果队列为空，则返回null
     * Queue 方法
     * @return
     */
    @Override
    public Future<V> poll() {
        return completionQueue.poll();
    }

    @Override
    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return completionQueue.poll(timeout, unit);
    }
}
