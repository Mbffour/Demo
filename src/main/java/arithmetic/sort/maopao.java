package arithmetic.sort;

public class maopao {


    /**
     * 算法思路：
     * 1、比较相邻的元素。如果第一个比第二个大，就交换它们两个；
     * 2、对每一对相邻元素作同样的工作，从开始第一对到结尾的最后一对，这样在最后的元素应该会是最大的数；
     * 3、针对所有的元素重复以上的步骤，除了最后一个；
     * 4、重复步骤1~3，直到排序完成。
     * @param args
     */
    public static  void main(String[] args){



        int[] arrys = {6,12,23,8,4,1,16,9,7,2};



        int end =arrys.length-1;

        //1外层循环 length-1
        for(int i = 0;i<arrys.length-1;i++){

            for(int j = 0;j<end;j++){
                if(arrys[j+1]<arrys[j]){
                    int temp =arrys[j];
                    arrys[j]=arrys[j+1];
                    arrys[j+1]=temp;
                }
            }
            //确定尾巴是最大的树
            end--;
        }


        for(int a:arrys){
            System.out.println(a);
        }

    }

}
