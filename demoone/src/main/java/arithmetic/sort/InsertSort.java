package arithmetic.sort;

public class InsertSort {

    public static  void main(String[] aa){



        int[] array = {12,6,13,23,4,1,16,9,7,2};



        int j=0;
        for(int i=1;i<array.length;i++){
            //内层


            //i=3 j=2
            int temp = array[i];
            //已排序的长度 从尾端往前比对
            for(j=i-1;j>=0;j--){
               if(temp>array[j]){
                   break;
               }else{
                   //往前走
                   array[j+1]=array[j];
               }
            }

            //位置为j+1;

            array[j+1]=temp;

        }


        for(int a:array){
            System.out.println(a);
        }

    }


}
