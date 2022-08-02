package arithmetic.leetcode.array_string;

import java.util.LinkedList;

public class ValidPalindrome {




    public static void main(String[] args){
        String str = "OP";

        System.out.println(isPalindrome(str));
    }


    /**
     * Valid Palindrome II
     * Given a non-empty string s, you may delete at most one character. Judge whether you can make it a palindrome.
     * @param s
     * @return
     */
    public boolean validPalindrome(String s) {
        int left = 0;
        int right = s.length()-1;

        while(left<right){

            if(Character.toLowerCase(s.charAt(left))!=Character.toLowerCase(s.charAt(right))){
                if(Character.toLowerCase(s.charAt(left+1))!=Character.toLowerCase(s.charAt(right))){
                    return false;
                }else{
                    left++;
                }

                if(Character.toLowerCase(s.charAt(left))!=Character.toLowerCase(s.charAt(right-1))){
                    return false;
                }else{
                    right--;
                }
            }
            left++;
            right--;
        }

        return true;
    }





    /**
     * Valid Palindrome
     * Given a string, determine if it is a palindrome, considering only alphanumeric characters and ignoring cases.
     *
     * Note: For the purpose of this problem, we define empty string as valid palindrome.
     * @return
     */
    public static boolean isPalindrome(String s) {


        //65-90  97-122
        char[] chars = s.toCharArray();

        int left=0;
        int right=chars.length-1;


        while(left<right){

            int a =chars[left];
            int b =chars[right];

            if(a==b||a-b==32||a-b==-32){
                left++;
                right--;
                continue;
            }

            //a 和 b 都合法
            if((a>=65&&a<=90||a>=97&&a<=122)&&
                    (b>=65&&b<=90||b>=97&&b<122)){
                return false;
            }

            if(a<65 ||
                    (a>90&&a<97)
                    ||a>122){
                left++;
            }

            if(b<65||
                    (b>90&&b<97)
                    ||b>122){
                right--;
            }

        }
        return true;
    }


    public static boolean isPalindrome2(String s) {

        int left=0,right=s.length()-1;

        while(left<right){
            while(left<right && !Character.isLetterOrDigit(s.charAt(left))) left++;
            while(left<right && !Character.isLetterOrDigit(s.charAt(right))) right--;
            if(Character.toLowerCase(s.charAt(left))
                    !=Character.toLowerCase(s.charAt(right))){
                return false;
            }
            left++;
            right--;
        }
        return  true;


    }






    /*
    Given a string containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.

    An input string is valid if:

    Open brackets must be closed by the same type of brackets.
    Open brackets must be closed in the correct order.
    Note that an empty string is also considered valid.
     */
    public boolean isValid(String s) {
        LinkedList<Character> list = new LinkedList();


        for(char c : s.toCharArray()){
            char tartget = transform(c);
            if(list.size()>0&&list.getFirst()==tartget){
                list.pop();
            }else{
                list.push(c);
            }
        }

        if(list.size()>0) return false;
        else return true;
    }

    private char transform(char c) {
        if(c==')')return '(';
        if(c=='}')return '{';
        if(c==']')return '[';
        return 'a';
    }
}
