package top.zzspace.zzlock.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 
 * @author zujool  At 2018/9/19 15:56
**/
@ConfigurationProperties(prefix = "zzlock")
public class ZzLockProperties {

    private String lockKeyPrefix;

    /**
     * 尝试获取多长时间的锁
     */
    private int timeoutMs = 0;

    /**
     * 当前锁超时时间
     * 这个时间加当前毫秒时间戳，存入key中，当key已占用时，使用这个时间戳与当前时间戳比较，若前者小于后者，则说明此key原来的锁已过期
     */
    private int expireMs = 0;

    /**
     * key的超时时间，多久从库中删除。与上边不同的是，这是一个保底机制，避免释放key出现问题时，一堆key没有删除
     */
    private int keyExpireMs = 7200 * 1000;

    public int getKeyExpireMs() {
        return keyExpireMs;
    }

    public void setKeyExpireMs(int keyExpireMs) {
        this.keyExpireMs = keyExpireMs;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getExpireMs() {
        return expireMs;
    }

    public void setExpireMs(int expireMs) {
        this.expireMs = expireMs;
    }

    public String getLockKeyPrefix() {
        return lockKeyPrefix;
    }

    public void setLockKeyPrefix(String lockKeyPrefix) {
        this.lockKeyPrefix = lockKeyPrefix;
    }
}
