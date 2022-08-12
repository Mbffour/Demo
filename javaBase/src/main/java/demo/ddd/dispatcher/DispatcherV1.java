package demo.ddd.dispatcher;

import demo.ddd.inscase.InsCase;

import java.util.List;

/**
 * @author ：mbf
 * @date ：2022/8/4
 */
public class DispatcherV1 implements DispatcherRule {
    //当前版本
    private int version;


    //对应的规则字段

    //立案规则
    private String lianMethod;
    private String liandiff;
    //质检规则
    private String zhijianMethod;
    //问题件
    private String troubleMethod;


    @Override
    public String dispatcher(InsCase data, DispatcherSupport dispatcherSupport) {
        //根据不同策略获取用户
        //获取用户
        List<String> allUser = dispatcherSupport.getAllUser();
        return null;
    }
}
