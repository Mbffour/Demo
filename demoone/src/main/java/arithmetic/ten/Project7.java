package arithmetic.ten;

import java.util.Stack;

public class Project7 {


    //用两个栈来实现一个队列，完成队列的Push和Pop操作。 队列中的元素为int类型。



    public static void main(String[] args){

        Project7 p = new Project7();
        p.push(1);
        p.push(2);
        p.push(3);

        System.out.println(p.pop());

        System.out.println(p.pop());

        p.push(4);

        System.out.println(p.pop());


    }
    Stack<Integer> stack1 = new Stack<Integer>();
    Stack<Integer> stack2 = new Stack<Integer>();

    public void push(int node) {

        stack1.push(node);
    }

    public int pop() {
        //如果pop为空  清空push 填入pop
        if(stack2.empty()){
            if(stack1.empty()){
                throw new RuntimeException("Null");
            }

            int tempSize = stack1.size();
            for(int i=0;i<tempSize;i++){
                Integer pop = stack1.pop();
                stack2.push(pop);
            }
        }
        return  stack2.pop();
    }
}
