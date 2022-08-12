package demo.ddd.inscase.status;

import demo.ddd.inscase.InsCaseDo;

import java.util.ArrayList;
import java.util.List;

/**
 * 状态上下文
 *
 * @author ：mbf
 * @date ：2022/8/5
 */
public class InsCaseStatusContext implements Status {

    private InsCaseDo insCaseDo;

    public InsCaseStatusContext(InsCaseDo insCaseDo) {
        this.insCaseDo = insCaseDo;
    }

    /**
     * 状态流转
     * @param status
     */
    public void changeStatus(ClaimStatusEnum status){
        insCaseDo.setStatus(status);
    }

    /**
     * 出栈
     * @return
     */
    public ClaimStatusEnum prePop(){
        if(getPreList() == null || getPreList().isEmpty()){
            return null;
        }
        return getPreList().remove(getPreList().size() - 1);
    }

    /**
     * 入栈
     * @param status
     */
    public void   prePush( ClaimStatusEnum status){
        if(getPreList() == null){
            setPreList(new ArrayList<>(5));
        }
        getPreList().add(status);
    }

    private ClaimStatusEnum getCurrentStatus(){
        return insCaseDo.getStatus();
    }


    private List<ClaimStatusEnum> getPreList(){
        return insCaseDo.getPre();
    }
    private void setPreList(List<ClaimStatusEnum> preList){
        insCaseDo.setPre(preList);
    }

    @Override
    public void unRegister() {
        getCurrentStatus().doUnRegister(this);
    }
    @Override
    public void next() {
        getCurrentStatus().doNext(this);
    }
    @Override
    public void fail() {
        getCurrentStatus().doFail(this);
    }
    @Override
    public void check() {
        getCurrentStatus().doCheck(this);
    }
    @Override
    public void hung() {
        getCurrentStatus().doHung(this);
    }

    @Override
    public void unHung() {
        getCurrentStatus().doUnHung(this);
    }


    @Override
    public String toString() {
        return "InsCaseStatusContext{" +
                "status=" + getCurrentStatus() +
                ", preList=" + getPreList() +
                '}';
    }
}
