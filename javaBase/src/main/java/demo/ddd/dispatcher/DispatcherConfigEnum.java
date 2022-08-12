package demo.ddd.dispatcher;

import java.util.stream.Stream;

/**
 * @author ：mbf
 * @date ：2022/8/4
 */
public enum DispatcherConfigEnum {

    v1(1, DispatcherV1.class),
    ;


    /**
     * 版本
     */
    public int version;
    /**
     * 分配规则
     */
    public Class<? extends DispatcherRule> aClass;

    DispatcherConfigEnum(int version, Class<? extends DispatcherRule> aClass) {
        this.version = version;
        this.aClass = aClass;
    }

    public static DispatcherConfigEnum trans(int version){
        return Stream.of(DispatcherConfigEnum.values())
                .filter(config -> config.version == version)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("没有对应的规则 version:" + version));
    }
}
