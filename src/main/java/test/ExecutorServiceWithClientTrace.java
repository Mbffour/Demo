package test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ExecutorServiceWithClientTrace implements ExecutorService {

    protected final ExecutorService target;

    public ExecutorServiceWithClientTrace(ExecutorService target) {
        this.target = target;
    }


    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return target.submit(wrap(task,clinetTrace(),Thread.currentThread().getName()));
    }


    private <T> Callable<T> wrap(final Callable<T> task,final Exception clientStack, String clientThreadName){

        return ()->{
            try{
               return  task.call();
            }catch (Exception e){
                System.out.println("Exception {} in task submitted from thrad {} here:"+e+"||"+clientThreadName+"||"+clientStack);
                throw  e;
            }
        };

    }

    private Exception clinetTrace(){
        return new Exception("Client stack trace");
    }



    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit(Executors.callable(task,result));
    }


    @Override
    public Future<?> submit(Runnable task) {
       return submit(Executors.callable(task,null));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public void execute(Runnable command) {

    }
}
