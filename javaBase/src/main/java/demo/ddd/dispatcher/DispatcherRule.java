package demo.ddd.dispatcher;

import demo.ddd.inscase.InsCase;

/**
 * @author ：mbf
 * @date ：2022/8/4
 */
public interface DispatcherRule {


    /**
     * 分配规则
     * @param data
     * @param dispatcherSupport
     * @return 公共池就为null
     */
    String dispatcher(InsCase data, DispatcherSupport dispatcherSupport);
}
