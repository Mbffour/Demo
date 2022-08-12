package demo.dataStructure;

import java.util.LinkedList;

/**
 * @author ：mbf
 * @date ：2022/8/2
 */
public class LinkNodeDemo {


    public int size;
    public TNode head;
    public TNode tail;



    public void remove(Object o){
        if(head ==null){
            return;
        }

        TNode t = head;
        while (t !=null){
            if(t.val.equals(o)){
                //是不是头尾节点
                if(t == tail){
                    tail.pre.next = null;
                    tail = tail.pre;
                }else if(t ==  head){
                    head.next.pre = null;
                    head = head.next;
                }else{
                    t.pre.next = t.next;
                    t.next.pre = t.pre;
                }
                size --;
                return;
            }
            t = t.next;
        }
    }

//    public void remove(int index){
//
//    }

    public void add(int index,Object o){
        if(index< 0 || index>size){
            throw new RuntimeException("下标错误");
        }
        //尾节点
        if(index == size){
            add(o);
            return;
        }
        //找到对应的节点
        int start = 0;
        TNode target = head;
        while (start < index){
            target = target.next;
            start++;
        }
        //创建新节点
        TNode pre =   target.pre;
        pre.next = new TNode(target,pre,o);
        target.pre = pre.next;
        //尾节点不需要变
        size++;
    }
    public void add(Object o){
        //init head
        if(head ==null){
            head = new TNode(null,null,o);
            tail = head;
            size ++;
            return;
        }

        //doNext 新节点
        tail.next = new TNode(null,tail,o);
        tail = tail.next;
        size ++;

    }

    @Override
    public String toString() {
        if(head ==null){
            return "";
        }
        System.out.println("size:"+size);
        StringBuilder str = new StringBuilder();
        str.append("[");
        TNode t = head;
        while (t!=null){
            str.append(t.val);
            t = t.next;
            if(t != null){
                str.append(",");
            }
        }

        str.append("]");
        return str.toString();
    }

    public static void main(String[] args){

//        LinkedList linkedList = new LinkedList();
//        linkedList.re
        LinkNodeDemo linkList = new LinkNodeDemo();
        linkList.add(1);
        linkList.add(2);
        linkList.add(3);
        linkList.add(4);
        System.out.println(linkList);

        linkList.add(4,90);
        System.out.println(linkList);

        //remove
        linkList.remove(1);
        System.out.println(linkList);
        linkList.remove(90);
        System.out.println(linkList);
        linkList.remove(3);
        System.out.println(linkList);
    }
    public static class TNode{
        public TNode next;
        public TNode pre;
        public Object val;

        public TNode(TNode next, TNode pre, Object val) {
            this.next = next;
            this.pre = pre;
            this.val = val;
        }
    }
}
