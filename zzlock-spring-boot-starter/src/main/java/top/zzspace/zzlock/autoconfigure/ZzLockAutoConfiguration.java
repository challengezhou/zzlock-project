package top.zzspace.zzlock.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import top.zzspace.zzlock.LockExecutor;
import top.zzspace.zzlock.ZzLock;

import java.util.concurrent.TimeUnit;

/**
 * auto config
 *
 * @author zujool  At 2018/9/19 15:42
 **/
@Configuration
@ConditionalOnBean(StringRedisTemplate.class)
@EnableConfigurationProperties(ZzLockProperties.class)
public class ZzLockAutoConfiguration {

    private final ZzLockProperties properties;

    public ZzLockAutoConfiguration(ZzLockProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public LockExecutor lockExecutor(StringRedisTemplate stringRedisTemplate) {
        return new LockExecutor() {
            @Override
            public Boolean setIfNotExist(String key, String value) {
                Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
                if (null != result && result && 0 != properties.getKeyExpireMs()) {
                    stringRedisTemplate.expire(key, properties.getKeyExpireMs(), TimeUnit.MILLISECONDS);
                }
                return result;
            }

            @Override
            public String get(String key) {
                return stringRedisTemplate.opsForValue().get(key);
            }

            @Override
            public String getAndSet(String key, String value) {
                String result = stringRedisTemplate.opsForValue().getAndSet(key, value);
                if (null != result && !"".equals(result) && 0 != properties.getKeyExpireMs()) {
                    stringRedisTemplate.expire(key, properties.getKeyExpireMs(), TimeUnit.MILLISECONDS);
                }
                return result;
            }

            @Override
            public void del(String key) {
                stringRedisTemplate.delete(key);
            }

            @Override
            public void release() {

            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ZzLock zzLock(LockExecutor lockExecutor) {
        ZzLock zzLock = new ZzLock(lockExecutor, properties.getTimeoutMs(), properties.getExpireMs());
        zzLock.setLockKeyPrefix(properties.getLockKeyPrefix());
        return zzLock;
    }

}
