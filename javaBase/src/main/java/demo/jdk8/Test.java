package demo.jdk8;

import java.util.function.Function;

/**
 * @author ：mbf
 * @date ：2022/8/1
 */
public class Test {



    public static InterfaceA get(){
        return null;
    }
    public static void main(String[] args){
        Test t = new Test();


        InterfaceA a = Test::get;
    }

    private static void get(Object o) {

    }


}
