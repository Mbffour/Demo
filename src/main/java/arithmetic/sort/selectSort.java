package arithmetic.sort;

public class selectSort {


    /*
    算法思路：
首先在未排序序列中找到最小（大）元素，存放到排序序列的起始位置，然后，再从剩余未排序元素中继续寻找最小（大）元素，然后放到已排序序列的末尾。以此类推，直到所有元素均排序完毕。
     */

    public static void main(String[] aa){
        int[] arrys = {6,12,23,8,4,1,16,9,7,2};




        for(int i=0;i<arrys.length-1;i++){

            int tempindex=i;
            for(int j = i;j<arrys.length-1;j++){
                    if(arrys[tempindex]>arrys[j+1]){
                        tempindex=j+1;
                    }
                }

            //交换位置
            int temp = arrys[tempindex];
            arrys[tempindex]=arrys[i];
            arrys[i]=temp;
            }


        for(int a:arrys){
            System.out.println(a);
        }
    }


    }
