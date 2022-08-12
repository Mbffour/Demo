package demo.ddd.inscase;

/**
 * 挂起枚举
 * @author ：mbf
 * @date ：2022/8/4
 */
public enum HungEnum {

    /**
     * 补充材料时 用户可以修改案件
     */
    SUPPLY("补充材料"),
    OTHER("其他"),
    CHECK("调查")
    ;
    private String desc;

    HungEnum(String desc) {
        this.desc = desc;
    }
}
