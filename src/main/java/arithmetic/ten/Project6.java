package arithmetic.ten;

import java.util.Arrays;

public class Project6 {



    public static void main(String[] args){

        Project6 p = new Project6();


        int[] pre = new int[]{1,2,4,7,3,5,6,8};
        int[] in = new int[] {4,7,2,1,5,3,8,6};

        p.reConstructBinaryTree(pre,in);
    }


    public TreeNode reConstructBinaryTree(int [] pre,int [] in) {
        TreeNode root=reConstructBinaryTree(pre,0,pre.length-1,in,0,in.length-1);
        return root;
    }
    //前序遍历{1,2,4,7,3,5,6,8}和中序遍历序列{4,7,2,1,5,3,8,6}
    private TreeNode reConstructBinaryTree(int [] pre,int startPre,int endPre,int [] in,int startIn,int endIn) {

        if(startPre>endPre||startIn>endIn)
            return null;
        TreeNode root=new TreeNode(pre[startPre]);

        for(int i=startIn;i<=endIn;i++)

        /**
         * 在右节点时 下标  i-startIn 为 增长的值
         *
         * i-startIn + startPre+1  右节点 截取后的值
         *
         * in 数组根据i 分成两份就行
         *
         * pre 数组分成两份后要向后移动
         */
            if(in[i]==pre[startPre]){
                root.left=reConstructBinaryTree(pre,startPre+1,startPre+i-startIn,in,startIn,i-1);
                root.right=reConstructBinaryTree(pre,i-startIn+startPre+1,endPre,in,i+1,endIn);
                break;
            }
        return root;
    }

    /**
     * 前序遍历：根结点 ---> 左子树 ---> 右子树   {1,2,4,7,3,5,6,8}
     *
     * 中序遍历：左子树---> 根结点 ---> 右子树    {4,7,2,1,5,3,8,6}
     *
     * 后序遍历：左子树 ---> 右子树 ---> 根结点
     *
     * 层次遍历：仅仅需按层次遍历就可以
     *
     * @param pre
     * @param in
     * @return
     */
    //输入某二叉树的前序遍历和中序遍历的结果，请重建出该二叉树。
    // 假设输入的前序遍历和中序遍历的结果中都不含重复的数字。
    // 例如输入前序遍历序列{1,2,4,7,3,5,6,8}
    // 和中序遍历序列{4,7,2,1,5,3,8,6}，则重建二叉树并返回。

    public TreeNode reConstructBinaryTree2(int [] pre,int [] in) {

        if (pre == null || in == null) {
            return null;
        }
        if (pre.length == 0 || in.length == 0) {
            return null;
        }
        if (pre.length != in.length) {
            return null;
        }

        //abcdef
        TreeNode rootNode = new TreeNode(pre[0]);

        for(int i=0;i<in.length;i++){
            //划分左右子数
            if(in[i]==pre[0]){
                rootNode.left=reConstructBinaryTree2(
                        Arrays.copyOfRange(pre,1,i+1)
                        ,Arrays.copyOfRange(in,0,i));
                rootNode.right=reConstructBinaryTree2(
                        Arrays.copyOfRange(pre,i+1,pre.length)
                        ,Arrays.copyOfRange(in,i+1,in.length));
            }
        }
        return rootNode;
    }


    public static class TreeNode{
        int val;
         TreeNode left;
         TreeNode right;

        public TreeNode(int val) {
            this.val = val;
        }
    }
}
