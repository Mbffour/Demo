package demo.ddd.check;

import java.util.stream.Stream;

/**
 * @author ：mbf
 * @date ：2022/8/4
 */
public enum InspectionVersionEnum {
    /**
     * 质检
     */
    v1(1, CheckRuleV1.class);


    private int version;
    private Class<?extends CheckRule> aClass;


    InspectionVersionEnum(int version, Class<? extends CheckRule> aClass) {
        this.version = version;
        this.aClass = aClass;
    }

    public static InspectionVersionEnum trans(int version){
        return Stream.of(InspectionVersionEnum.values())
                .filter(config -> config.version == version)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("没有对应的规则 version:" + version));
    }


    public int getVersion() {
        return version;
    }

    public Class<? extends CheckRule> getaClass() {
        return aClass;
    }
}
