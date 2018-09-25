package top.zzspace.zzlock.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 
 * @author zujool  At 2018/9/19 15:56
**/
@ConfigurationProperties(prefix = ZzLockProperties.ZZLOCK_CONFIG_PREFIX)
public class ZzLockProperties {

    public static final String ZZLOCK_CONFIG_PREFIX = "zzlock";

    private int timeoutMs = 0;

    private int expireMs = 0;

    private String lockKeyPrefix;

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
