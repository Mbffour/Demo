package arithmetic.ten;

public class Project3 {


    // 在一个二维数组中，每一行都按照从左到右递增的顺序排序，每一列都按照从上到下递增的顺序排序。
    // 请完成一个函数，输入这样的一个二维数组和一个整数，判断数组中是否含有该整数。
    public static boolean Find(int target, int [][] array) {


        //边界检查 小于最小值 大于最大值

        if(array==null&&array.length==0){
            throw  new RuntimeException("arrys is null");
        }

        if(target<array[0][0]||target>array[array.length-1][array[array.length-1].length-1]){
            return  false;
        }


        int col = array[0].length;
        int row = 0;

        while(row<array.length&&col>=0){

            if(array[row][col]==target){
                return true;
            }
            //剔除列
            else if (array[row][col]>target){
                col--;
            }
            //剔除行
            else{
                row++;
            }
        }

        return  false;

    }
}
