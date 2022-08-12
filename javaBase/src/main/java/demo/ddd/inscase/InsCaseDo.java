package demo.ddd.inscase;

import demo.ddd.inscase.status.ClaimStatusEnum;
import demo.ddd.check.CheckConfig;

import java.util.List;

/**
 * 数据库实体类
 * @author ：mbf
 * @date ：2022/8/5
 */
public class InsCaseDo {
    private ClaimStatusEnum status;
    private List<ClaimStatusEnum> pre;
    private String preUser;
    private String curUser;
    private CheckConfig config;


    @Override
    public String toString() {
        return "InsCaseDo{" +
                "status=" + status +
                ", pre=" + pre +
                ", preUser='" + preUser + '\'' +
                ", curUser='" + curUser + '\'' +
                ", config=" + config +
                '}';
    }

    public ClaimStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ClaimStatusEnum status) {
        this.status = status;
    }

    public List<ClaimStatusEnum> getPre() {
        return pre;
    }

    public void setPre(List<ClaimStatusEnum> pre) {
        this.pre = pre;
    }

    public String getPreUser() {
        return preUser;
    }

    public void setPreUser(String preUser) {
        this.preUser = preUser;
    }

    public String getCurUser() {
        return curUser;
    }

    public void setCurUser(String curUser) {
        this.curUser = curUser;
    }

    public CheckConfig getConfig() {
        return config;
    }

    public void setConfig(CheckConfig config) {
        this.config = config;
    }
}
