package demo.ddd;

import com.alibaba.fastjson.JSONObject;
import demo.ddd.check.CheckConfig;
import demo.ddd.check.CheckRule;
import demo.ddd.check.CheckSupport;
import demo.ddd.check.InspectionVersionEnum;

/**
 * @author ：mbf
 * @date ：2022/8/4
 */
public class RuleTest {



    public static void main(String[] args){
        CheckConfig config = new CheckConfig();
        InspectionVersionEnum trans = InspectionVersionEnum.trans(config.getVersion());
        CheckRule rule = JSONObject.parseObject(config.getConfig(), trans.getaClass());

        //调用相应规则
        boolean data = rule.check((CheckSupport) () -> null, null);



//        DispatcherConfig config = new DispatcherConfig();
//
//        DispatcherConfigEnum configEnum = DispatcherConfigEnum.trans(config.getVersion());
//
//        DispatcherRule rule = JSONObject.parseObject(config.getConfig(), configEnum.aClass);
//
//
//
//        rule.dispatcher("案件", new DispatcherSupport() {
//            @Override
//            public List<String> getAllUser() {
//                return null;
//            }
//
//            @Override
//            public String getUser() {
//                return null;
//            }
//        });
    }
}
