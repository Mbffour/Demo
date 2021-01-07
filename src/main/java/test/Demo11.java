package test;

import java.math.BigDecimal;

public class Demo11 {


    public static double div(double v1,double v2,int scale){
        if(scale<0){
            throw new IllegalArgumentException(
                    "The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2,scale,BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供精确的乘法运算。
     * @param v1 被乘数
     * @param v2 乘数
     * @return 两个参数的积
     */
    public static double mul(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.multiply(b2).doubleValue();
    }



    public static double sub(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.subtract(b2).doubleValue();
    }


    public static double add(double v1,double v2){
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.add(b2).doubleValue();
    }

    public static double count(double base, double off, int num) {
        BigDecimal baseDecimal = new BigDecimal(base);
        BigDecimal offDecimal = new BigDecimal(off);
        BigDecimal multiple = new BigDecimal(10000);
        baseDecimal = baseDecimal.multiply(offDecimal);
        if(num == 2){
            baseDecimal = baseDecimal.divide(multiple,2,BigDecimal.ROUND_HALF_UP);
        }else{
            baseDecimal = baseDecimal.divide(multiple,2,BigDecimal.ROUND_DOWN);
        }
        return baseDecimal.doubleValue();
    }




    public static void main(String[] args){
      /*  double a =5.12;
        a= div(a, 100, 4);


        double count = mul(100, a);*/

        Double balanceNum=1.0;
        Double inGameNum =null;
        Double frozenNum =3d;


        String sql = "UPDATE t_club_member_info SET ";

        if(balanceNum!=null)
            sql += " credit_balance = " +balanceNum+",";
        if(inGameNum!=null)
            sql += " credit_ingame = " + inGameNum+",";
        if(frozenNum!=null)
            sql += " credit_frozen = " + frozenNum+",";

        sql = sql.substring(0,sql.lastIndexOf(","));

        System.out.println(sql);

    }
}
