package arithmetic.leetcode.array_string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThreeSum {


    public static void main(String[] args){
        int[] arrgs = {-1,2,1,-4};
        System.out.println(threeSumClosest(arrgs,1));
    }


    /**
     * 3Sum Smaller 三数之和较小值
     * @param nums
     * @param target
     * @return
     */
    int threeSumSmaller(int[] nums, int target) {

        Arrays.sort(nums);


        if(nums.length<3)return 0;

        int rs = 0;
        for(int i=0;i<nums.length-2;i++){

            int left = i+1;
            int right = nums.length-1;

            while(left<right){


                int add = nums[i] + nums[left] + nums[right];

                if(add>=target){
                    right--;
                }else if(add<target){

                    //当前下标 三个值 小于目标数
                    //right - left 之间的所有数肯定也小于 目标数

                    //之和小于 目标数  所有的都小于目标数  几个数就几种可能
                    rs+=right-left;

                    left++;
                }

            }


        }


        return rs;
    }
    /**
     * 3Sum Smaller
     * @param nums
     * @param target
     * @return
     */
    public static int threeSumClosest(int[] nums, int target) {


            //排序
            Arrays.sort(nums);


            int rs = nums[0] + nums[1] + nums[2];
            int diff = rs-target>0?rs-target:-(rs-target);


            //排序后找最接近的数
            for(int i=0;i<nums.length-2;i++){


                //第二个数开始 相同的数跳过
                if(i>0&&nums[i]==nums[i-1])continue;


                int left=i+1;
                int right = nums.length-1;

                while(left<right){
                    int sum = nums[i] + nums[left] + nums[right];
                    int tempDiff = target - sum;
                    if(tempDiff<0)tempDiff=-tempDiff;

                    if(diff>tempDiff){
                        diff = tempDiff;
                        rs = sum;
                    }
                    /**
                     * 单独判断条件
                     */
                    if(sum<target) left++;
                    else right--;
                }
            }

            return rs;
    }





    public List<List<Integer>> threeSum(int[] nums) {


        //排序
        Arrays.sort(nums);


        List<List<Integer>> list = new ArrayList<>();


        int start=0;
        int end = 0;
        int current =0;

        for(int i=0;i<nums.length-2;i++){
            if(nums[i]>0){
                break;
            }

            //第二个开始
            if(i>0&&nums[i]==nums[i-1])continue;

            current = i;
            start=i+1;
            end=nums.length-1;

            while(start<end){
                int target = nums[start] + nums[end];
                if(-nums[current]==target){
                    ArrayList<Integer> indexs = new ArrayList<>();
                    indexs.add(nums[current]);
                    indexs.add(nums[start]);
                    indexs.add(nums[end]);
                    list.add(indexs);

                    //重复数字
                    while (start < end && nums[start] == nums[start + 1]) ++start;
                    while (start < end && nums[end] == nums[end - 1]) --end;

                    ++start;
                    --end;
                }else if(target>-nums[current]){
                    end--;
                }else if(target<-nums[current]){
                    start++;
                }
            }
        }


        return list;
    }
}
