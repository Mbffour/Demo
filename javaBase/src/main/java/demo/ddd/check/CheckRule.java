package demo.ddd.check;

import demo.ddd.inscase.InsCaseDo;

/**
 * @author ：mbf
 * @date ：2022/8/4
 */
public interface CheckRule {

    boolean check(CheckSupport checkSupport, InsCaseDo data);
}
