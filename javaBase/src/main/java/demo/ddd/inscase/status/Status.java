package demo.ddd.inscase.status;

/**
 * @author ：mbf
 * @date ：2022/8/5
 */
public interface Status {

    /**
     * 撤案
     * @return
     */
     void unRegister();
    /**
     *  下一步
     * @return
     */
    void next();

    /**
     * 失败
     * @return
     */
    void fail();




    /**
     * 质检
     * @return
     */
    void check();


    /**
     * 挂起
     * @return
     */
    void hung();
    /**
     * 解挂
     * @return
     */
    void unHung();
}
