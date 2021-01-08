package jvm;

public class SubClass extends SuperClass {

    private int mSubX = 1;

    public SubClass() {}

    @Override
    public void setX(int x) {
        super.setX(x);
        mSubX = x;
        System.out.println("mSubX is assigned " + x);
    }

    public void printX() {
        System.out.println("mSubX = " + mSubX);
    }

    public static void main(String[] args) {

        try {
            Class.forName("111",false,null );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        SubClass sc = new SubClass();
        sc.printX();
    }

    /*
    现在我来详细说一下invokevirtual指令的多态查找过程：
1.找到操作数栈顶的第一个元素所指向的对象的实际类型，记做C

2.如果在C中找到与常量中的描述符和简单名称都相符的方法，则进行访问权限校验，如果通过则返回这个方法的直接引用；不通过则抛出IllegalAccessError异常

3.否则，按照继承关系从下往上依次对C的各个父类进行第二步的搜索和验证过程

4.如果始终没有找到合适的方法，就抛出AbstractMethodError异常

而上述步骤就是Java语言中方法重写的本质，而这种在运行期根据实际类型（对应步骤一）确定方法执行版本的分派过程就是动态分派！！！

那么我们回到刚才所讨论的代码上，要找到当前栈帧上操作数栈顶引用所代表的实际类型，看一下上面贴出的SuperClass.<init>的字节码。我们发现在调用setX方法之前，对操作数栈压入了this，又弹出this调用了Object的<init>构造器，之后又压入了99和this，此时操作数栈顶引用this所代表的实际类型就是subClass（上面已经进行了验证）。根据动态分派的原理，最后会调用SubClass中的setX方法，也就是重写后的方法。
     */
}
