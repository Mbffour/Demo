package MyExecutor.MyFuture;

import java.util.concurrent.Future;

public interface MyRunnableFuture<V> extends Runnable, Future<V> {
    @Override
    void run();
}
