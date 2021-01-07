package jvm;

public class BB extends  AA {
    private AA aa ;
    public int num = 10;
    private int a = AA.a;

    private static int temp = 1;


    static {
        temp=4252;
    }
    public static void main(String[] arhs){

        AA a = new BB();
//
//        BB b = new BB();
//
//        b.sayHi(a);
        a.kaka("sss");

        System.out.println(111);
    }

    public String sayHi(AA a){
        a.kaka("ssss");
        return "hi"+num;
    }


    public String sayHi(BB a){
        a.kaka("ssss");
        return "hi"+num;
    }

    @Override
    public void kaka(String sss) {
        System.out.println("bbbbbbnb");
    }

    //    public int sayHi(String s){
//        aa.kaka("ssss");
//        return 11;
//    }
}
