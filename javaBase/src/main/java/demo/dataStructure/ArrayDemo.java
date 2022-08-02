package demo.dataStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author ：mbf
 * @date ：2022/8/2
 */
public class ArrayDemo {




    public static void main(String[] args){
//        ArrayList list = new ArrayList();
//        list.remove()
        Integer[] array = new Integer[10];
        System.out.println(   Arrays.toString(array));
        for(int i = 0;i<array.length;i++){
            array[i] = i;
        }

        System.out.println(   Arrays.toString(array));
        int target = 6;

        boolean trans = false;
        for(int i=0;i<array.length-1;i++){
            if(array[i] == target || trans){
                trans = true;
                array[i] = array[i+1];
            }
        }
        array[array.length-1] = null;
        System.out.println(   Arrays.toString(array));


        //插入
        int addNum = 6;
        int addTransIndex = 0;
        int temp = 0;
        for(int i=0;i<array.length-1;i++){
            if(array[i] > addNum){
                temp = array[i];
                array[i] = addNum;
                addTransIndex = i;
                break;
            }
        }
        for(int i = array.length-1;i>addTransIndex;i--){
            array[i] = array[i-1];
        }
        array[addTransIndex+1] = temp;

        System.out.println(   Arrays.toString(array));
    }
}
