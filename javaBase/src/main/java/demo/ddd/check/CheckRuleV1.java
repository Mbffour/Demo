package demo.ddd.check;

import demo.ddd.inscase.InsCase;
import demo.ddd.inscase.InsCaseDo;

/**
 * @author ：mbf
 * @date ：2022/8/4
 */
public class CheckRuleV1 implements CheckRule {
    private int version;


    /**
     * 一些规则
     */
    private String rule;

    @Override
    public boolean check(CheckSupport inspectionSupport, InsCaseDo data) {

        //处理数据
        return false;
    }
}
