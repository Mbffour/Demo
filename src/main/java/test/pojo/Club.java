package test.pojo;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Club {

    private String name ;


    private volatile Long  CoinNum;



    private ConcurrentHashMap<Long,User> users;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConcurrentHashMap<Long, User> getUsers() {
        return users;
    }

    public void setUsers(ConcurrentHashMap<Long, User> users) {
        this.users = users;
    }


    /**
     * 写操作加锁
     * @param num
     */
    public void addCoin(long num) {
        synchronized (this){
            this.CoinNum = CoinNum +num;
        }
    }
}
