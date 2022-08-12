package demo.ddd.inscase;

/**
 * 审核结果枚举
 * @author ：mbf
 * @date ：2022/8/4
 */
public enum AuditResultEnum {
    PAID("赔付"),
    REFUSE("拒绝")
    // 协议理赔 通融理赔
    ;


    private String desc;

    AuditResultEnum(String desc) {
        this.desc = desc;
    }
}
