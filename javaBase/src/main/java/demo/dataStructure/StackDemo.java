package demo.dataStructure;

import java.util.*;

/**
 * @author ：mbf
 * @date ：2022/8/2
 */
public class StackDemo {

    public Object[] array;
    public int size;
    public int capacity;

    public StackDemo(int num) {
        //init
        this.array = new Object[num];
    }


    //确保容量大小
    private void ensureCapacity() {
        int newCapacity = capacity * 8;
        array = Arrays.copyOf(array, newCapacity);
        capacity = newCapacity;
    }


    /**
     * 出
     * @return
     */
    public Object pop(){
        if(size == 0){
            return null;
        }
        return array[--size];
    }

    /**
     * 进
     */
    public void push(Object obj){
        if(size == 0){
            array[size] = obj;
            size++;
            return;
        }
        array[size] = obj;
        size++;

    }

    @Override
    public String toString() {
        return "StackDemo{" +
                "array=" + Arrays.toString(array) +
                ", size=" + size +
                '}';
    }


    public static class OptRecord {
        private StackDemo go = new StackDemo(10);
        private StackDemo back = new StackDemo(10);
        private Object cur;

        public void opt(Object object){
            if(cur == null){
                cur = object;
            }else {
                back.push(cur);
                cur = object;
            }
        }

        /**
         * 返回操作
         * @return
         */
        public Object back(){
            Object pop = back.pop();
            if(pop!=null){
                go.push(cur);
                cur = pop;
            }
            return pop;
        }


        /**
         * 前进操作
         * @return
         */
        public Object go(){
            Object pop = go.pop();
            if(pop!=null){
                back.push(cur);
                cur = pop;
            }
            return pop;
        }

    }



    /*
    给定一个只包括 '('，')'，'{'，'}'，'['，']' 的字符串，判断该字符串是否有效。

有效字符串需满足：

左括号必须用相同类型的右括号闭合。
左括号必须以正确的顺序闭合。
比如 "()"、"()[]{}"、"{[]}" 都是有效字符串，而 "(]" 、"([)]" 则不是。

     */

    public static boolean check(String str){

        Map<Character,Character> map = new HashMap<>(3);
        map.put('{','}');
        map.put('[',']');
        map.put('(',')');

        Stack<Character> stack = new Stack();
        for(int i = 0 ; i<str.length() ;i ++){
            char c = str.charAt(i);
            //左括号入栈
            if(map.containsKey(c)){
                stack.push(c);
            }else{
                Character pop = stack.pop();
                if (map.get(pop) != c){
                    return false;
                }
            }
        }
        return  stack.isEmpty();
    }


    public static void main(String[] args){
        String s = "()[]{}";
        System.out.println(check(s));
//        OptRecord optRecord = new OptRecord();
//        optRecord.opt("1");
//        optRecord.opt("2");
//        optRecord.opt("3");
//        optRecord.opt("4");
//
//        System.out.println(optRecord.back());
//        System.out.println(optRecord.back());
//        System.out.println(optRecord.back());
//        System.out.println(optRecord.back());
//        System.out.println(optRecord.go());
//        System.out.println(optRecord.back());
//        System.out.println(optRecord.go());
//        System.out.println(optRecord.go());
//        System.out.println(optRecord.back());


//        System.out.println(optRecord.go());
//        System.out.println(optRecord.back());


//        StackDemo stackDemo = new StackDemo(10);
//        stackDemo.push(1);
//        stackDemo.push(2);
//        System.out.println(stackDemo);
//
//        stackDemo.pop();
//        System.out.println("pop"+stackDemo);
//
//        stackDemo.push(123);
//        System.out.println(stackDemo);
//
//        stackDemo.pop();
//        System.out.println("pop"+stackDemo);
//        stackDemo.pop();
//        System.out.println("pop"+stackDemo);
//
//        stackDemo.push(99);
//        System.out.println(stackDemo);

    }
}
