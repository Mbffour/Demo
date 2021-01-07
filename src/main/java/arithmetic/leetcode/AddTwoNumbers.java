package arithmetic.leetcode;

import java.util.ArrayList;
import java.util.List;

public class AddTwoNumbers {






    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {

        //1 找最后一个节点

        int index =0;

        List<Integer> alist = new ArrayList<>();
        List<Integer> blist = new ArrayList<>();

        while(l1!=null){
            alist.add(l1.val);
            l1 = l1.next;
        }
        while(l2!=null){
            blist.add(l2.val);
            l2 = l2.next;
        }

        return null;
    }



    public class ListNode {
     int val;
     ListNode next;
     ListNode(int x) { val = x; }
  }


}
