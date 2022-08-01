package MyLock.AQS;

public abstract class MyAbstractOwnableSynchronizer {


    public MyAbstractOwnableSynchronizer() { }


    /**
     * The current owner of exclusive mode synchronization.
     */
    private transient Thread exclusiveOwnerThread;


    /**
     * Sets the thread that currently owns exclusive access.
     * @param thread
     */
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }




    /*
    Returns the thread last set by
     */
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
