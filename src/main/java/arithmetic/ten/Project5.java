package arithmetic.ten;

import java.util.ArrayList;

public class Project5 {


    //输入一个链表，从尾到头打印链表每个节点的值。
    public ArrayList<Integer> printListFromTailToHead(ListNode listNode) {
        ArrayList<Integer> resultList = new ArrayList<Integer>();

        if(listNode==null){
            return resultList;
        }

        ArrayList<Integer> list = new ArrayList<Integer>();


        do{
            list.add(listNode.val);
            listNode = listNode.next;
        }
        while(listNode!=null);


        for(int i=list.size()-1;i>=0;i--){
            resultList.add(list.get(i));
        }

        return  resultList;
    }



    ArrayList<Integer> resultList = new ArrayList<Integer>();
    //递归
    public ArrayList<Integer> printListFromTailToHead2(ListNode listNode) {


        if(listNode!=null){
            printListFromTailToHead2(listNode.next);
            resultList.add(listNode.val);
        }

        return resultList;
    }

    public static class ListNode{
        int val;

        ListNode next = null;
        ListNode(int val) {
            this.val = val;
        }
    }
}
