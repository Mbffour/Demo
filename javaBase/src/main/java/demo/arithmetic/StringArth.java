package demo.arithmetic;

import java.util.*;

/**
 * @author ：mbf
 * @date ：2022/8/1
 */
public class StringArth {

    //请实现一个函数，将一个字符串中的每个空格替换成“%20”。例如，当字符串为We Are Happy.则经过替换之后的字符串为We%20Are%20Happy。

    public static String replaceSpace(String str){
        StringBuilder strBuilder = new StringBuilder();

        for(int i=0;i<str.length();i++){

            String substring = str.substring(i, i + 1);

            if(substring.equals(" ")){
                strBuilder.append("%20");
            }else{
                strBuilder.append(substring);
            }
        }
        return strBuilder.toString();
    }

    // 编写一个函数来查找字符串数组中的最长公共前缀。如果不存在公共前缀，返回空字符串 ""
    // 输入: ["flower","flow","flight"]
    // 输出: "fl"

    public static String maxComPreStr(String[] array){
        if(array.length == 0){
            return "";
        }
        int[] result = new int[2];
        String one = array[0];

        for(int i = 1; i< array.length;i++){
            //找公共前缀
            while (array[i].indexOf(one)!=0){
                //长度减一
                one = one.substring(0,one.length()-1);
                if(one.length() == 0) {
                    return "";
                }
            }
        }
        return one;
    }



    /*
    最长回文串
    字符出现次数为双数的组合
    字符出现次数为偶数的组合+单个字符中出现次数最多且为奇数次的字符
     */

    public static int maxHuiWen(String str){
        char[] chars = str.toCharArray();

        Set<Character> set = new HashSet<>();
        int count = 0;
        for(char c : chars){
            if(!set.contains(c)){
                set.add(c);
            }else{
                set.remove(c);
                count++;
            }
        }
        return set.isEmpty()?count * 2:count*2+1;
    }

    /*
    验证
    给定一个字符串，验证它是否是回文串，只考虑字母和数字字符，可以忽略字母的大小写。 说明：本题中，我们将空字符串定义为有效的回文串。
输入: "A man, a plan, a canal: Panama"
输出: true
     */
    public static boolean checkHuiwen(String str){
        //忽略大小写
        str = str.toLowerCase();
        //字母和数字
        int i=0;
        int j=str.length()-1;
        // aba    0 1    0
        while (i<j){

            boolean flag = true;
            //连续两个都是其他字符的情况
            if(!Character.isLetterOrDigit(str.charAt(i))){
                i++;
                flag =false;
            }
            if(!Character.isLetterOrDigit(str.charAt(j))){
                j--  ;
                flag = false;
            }

            if(flag){
                char c = str.charAt(i);
                char d = str.charAt(j);
                if(c!=d){
                    return false;
                } else {
                    i++;
                    j--;
                }
            }
        }
        return true;
    }


    /*
    Leetcode: LeetCode: 最长回文子串 给定一个字符串 s，找到 s 中最长的回文子串。你可以假设 s 的最大长度为1000。
输入: "babad"
输出: "bab"
注意: "aba"也是一个有效答案。
     */

    public static String maxHuiWenStr(String str){


        String s = "abccba";



        return "";
    }



    public static int lengthOfLongestSubstring(String s) {
        int result = 0;
        Set<Character> set = new HashSet();
        for(char c:s.toCharArray()){
            if(set.contains(c)){
                result = set.size() > result? set.size():result;
                set.clear();
                set.add(c);
            }else{
                set.add(c);
            }
        }
        return result;
    }



    public static void main(String[] args){
        lengthOfLongestSubstring(" ");
//        System.out.println(checkHuiwen("A man, a plan, a canal: Panama"));
//        System.out.println(maxHuiWen("abccccdd"));
//        String str = "We Are Happy";
//        String s = replaceSpace(str);
//        System.out.println(s);
//
//        String[] arry =new String[]{"abc","flow","flight"};
//        System.out.println(maxComPreStr(arry));

        String s ="a";
        int index = s.length()-1;
        String substring = s.substring(0, index);
        System.out.println(substring);

    }
}
