package test.pojo;


/**
 * 联盟 id + 俱乐部id + userid
 */
public class User {

    private volatile  String name;

    /**
     * 保证写入操作加锁
     */
    private volatile  Long coinNum;


    public Long getCoinNum() {
        return coinNum;
    }

    public void setCoinNum(Long coinNum) {
        this.coinNum = coinNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                '}';
    }
}
