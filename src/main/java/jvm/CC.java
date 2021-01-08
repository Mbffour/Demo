package jvm;

public class CC {
    public void tt(){
        AA  a = new BB();
        int result = a.kaka("1");
        System.out.println(result);
    }


    public static void  main(String[] args){
        CC c = new CC();
        c.tt();
    }
}
