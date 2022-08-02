package MyExecutor.MyExecutorServiceP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public abstract class MyAbstractExecutorService  implements MyExecutorService{


    /**
     * 默认通过FutureTask 封装任务  可实现RunnableFuture 扩展
     * @param runnable
     * @param value
     * @param <T>
     * @return  RunnableFuture  实现了Future 和 Runnable接口
     */
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value);
    }


    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<T>(callable);
    }


//**********************************************************MyExecutor 声明方法 执行任务  留给子类实现  **********************************************************************************************
    /*
    @Override
    public void execute(Runnable runnable) {

    }*/

//**********************************************************ExecutorService 声明方法实现  **********************************************************************************************


    /**
     *        submit
     * 提交  执行任务并返回future
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if(task==null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T resut) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task, resut);
        execute(ftask);
        return ftask;
    }

    @Override
    public <T> Future<T> submit(Runnable task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task, null);
        execute(ftask);
        return ftask;
    }


    /**
     *          invokeAny
     * 批量提交任务并获得一个已经成功执行的任务的结果
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {

        try{
            return doInvokeAny(tasks, false, 0);
        }catch (TimeoutException cannotHappen){
            assert false;
            return null;
        }
    }



    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return doInvokeAny(tasks, true, unit.toNanos(timeout));
    }



    /**
     * the main mechanics of invokeAny.
     * @param tasks
     * @param
     * @param
     * @param <T>
     * @return
     * @throws InterruptedException    可中断
     * @throws ExecutionException      执行中异常
     * @throws TimeoutException        超时异常控制
     */
    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos)
            throws InterruptedException, ExecutionException, TimeoutException{
        if(tasks == null)throw new NullPointerException();

        //批量任务的数量
        int ntasks = tasks.size();

        if (ntasks == 0) throw new IllegalArgumentException();


        ArrayList<Future<T>> futures = new ArrayList<>(ntasks);

        //包装模式
        MyExecutorCompletionService<T> ecs  = new MyExecutorCompletionService<T>(this);

        try{
            ExecutionException ee = null;
            final long deadline = timed?System.nanoTime()+nanos:0l;

            Iterator<? extends Callable<T>> it = tasks.iterator();

            // Start one task for sure; the rest incrementally
            futures.add(ecs.submit(it.next()));

            /**
             * 先提交一个任务
             */
            --ntasks;

            /**
             * 执行中的任务
             * 已经提交到线程池中但是还没有执行完成的任务数
             */
            int active = 1;


            /**
             * 边提交任务边检查是否完成
             * 直到提交完所有
             */
            for(;;){
                //没有就直接返回null  返回的是已经执行完的任务
                Future<T> f = ecs.poll();

                //当前没有 已经执行完的任务  再提交一个
                if(f == null){
                    //还有未提交的任务
                    if(ntasks>0){
                        --ntasks;
                        //提交任务 线程池根据容量执行
                        futures.add(ecs.submit(it.next()));
                        ++active;
                    }

                    else if(active == 0){
                        break;
                    }

                    //如果定时获取
                    else if(timed){
                        f = ecs.poll(nanos,TimeUnit.NANOSECONDS);
                        //为null 就抛出异常  任务还没有执行完毕  中断
                        if (f == null)
                            throw new TimeoutException();
                        //
                        nanos = deadline - System.nanoTime();
                    }
                    else
                    /**
                     * 此时 active>0  ntasks==0 全部任务已经提交
                     */
                     //阻塞 直到一个任务完成 加入到阻塞队列中
                     //ecs.take() InterruptedException 中断 抛给上级处理
                     f = ecs.take();
                }

                /**
                 * 此任务是不是出现异常  出现异常 重新获取
                 */
                if(f!=null){
                    --active;
                    try{
                        /**
                         * get 不会阻塞  获得的Future为已经完成的任务  (cancel/异常/完成 )
                         * 如果异常将会抛出  接着下次循环 直到 获得一个正确的结果
                         *
                         * f.get() InterruptedException 中断抛给上级处理
                         */
                        return f.get();
                    }catch (ExecutionException eex){
                        ee = eex;
                    }catch (RuntimeException rex){
                        ee = new ExecutionException(rex);
                    }
                }
            }

            /**
             * 所有任务都是异常结束的 抛出异常
             */
            if (ee == null) {
                 // new ExecutionException(）
                ee = new ExecutionException(new RuntimeException());
            }
            throw ee;
        }finally {

            /**
             * 全部调用中断
             * 取消一个已经完成的任务不会带来什么负面影响
             */
            for(int i = 0,size = futures.size();i<size;i++){
                futures.get(i).cancel(true);
            }
        }
    }


//******************************************************invokeAll***********************************************


    /**
     *  批量提交任务并获得他们的future，Task列表与Future列表一一对应
     * @param tasks
     * @param <T>
     * @return
     * @throws InterruptedException
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        if(tasks == null)
            throw new NullPointerException();
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());

        boolean done = false;

        try{
            //循环提交
            for(Callable<T> t : tasks){
                RunnableFuture<T> f  = newTaskFor(t);
                futures.add(f);
                execute(f);
            }

            //批量获取
            for(int i = 0,size = futures.size();i<size;i++){
                Future<T> f = futures.get(i);

                if(f.isDone()){

                    /**
                     * 这里没有忽略InterruptedException，
                     * 就是为了当调用invokeAll()的线程被中断的时候，能够响应中断。
                     *  中断后 取消所有任务
                     */
                    try {
                        //会等待任务执行 阻塞
                        f.get();
                    }
                    //任务被取消的异常
                    catch (CancellationException ignore) { }
                    catch (ExecutionException ignore) { }


                }
            }
            done = true;

            return futures;
        }finally {
            /**
             * 出现异常 取消全部任务  比如中断InterruptedException
             */
            if(!done)
                for(int i = 0, size = futures.size();i<size;i++)
                    futures.get(i).cancel(true);
        }
    }


    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {

        if(tasks == null)
            throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());

        boolean done = false;
        try{
            //1 把所有任务封装为RunnableFuture
            for(Callable<T> task :tasks){
                futures.add(newTaskFor(task));
            }

            final long deadline = System.nanoTime()+nanos;
            final int size =futures.size();


            //批量提交任务  并刷新时间 到时间后 返回future
            for(int i = 0 ;i<size;i++){
                execute((Runnable) futures.get(i));
                //判断时间
                nanos = deadline - System.nanoTime();
                /**
                 * 时间到了直接返回  任务此时也许没有完成
                 */
                if(nanos<=0L)
                    return futures;
            }

            for(int i=0;i<size;i++){
                Future<T> f =futures.get(i);

                /**
                 * 实际发生超时期满，和程序探测到超时满，是存在延时的
                 */
                if(!f.isDone()){
                    if(nanos<=0L)
                        return futures;
                        try {
                            f.get(nanos,TimeUnit.NANOSECONDS);
                        }
                        catch (CancellationException ignore) { }
                        catch (ExecutionException ignore) { }
                        /**
                         * 超时时间到后 返回future
                         */
                        catch (TimeoutException e) {
                            return futures;
                        }
                    /**
                     * 触发其他异常会走到这  例如中断  执行中异常 取消异常
                     * 继续任务
                     */
                    nanos = nanos -System.nanoTime();
                }
            }
            done = true;

            //出现异常 咋办
            return futures;

        }finally {
            /**
             * 取消全部任务
             */
            for(int i =0,size=futures.size();i<size;i++){
                futures.get(i).cancel(true);
            }
        }

    }




}
