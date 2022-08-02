package arithmetic.ten;

public class Project9 {


    //大家都知道斐波那契数列，现在要求输入一个整数n，请你输出斐波那契数列的第n项（从0开始，第0项为0）。
    //n<=39
    public static int Fibonacci2(int n) {
        if(n==1){
            return 1;
        }
        if(n==2){
            return 2;
        }
        return Fibonacci(n-1)+Fibonacci(n-2);

    }


    public static int Fibonacci(int n) {

        int a=1;
        int b=1;

        if(n==a){
            return a;
        }
        if(n==b){
            return b;
        }


        //1 2 3 5 8 13
        //1 2 3 4 5 6
        for(int i=2;i<n;i++){
            b=a+b;   //b = a+b
            a=b-a;  //a==b
        }
        return b;
    }



    public static void main(String[] ars){
        int fibonacci = Fibonacci(6);
        System.out.print(fibonacci);
    }
}
