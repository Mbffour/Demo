package arithmetic.ten;

import java.util.ArrayList;
import java.util.List;

public class Project4 {


    //请实现一个函数，将一个字符串中的每个空格替换成“%20”。
    //例如，当字符串为We Are Happy.则经过替换之后的字符串为We%20Are%20Happy。
    public String replaceSpace(StringBuffer str) {
        //记录空格的位置 填充字符
        if(str==null&&str.length()==0){
            return null;
        }
        StringBuilder newStr = new StringBuilder();
        for(int i=0;i<str.length();i++){
            if(String.valueOf(str.charAt(i)).equals(" ")){

                newStr.append("%20");
            }else {
                newStr.append(str.charAt(i));
            }
        }
        return newStr.toString();
    }
}
