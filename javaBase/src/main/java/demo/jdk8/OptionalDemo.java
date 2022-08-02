package demo.jdk8;

import java.util.Optional;

/**
 * @author ：mbf
 * @date ：2022/8/1
 */
public class OptionalDemo {

    public static final String EXCEPTION_A = "xx不能为null";

    public static void main(String[] args){
        Object aa = null;
        Optional.ofNullable(aa).orElseThrow(()-> new RuntimeException(EXCEPTION_A));
//        Optional.ofNullable(zoo).map(o -> o.getDog()).map(d -> d.getAge()).filter(v->v==1).orElse(3);
//        Optional.ofNullable(zoo).map(o -> o.getDog()).map(d -> d.getAge()).ifPresent(age ->
//                System.out.println(age)
//        );
    }
}
