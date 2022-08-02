package demo.arithmetic;

import java.util.List;

/**
 * @author ：mbf
 * @date ：2022/8/1
 */
public class NodeDemo {


    public static void main(String[] args){
        ListNode listNode = new ListNode(9, new ListNode(9));
        ListNode listNode2 = new ListNode(5);
        ListNode listNode1 = addTwoNumbers(listNode, listNode2);

        System.out.println(listNode1);
    }
    public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {

        ListNode head = new ListNode();
        ListNode cur = head;
        int jinwei =0;
        while(l1!=null || l2 !=null){
            int a = l1==null?0:l1.val;
            int b = l2==null?0:l2.val;
            int result = a+b + jinwei;

            if(result>=10){
                jinwei = 1;
                result = result-10;
            } else{
                jinwei = 0;
            }
            cur.next = new ListNode(result);
            cur = cur.next;

            if(l1!=null){
                l1 = l1.next;
            }
            if(l2 !=null ){
                l2 = l2.next;
            }
        }

        if(jinwei>0){
            cur.next = new ListNode(jinwei);
        }

        return head.next;
    }


    public static class ListNode {
      int val;
      ListNode next;
      ListNode() {}
      ListNode(int val) { this.val = val; }
     ListNode(int val, ListNode next) { this.val = val; this.next = next; }
  }


}
