package arithmetic.sort;

public class qingwa {

    public static void main(String[] args){
        //一只青蛙跳台阶，一次可以跳1阶，可以2阶。那么，台阶为n时，有多少种跳法。



        //1、1、2、3、5、8、13、21、34
        long run = run(10);


        //4 3 2
        System.out.println(run);

        int i = qingwa2(3);

        System.out.println(i);
    }

    //    f(1) = 1
    //    f(2) = 2
    //    f(n) = f(n-1) + f(n-2)


    private static int qingwa(int a) {
        if (a == 1) {
            return 1;
        } else if (a == 2) {
            return 2;
        } else {
            return qingwa(a - 1) + qingwa(a - 2);
        }
    }



    public static  long  run(int n){
        if(n==0)return 0;
        if(n==1) return 1;

        if(n==2) return 2;


        //非递归 fn = fn-1 +fn-2

        int one = 1;
        int two = 2;


        int target=0;
        for(int i=2;i<n;i++){
            target=one+two;
            one = two;
            two=target;
        }
        return target;

        //return  run(n-1)+run(n-2);
    }



    public static int qingwa2(int n){
        //假设一共有n阶，同样共有f(n)种跳法，那么这种情况就比较多，
        // 最后一步超级蛙可以从n-1阶往上跳，也可以n-2阶，也可以n-3…等等等，一次类推。
        //f(n) = f(n-1) + f(n-2) + ... + f(2) + f(1)
        //f(n-1)=f(n-2) + ... + f(2) + f(1)
        //f(n)=f(n-1)+f(n-1)=2f(n-1)


        if (n==1){
            return 1;
        }else {
            return 2*qingwa2(n-1);
        }


    }
}
