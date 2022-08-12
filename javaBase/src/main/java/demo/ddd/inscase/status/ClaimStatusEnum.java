package demo.ddd.inscase.status;

/**
 * 状态流转
 * @author ：mbf
 * @date ：2022/8/4
 */
public enum ClaimStatusEnum {
    /**
     *  提交索赔单 准备立案
     */
    REGISTER("立案"){
        @Override
        public void doNext(InsCaseStatusContext context) {
            context.changeStatus(AUDIT);
        }

        @Override
        public void doUnRegister(InsCaseStatusContext context) {
            context.changeStatus(E_UN_REGISTER);
        }

        @Override
        public void doFail(InsCaseStatusContext context) {
            context.changeStatus(E_REGISTER_FAIL);
        }

        @Override
        public void doHung(InsCaseStatusContext context) {
            context.prePush(this);
            context.changeStatus(HUNG);
        }
    },


    AUDIT("审核"){
        @Override
        public void doNext(InsCaseStatusContext context) {
            context.changeStatus(WAIT_PAY);
        }

        @Override
        public void doHung(InsCaseStatusContext context) {
            context.prePush(this);
            context.changeStatus(HUNG);
        }

        @Override
        public void doFail(InsCaseStatusContext context) {
            context.changeStatus(E_REFUSE);
        }
    },


    /**
     * 质检
     */
    CHECK("质检"){
        /**
         * 质检通过就返回之前的状态   不赔付/ 赔付待付款
         * @return
         */
        @Override
        public void doNext(InsCaseStatusContext context) {
            context.changeStatus(context.prePop());
        }

        /**
         * 质检不通过就 返回到审核阶段
         * @return
         */
        @Override
        public void doFail(InsCaseStatusContext context) {
            //前状态出栈 不需要记录了
            context.prePop();
            context.changeStatus(AUDIT);
        }

        @Override
        public void doHung(InsCaseStatusContext context) {
            context.prePush(this);
            context.changeStatus(HUNG);
        }
    },


    WAIT_PAY("已决未付"){

        @Override
        public void doCheck(InsCaseStatusContext context) {
            context.prePush(this);
            context.changeStatus(CHECK);
        }

        @Override
        public void doNext(InsCaseStatusContext context) {
            context.changeStatus(E_FINISH);
        }
    },


    /**
     * 挂起状态
     */
    HUNG("挂起"){
        @Override
        public void doUnHung(InsCaseStatusContext context) {
            //返回前一状态
            context.changeStatus(context.prePop());
        }
    },

    /**
     * 完结状态 拒赔  可能会进入质检
     */
    E_REFUSE("拒赔"){
        @Override
        public void doCheck(InsCaseStatusContext context) {
            context.prePush(this);
            context.changeStatus(CHECK);
        }
    },

    /**
     * 完结状态 支付完成
     */
    E_FINISH("完成"),

    /**
     * 完结状态 撤案
     */
    E_UN_REGISTER("撤案"),

    /**
     * 完结状态  立案失败
     */
    E_REGISTER_FAIL("不予立案"),

    ;


    private String des;

    ClaimStatusEnum(String des) {
        this.des = des;
    }

    /**
     * 撤案
     * @return
     */
    void doUnRegister(InsCaseStatusContext context){
        throw new RuntimeException("不支持的操作");
    }
    /**
     *  下一步
     * @return
     */
    void doNext(InsCaseStatusContext context){
        throw new RuntimeException("不支持的操作");
    }
    /**
     * 失败
     * @return
     */
    void doFail(InsCaseStatusContext context){
        throw new RuntimeException("不支持的操作");
    }

    /**
     * 质检
     * @return
     */
    void doCheck(InsCaseStatusContext context){
        throw new RuntimeException("不支持的操作");
    }
    /**
     * 挂起
     * @return
     */
    void doHung(InsCaseStatusContext context){ throw new RuntimeException("不支持的操作"); }
    /**
     * 解挂
     * @return
     */
    void doUnHung(InsCaseStatusContext context){
        throw new RuntimeException("不支持的操作");
    }



}
