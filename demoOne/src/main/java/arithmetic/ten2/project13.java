package arithmetic.ten2;

public class project13 {



    public static  int NumberOf12(int n) {
        int count = 0;
        while (n != 0) {
            ++count;
            n = (n - 1) & n;
        }
        return count;
    }

    //输入一个整数，输出该数二进制表示中1的个数。其中负数用补码表示。

    public static  int NumberOf1(int n) {

        int flag=1;
        int oneNum=0;

        while(flag!=0){

            System.out.println(n&flag);
            if((n&flag)==1){
                ++oneNum;
            }
            //有符号为左移    10000 000 0000 最后 变为 0
            flag=flag<<1;
        }

        return oneNum;
    }


    public static void main(String[] args){
//1000   0000   0000  0000      0000  0000 0000 0000


        int flag = 1;


        int num = 0;
        for(int i=0;i<32;i++){

            flag  =  flag<<1;

            num++;
            System.out.println(flag+"||"+num);
        }

    }
}
