package demo.arithmetic.sort;

import cn.hutool.core.collection.ListUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ：mbf
 * @date ：2022/8/2
 */
public class SortDemo {

    /**
     * 冒泡排序
     * 比较相邻的元素。如果第一个比第二个大，就交换它们两个；
     * 对每一对相邻元素作同样的工作，从开始第一对到结尾的最后一对，这样在最后的元素应该会是最大的数；
     * 针对所有的元素重复以上的步骤，除了最后一个；
     * 重复步骤 1~3，直到排序完成。
     *
     * @param array
     * @return
     */
    public static int[] bubbleSort(int[] array) {

        int temp;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array.length - i - 1; j++) {
                if (array[j] > array[j + 1]) {
                    temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                }
            }
        }
        return array;
    }


    /**
     * 选择排序
     * 首先在未排序序列中找到最小（大）元素，存放到排序序列的起始位置
     * 再从剩余未排序元素中继续寻找最小（大）元素，然后放到已排序序列的末尾。
     * 重复第 2 步，直到所有元素均排序完毕。
     *
     * @param array
     */
    public static void selectionSort(int[] array) {


        for (int i = 0; i < array.length; i++) {
            int minIndex = i;
            //确定最小的 交换位置
            for (int j = i + 1; j < array.length; j++) {
                if (array[minIndex] > array[j]) {
                    minIndex = j;
                }
            }
            int temp = array[i];
            array[i] = array[minIndex];
            array[minIndex] = temp;
        }
    }


    /**
     * 插入排序
     * 从第一个元素开始，该元素可以认为已经被排序；
     * 取出下一个元素，在已经排序的元素序列中从后向前扫描；
     * 如果该元素（已排序）大于新元素，将该元素移到下一位置；
     * 重复步骤 3，直到找到已排序的元素小于或者等于新元素的位置；
     * 将新元素插入到该位置后；
     * 重复步骤 2~5。
     *
     * @param arrays
     */
    public static void insertSort(int[] arrays) {
        // {5, 76, 2, 6, 73, 21, 772, 6, 214, 782, 643, 3, 86};
        for(int i = 1;i<arrays.length;i++){
            int target = arrays[i];
            for(int j = i-1;j>=0;j--){
                if(target<arrays[j]){
                    exchange(arrays,j,j+1);
                }
            }
        }

    }

    /**
     * 希尔排序
     * 我们来看下希尔排序的基本步骤，在此我们选择增量 gap=length/2，缩小增量继续以 gap = gap/2 的方式，这种增量选择我们可以用一个序列来表示，{n/2, (n/2)/2, ..., 1}，称为增量序列。希尔排序的增量序列的选择与证明是个数学难题，我们选择的这个增量序列是比较常用的，也是希尔建议的增量，称为希尔增量，但其实这个增量序列不是最优的。此处我们做示例使用希尔增量。
     * <p>
     * 先将整个待排序的记录序列分割成为若干子序列分别进行直接插入排序，具体算法描述：
     * <p>
     * 选择一个增量序列 {t1, t2, …, tk}，其中 (ti>tj, i<j, tk=1)；
     * 按增量序列个数 k，对序列进行 k 趟排序；
     * 每趟排序，根据对应的增量 t，将待排序列分割成若干长度为 m 的子序列，分别对各子表进行直接插入排序。仅增量因子为 1 时，整个序列作为一个表来处理，表长度即为整个序列的长度
     *
     * @param arr
     */
    public static void ShellSort(int[] arr) {
        int gap = arr.length/2;

        while (gap>0){

            for(int i=gap;i<arr.length;i++){
                //插入排序 增量为gap
                int target = arr[i];
                for(int j=i-gap;j>=0;j = j-gap){
                    if(target<arr[j]){
                        exchange(arr,j,j+gap);
                    }
                }
            }
            gap = gap/2;
        }
    }


    public static void exchange(int[] array,int a,int b){
        int temp = array[a];
        array[a] = array[b];
        array[b] = temp;
    }


    /**
     * 归并排序
     * 归并排序是建立在归并操作上的一种有效的排序算法。该算法是采用分治法 (Divide and Conquer) 的一个非常典型的应用。归并排序是一种稳定的排序方法。
     * 将已有序的子序列合并，得到完全有序的序列；即先使每个子序列有序，再使子序列段间有序。
     * 若将两个有序表合并成一个有序表，称为 2 - 路归并。
     * 和选择排序一样，归并排序的性能不受输入数据的影响，但表现比选择排序好的多，因为始终都是 O(nlogn) 的时间复杂度。代价是需要额外的内存空间。
     * #算法步骤
     * 归并排序算法是一个递归过程，边界条件为当输入序列仅有一个元素时，直接返回，具体过程如下：
     *
     * 如果输入内只有一个元素，则直接返回，否则将长度为 n 的输入序列分成两个长度为 n/2 的子序列；
     * 分别对这两个子序列进行归并排序，使子序列变为有序状态；
     * 设定两个指针，分别指向两个已经排序子序列的起始位置；
     * 比较两个指针所指向的元素，选择相对小的元素放入到合并空间（用于存放排序结果），并移动指针到下一位置；
     * 重复步骤 3 ~4 直到某一指针达到序列尾；
     * 将另一序列剩下的所有元素直接复制到合并序列尾
     * @param array
     */
    public static int[] mergeSort(int[] array){

        if(array.length <=1){
            return array;
        }
        int mid = array.length/2;
        int[] left = Arrays.copyOfRange(array, 0, mid);
        int[] right = Arrays.copyOfRange(array,mid,array.length);

        return merge(mergeSort(left),mergeSort(right));
    }

    /**
     * 合并两个有序数组  用双指针
     * @param left
     * @param right
     * @return
     */
    private static int[] merge(int[] left, int[] right) {
        int[] result = new int[left.length+right.length];

        int leftIndex=0,rightIndex = 0,resultIndex =0;
        while (leftIndex<left.length && rightIndex<right.length){

            if(left[leftIndex]<right[rightIndex]){
                result[resultIndex] = left[leftIndex];
                leftIndex++;
            }else{
                result[resultIndex]=right[rightIndex];
                rightIndex++;
            }
            resultIndex++;
        }

        //多余的左右数组

        if(leftIndex<left.length){
            while (leftIndex<left.length){
                result[resultIndex] = left[leftIndex];
                resultIndex++;
                leftIndex++;
            }
        }else{
            while (rightIndex<right.length){
                result[resultIndex] = right[rightIndex];
                resultIndex++;
                rightIndex++;
            }
        }

        return result;
    }


    /**
     * 快速排序
     * 快速排序用到了分治思想，同样的还有归并排序。乍看起来快速排序和归并排序非常相似，都是将问题变小，
     * 先排序子串，最后合并。不同的是快速排序在划分子问题的时候经过多一步处理，将划分的两组数据划分为一大一小，
     * 这样在最后合并的时候就不必像归并排序那样再进行比较。但也正因为如此，划分的不定性使得快速排序的时间复杂度并不稳定。
     *
     * 快速排序的基本思想：通过一趟排序将待排序列分隔成独立的两部分，其中一部分记录的元素均比另一部分的元素小，
     * 则可分别对这两部分子序列继续进行排序，以达到整个序列有序。
     *
     * #算法步骤
     * 快速排序使用分治法（Divide and conquer）策略来把一个序列分为较小和较大的 2 个子序列，然后递回地排序两个子序列。具体算法描述如下：
     *
     * 从序列中随机挑出一个元素，做为 “基准”(pivot)；
     * 重新排列序列，将所有比基准值小的元素摆放在基准前面，所有比基准值大的摆在基准的后面（相同的数可以到任一边）。
     * 在这个操作结束之后，该基准就处于数列的中间位置。这个称为分区（partition）操作；
     * 递归地把小于基准值元素的子序列和大于基准值元素的子序列进行快速排序。
     * @param array
     */
    public static void quickSort(int[] array,int left,int right){
        if(right-left<=1){
            return;
        }

        int target = array[left];

        int low = left;
        int high = right;

        //    2 2 4 5 9 6 8
        while (right>left){

            while (array[right]>=target && right>left){
                right--;
            }

            while (array[left]<=target && right>left){
                left++;
            }

            if(left==right){
                array[low] = array[left];
                array[left] = target;
            }else {
                exchange(array,left,right);
            }
        }

        quickSort(array,low,left-1);
        quickSort(array,left+1,high);
    }


    private static int[] getMinAndMax(List<Integer> arr) {
        int maxValue = arr.get(0);
        int minValue = arr.get(0);
        for (int i : arr) {
            if (i > maxValue) {
                maxValue = i;
            } else if (i < minValue) {
                minValue = i;
            }
        }
        return new int[] { minValue, maxValue };
    }

    /**
     * 桶排序
     * @param arr
     * @return
     */
    public static List<Integer> bucketSort(List<Integer> arr, int bucket_size) {
        if (arr.size() < 2 || bucket_size == 0) {
            return arr;
        }
        int[] extremum = getMinAndMax(arr);
        int minValue = extremum[0];
        int maxValue = extremum[1];
        int bucket_cnt = (maxValue - minValue) / bucket_size + 1;
        List<List<Integer>> buckets = new ArrayList<>();
        for (int i = 0; i < bucket_cnt; i++) {
            buckets.add(new ArrayList<Integer>());
        }
        for (int element : arr) {
            int idx = (element - minValue) / bucket_size;
            buckets.get(idx).add(element);
        }
        for (int i = 0; i < buckets.size(); i++) {
            if (buckets.get(i).size() > 1) {
                //排序
                buckets.set(i, bucketSort(buckets.get(i), bucket_size / 2));
            }
        }
        ArrayList<Integer> result = new ArrayList<>();
        for (List<Integer> bucket : buckets) {
            for (int element : bucket) {
                result.add(element);
            }
        }
        return result;
    }




    public static void main(String[] args) {

        int[] array = {72,5, 76, 2, 6, 73, 21, 772, 6, 214, 782, 643, 3, 86};

        List<Integer> list = bucketSort(Arrays.stream(array).boxed().collect(Collectors.toList()), 5);
        System.out.println(list);
        // int[] array = {4, 21, 6, 3,7,9};   3 21 6 4 7 9
//        quickSort(array,0,array.length-1);
//        quickDemo(array,0,array.length-1);
 //       array = mergeSort(array);
          //ShellSort(array);
//          insertSort(array);
//        selectionSort(array);
//        bubbleSort(array);
       // System.out.println(Arrays.toString(array));
    }


}
