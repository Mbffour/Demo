package test.pojo;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 联盟中 所有操作是要加锁的吗
 * 1 给俱乐部加币 要获取俱乐部的锁
 * 2 俱乐部添加 玩家的授信  加锁
 *
 * 3 统计所有值 get 操作 无锁
 *
 */
public class Le {
    private  ConcurrentHashMap<Long,Club> Clubs = new ConcurrentHashMap<>();


    /**
     * 俱乐部加锁
     * @param clubid
     * @param num
     * @return
     */
    public boolean addCoin(long clubid,long num){
        Club club = Clubs.get(clubid);

        club.addCoin(num);

        return true;
    }


    public Club getClub(long id) {
        return Clubs.get(id);
    }




    public ConcurrentHashMap<Long,Club> getClubs() {
        return Clubs;
    }

    public void setClubs(ConcurrentHashMap<Long,Club> clubs) {
        Clubs = clubs;
    }
}
