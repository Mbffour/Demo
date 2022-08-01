package arithmetic.sort;

public class quickSort {



    public static  void main(String[] aa){

        int[] arrys = {6,12,23,8,4,1,16,9,7,2};

        qsort(arrys,0,arrys.length-1);

        for(int i:arrys){
            System.out.println(i);
        }

    }



    public static void qsort(int []  arrys,int l,int r){


        if(l>r)return;

        //基准值
        int base = arrys[l];
        int left=l;
        int right = r;
        while(left<right){

            // base = 6                              right
            //      int[] arrys = {6,12,23,8,4,1,16,9,7,2};
            //
            while(arrys[right]>base&&left<right){
                right--;
            }


            arrys[left]=arrys[right];



            while(arrys[left]<base&&left<right){
                left++;
            }


            arrys[right]=arrys[left];






        }
        arrys[left]=base;


        qsort(arrys,l,left-1);
        qsort(arrys,left+1,r);


    }
}



