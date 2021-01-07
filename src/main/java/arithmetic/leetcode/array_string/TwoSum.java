package arithmetic.leetcode.array_string;

import java.util.*;

public class TwoSum {

    public static void  main(String[] args){


        cTwoSum t = new cTwoSum();

        t.add(1);
        t.add(13);
        t.add(3);

        int i =0;
        while(i<20){
            i++;
            System.out.println(t.find(i)+"||"+i);
        }

        //findTarget(new int[]{5,3,6,2,4,null,1},-1);

    }

    public int[] twoSum(int[] nums, int target) {

        Map<Integer,Integer> map = new HashMap<>();
        int[] res = new int[2];
        for(int i=0;i<nums.length;i++){
            if(map.containsKey(target-nums[i])){
                res[0]=i;
                res[1]=map.get(target-nums[i]);
            }

            map.put(nums[i],i);
        }


        return  null;
    }

    public static  int[] twoSum2(int[] numbers, int target) {
        int[] res= new int[2];
        int sIndex = 0;
        int eIndex = numbers.length-1;

        while(sIndex<eIndex){
            int a = numbers[sIndex]+numbers[eIndex];

            if(a==target){
                res[0]=sIndex+1;
                res[1]=eIndex+1;
                return res;
            }else if(a>target){
               eIndex--;
            }else{
                sIndex++;
            }
        }
        return null;
    }


    /**
     *  Two Sum IV - Input is a BST
     * @param root
     * @param k
     * @return
     */
    //22 ms

    public static boolean findTarget2(TreeNode root, int k) {
        if(root==null)
            return false;
        List<Integer> list = new ArrayList<>();
        changeList(root,list);


        int sindex=0;
        int edinex=list.size()-1;

        while (edinex>sindex){

            int i = list.get(sindex) + list.get(edinex);
            if(i==k){
                return true;
            }else if(i>k){
                edinex--;
            }else{
                sindex++;
            }
        }


        return false;
    }



    public static  void changeList(TreeNode root, List<Integer> list){

        if(root==null)
            return;
        changeList(root.left,list);

        list.add(root.val);

        changeList(root.right,list);
    }


    /**
     *  Two Sum IV - Input is a BST
     * @param root
     * @param k
     * @return
     */
    public static boolean findTarget(TreeNode root, int k) {

        //遍历一边

        HashSet<Integer> set = new HashSet<>();

        //中序列
       return  find(root, set,k);
    }

    public static boolean find(TreeNode root,HashSet<Integer> set,int k) {  //中序遍历


        if(null==root){
            return false;
        }
        if(set.contains(k-root.val)){
            return true;
        }
        set.add(root.val);

        boolean a = find(root.left, set, k);

        boolean b = find(root.right, set, k);

        return a||b;
    }


    public  static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int x) { val = x; }
    }


    /**
     * 与Two Sum类似.
     *
     * 设计题目考虑trade off. 如果add 很多, find很少可以采用map方法.
     *
     * 简历HashMap保存key 和 key的frequency. find value时把HashMap的每一个key当成num1, 来查找HashMap是否有另一个value-num1的key.
     *
     * num1 = value - num1时，检查num1的frequency是否大于1.
     *
     * Time Complexity: add O(1). find O(n). Space: O(n).
     */
    public static class cTwoSum{


        private HashMap<Integer,Integer> map = new HashMap<>();


        public void add(int num){
            if(map.containsKey(num)){
                map.put(num,map.get(num)+1);
            }else{
                map.put(num,1);
            }
        }


        public boolean find(int sum){

            for(Map.Entry<Integer,Integer> e :map.entrySet()){
                Integer key = e.getKey();
                int t = sum - key;

                //当前值不相等
                if(key!=t&&map.containsKey(t)){
                    return true;
                }

                if(key==t&&e.getValue()>1){
                    return true;
                }
            }
            return false;
        }
    }



    /**
     * find 多 add少
     */

    public static class dTwoSum{
        //遍历
        private HashSet<Integer> nums = new HashSet<>();

        //存两数相加集合
        private HashSet<Integer> sums = new HashSet<>();



        public void add(int num){

        }

    }
}







