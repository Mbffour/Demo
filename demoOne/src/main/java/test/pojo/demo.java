package test.pojo;

import java.util.concurrent.ConcurrentHashMap;

public class demo {



    public static void main(String[] args){
        ConcurrentHashMap<Long,Le> leMap = new ConcurrentHashMap<>();

        ConcurrentHashMap<Long,User> userMap = new ConcurrentHashMap<>();


        User user1 = new User();
        user1.setName("user1");

        userMap.putIfAbsent(321l,user1);



        Club club1 = new Club();
        club1.setName("club1");

        club1.setUsers(new ConcurrentHashMap<>());
        club1.getUsers().putIfAbsent(1l,user1);

        Le l1 = new Le();
        l1.getClubs().putIfAbsent(201l,club1);



        leMap.put(301l,l1);





        Le le = leMap.get(301l);


        Club club = le.getClub(201l);


        User user = club.getUsers().get(1l);



        System.out.println(user);



        user1.setName("change");


        /**
         * 同一份引用
         */
        System.out.println(user);


        User user2 = userMap.get(321l);

        System.out.println(user2);


    }
}
