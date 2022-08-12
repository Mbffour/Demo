package demo.ddd.inscase;


import com.alibaba.fastjson.JSONObject;
import demo.ddd.dispatcher.DispatcherService;
import demo.ddd.inscase.status.InsCaseStatusContext;
import demo.ddd.inscase.status.ClaimStatusEnum;
import demo.ddd.check.CheckRule;
import demo.ddd.check.CheckSupport;
import demo.ddd.check.InspectionVersionEnum;

import java.util.List;

/**
 * 案件
 * @author ：mbf
 * @date ：2022/8/4
 */
public class InsCase {


    /**
     * 数据实体类
     */
    private InsCaseDo insCaseDo;

    /**
     * 状态上下文
     */
    InsCaseStatusContext statusContext;


    /**
     * 产生的消息
     */
    List<String> msg;


    /**
     * 其他额外的数据
     */
    String otherData;


    private InsCase(InsCaseDo insCaseDo) {
        this.insCaseDo = insCaseDo;
        statusContext = new InsCaseStatusContext(insCaseDo);

    }

    public static InsCase build(InsCaseDo insCaseDo){
      return  new InsCase(insCaseDo);
    }







    public void checkUser(String user){
        if(insCaseDo.getCurUser()!=user){
            throw new RuntimeException("没有权限");
        }
    }

    /**
     * 立案
     */
    public void register(String user){
        checkUser(user);
        statusContext.next();
        statusContext.fail();
        int i=0;
    }


    /**
     * 审核
     */
    public void audit(String user, CheckSupport inspectionSupport){
        checkUser(user);

        String result ="";

        //TODO 审核业务处理
        doAudit();
        boolean pass  = true;

        if(pass){
            //赔付
            statusContext.next();
        }else{
            //拒赔
            statusContext.fail();
        }

        //质检流程
        if(genCheckRule().check(inspectionSupport,insCaseDo)){
            statusContext.check();
        }

    }




    /**
     * 挂起
     */
    public void hung(String user){
        checkUser(user);
        statusContext.hung();
    }

    /**
     * 解挂
     */
    public void unHung(String user, DispatcherService dispatcherSupport){
        checkUser(user);
        statusContext.unHung();
        //根据规则分配  前人或者池里


    }

    public  void doAudit(){};
    protected  void doRegister(){};




    /**
     * 质检规则
     * @return
     */
    private CheckRule genCheckRule(){
        //根据结果判断是否质检
        InspectionVersionEnum trans = InspectionVersionEnum.trans(insCaseDo.getConfig().getVersion());
        return JSONObject.parseObject(insCaseDo.getConfig().getConfig(), trans.getaClass());
    }


    public static void main(String[] args){

        //当前操作审核
        ClaimStatusEnum status = ClaimStatusEnum.REGISTER;

        InsCaseDo insCaseDo = new InsCaseDo();
        insCaseDo.setStatus(status);


        InsCaseStatusContext context = new InsCaseStatusContext(insCaseDo);
        System.out.println(context);

//        context.doUnRegister();
//        System.out.println(context);

        context.next();
        System.out.println(context);

        context.hung();
        System.out.println(context);

        context.unHung();
        System.out.println(context);

        context.next();
        System.out.println(context);

        context.check();
        System.out.println(context);

        context.fail();
        System.out.println(context);


        System.out.println("-----");
        context.next();
        System.out.println(context);

        context.check();
        System.out.println(context);

        context.hung();
        System.out.println(context);

        context.unHung();
        System.out.println(context);

        context.next();
        System.out.println(context);

        context.next();
        System.out.println(context);

    }

    //状态

}
