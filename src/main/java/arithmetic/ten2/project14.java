package arithmetic.ten2;

public class project14 {


    //给定一个double类型的浮点数base和int类型的整数exponent。
    // 求base的exponent次方。


    /**
     * 计算n^k  , 当k太大时 。 不能再限制的时间内解决。  这时我们就要用二分快速幂
     * 例如, k=8  ,   化为二进制数  1000 ;
     * 又可以发现,  （1）n(1)* n(1) =n(10) ,
     *
     * （2） n(10)* n(10) =n(100)
     * （3）, n(100)* n(100) =n(1000)  ;
     *
     * (4) n(0)*n(0)=n(1)  例外 .
     *
     *  括号里面是二进制的数，每次本身乘以本身，结果为本身的二进制数加一个0， 所以n^k ， 其实只用运算3次 ，
     *
     *  当K是奇数是，例如 , k=11  ,   化为二进制数  1011 ;
     *
     * n(1011)= n(1000) * n(10) *n(1)  ;  因为也是四位二进制数表示，所以也只用算3次，形如k=8时：
     * 不同的是，  n(1011) = n(1000) * n(10) *n(1) =  (3)*(1)*(4) ;
     * 可以看出，我们最多要算3次，也只是为了算出第(3)部，而（1）和（4）
     * 都是之前就出来的，我们只需把他们保存起来，最后一起相乘即可，
     * 这里就用到了 k&1 （取二进制数里面的最右边的数）  ,
     * 意思是当遇到k 二进制数里面的1时，就执行 ans *= n ; ，
     * 保存起来。   如刚刚说的，刚开始先运算第（4）部，此时遇到1，即保存，
     * 再运算第（1）部，也遇到1，又保存起来。 接着运算第（2）部时，没遇见1，不保存，
     * 最后运算第（3）部，遇见了1，又保存，   。  最后一共保存了3次，因为k二进制数1011里遇到了3次1，
     * 分别保存了第（4）（1）（3）的结果， 所以结果 ans = (4)*(1)*(3).
     *
     * @return
     */
    public double Power(double n, int k) {


        double ans = 1;
        while(k!=0)
        {

            //k&1 最右边的数
            //从地位开始位移
            if((k&1)==1)                   //遇见1，即保存 ：
                ans *= n ;
            //保存每一位的乘后的值
            //第1为   n*n  第二位  n*n*n
            n*= n ;

            k>>=1;            // 向右移一位。本来是1011，执行后，变101。
        }
        return ans ;
    }




    public double Power2(double base, int n) {
        double res = 1,

                curr = base;
        int exponent;
        if(n>0){
            exponent = n;
        }else if(n<0){
            if(base==0)
                throw new RuntimeException("分母不能为0");
            exponent = -n;
        }else{// n==0
            return 1;// 0的0次方
        }

        while(exponent!=0){
            if((exponent&1)==1)
                res*=curr;
            curr*=curr;// 翻倍
            exponent>>=1;// 右移一位
        }


        return n>=0?res:(1/res);
    }

}
