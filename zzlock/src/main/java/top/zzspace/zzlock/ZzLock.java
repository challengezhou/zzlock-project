package top.zzspace.zzlock;

import top.zzspace.zzlock.exception.ZzLockFailedException;

/**
 * @author zujool  At 2018/9/19 16:00
 **/
public class ZzLock {

    private LockExecutor executor;

    private static final int DEFAULT_LOCK_FAILED = -1;

    private ThreadLocal<String> lockKeyHolder = new ThreadLocal<>();
    private ThreadLocal<Boolean> lockedHolder = ThreadLocal.withInitial(() -> false);

    private String lockKeyPrefix;
    /**
     * 锁超时，防止加锁以后，释放失败造成无法再获得锁
     */
    private int expireMs = 20 * 1000;
    /**
     * 锁等待，重试时间
     */
    private int timeoutMs = 4 * 1000;

    public ZzLock() {
    }

    public ZzLock(LockExecutor executor) {
        this.executor = executor;
    }

    public ZzLock(LockExecutor executor, int timeoutMs, int expireMs) {
        this.executor = executor;
        if (0 != timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
        if (0 != expireMs) {
            this.expireMs = expireMs;
        }
    }

    public String getLockKeyPrefix() {
        return lockKeyPrefix;
    }

    public void setLockKeyPrefix(String lockKeyPrefix) {
        if (null == lockKeyPrefix) {
            this.lockKeyPrefix = "ZzLock";
        } else {
            this.lockKeyPrefix = lockKeyPrefix;
        }
    }

    public int getExpireMs() {
        return expireMs;
    }

    public void setExpireMs(int expireMs) {
        this.expireMs = expireMs;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    private void setLockKey(String lockKey) {
        if (isLocked()){
            throw new ZzLockFailedException("Not support reentrant lock yet");
        }
        if (null == lockKey || lockKey.length() == 0) {
            throw new RuntimeException("lockKey must not be null");
        }
        lockKeyHolder.set(lockKeyPrefix + "." + lockKey);
    }

    public String getLockKey() {
        return lockKeyHolder.get();
    }

    private void setLocked(Boolean locked) {
        lockedHolder.set(locked);
    }

    public Boolean isLocked() {
        return lockedHolder.get();
    }


    /**
     * acquire lock. retry until acquire timeout
     *
     * @param lockKey the lock key
     * @return true if lock is acquired, false acquire timeout
     * @throws InterruptedException interrupt lock operation
     */
    public boolean acquire(String lockKey) throws InterruptedException {
        setLockKey(lockKey);
        return acquire(timeoutMs, expireMs);
    }

    /**
     * grab lock. will return immediately
     *
     * @param lockKey the lock key
     * @return true if lock is grabbed, false grab failed
     */
    public boolean grab(String lockKey) {
        setLockKey(lockKey);
        return grab(expireMs);
    }

    /**
     * Acquire lock.
     *
     * @return true if lock is acquired, false acquire timeout
     */
    private boolean acquire(int timeoutMs, int expireMs) throws InterruptedException {
        checkRedisExecutor();
        while (timeoutMs >= 0) {
            long currentLockExpireTimestamp = System.currentTimeMillis() + expireMs + 1;
            //锁到期时间
            String currentLockExpireTimestampStr = String.valueOf(currentLockExpireTimestamp);
            boolean quickLock = executor.setIfNotExist(getLockKey(), currentLockExpireTimestampStr);
            if (quickLock) {
                // lock acquired
                setLocked(true);
                return true;
            }
            //原有锁的过期时间
            String oldLockExpireTimestampStr = executor.get(getLockKey());
            boolean grabbed = checkGrabbed(oldLockExpireTimestampStr, currentLockExpireTimestampStr);
            if (grabbed) {
                return true;
            }
            timeoutMs -= 50;
            Thread.sleep(50);
        }
        return false;
    }

    /**
     * grab lock.
     *
     * @return true if lock is grab, false grab failed
     */
    private boolean grab(int expireMs) {
        checkRedisExecutor();
        long expires = System.currentTimeMillis() + expireMs + 1;
        //lock expire
        String expiresStr = String.valueOf(expires);

        if (executor.setIfNotExist(getLockKey(), expiresStr)) {
            // lock grabbed
            setLocked(true);
            return true;
        }
        //lock time
        String oldLockExpireTimestampStr = executor.get(getLockKey());
        return checkGrabbed(oldLockExpireTimestampStr, expiresStr);
    }

    /**
     * 进行抢锁的检查
     *
     * @param oldLockExpireTimestampStr     原有锁存储的时间戳字符串，用于过期检查
     * @param currentLockExpireTimestampStr 当前锁要设置的时间戳字符串，此锁的过期时间
     * @return 是否得到锁
     */
    private boolean checkGrabbed(String oldLockExpireTimestampStr, String currentLockExpireTimestampStr) {
        //当上一个锁超时后，进行抢锁操作
        if (oldLockExpireTimestampStr != null && Long.parseLong(oldLockExpireTimestampStr) < System.currentTimeMillis()) {
            //获取上一个锁到期时间，并设置现在的锁到期时间，
            //需保证只有一个线程能获取上一个锁设置的到期时间，因此getAndSet需要是原子操作
            String oldValueStr = executor.getAndSet(getLockKey(), currentLockExpireTimestampStr);
            //如过这个时候，多个线程恰好都到了这里，但是只有一个线程的设置值和当前值相同，他才有权利获取锁
            if (oldValueStr != null && oldValueStr.equals(oldLockExpireTimestampStr)) {
                // lock grabbed
                setLocked(true);
                return true;
            }
        }
        return false;
    }

    public void release() {
        checkRedisExecutor();
        if (isLocked()) {
            executor.del(getLockKey());
            lockKeyHolder.remove();
            lockedHolder.remove();
            executor.release();
        }
    }

    /**
     * release the locked although the locked is false
     */
    public void forceRelease() {
        setLocked(true);
        release();
    }

    public <T> T thinWrap(String lockKey, GenericCommandWrapper<T> wrapper) {
        setLockKey(lockKey);
        return thinWrap(wrapper, this.timeoutMs, this.expireMs);
    }

    public <T> T thinWrap(String lockKey, GenericCommandWrapper<T> wrapper, int timeoutMs, int expireMs) {
        setLockKey(lockKey);
        return thinWrap(wrapper, timeoutMs, expireMs);
    }

    private <T> T thinWrap(GenericCommandWrapper<T> wrapper, int timeoutMs, int expireMs) {
        try {
            if (acquire(timeoutMs, expireMs)) {
                return wrapper.executeGeneric();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            release();
        }
        throw new ZzLockFailedException("zzlock lock timeout");
    }

    public ExecutionResult wrap(String lockKey, CommandWrapper wrapper, int timeoutMs, int expireMs) {
        setLockKey(lockKey);
        return wrap(wrapper, DEFAULT_LOCK_FAILED, timeoutMs, expireMs);
    }

    public ExecutionResult wrap(String lockKey, CommandWrapper wrapper, Integer lockFailedCode) {
        setLockKey(lockKey);
        return wrap(wrapper, lockFailedCode, this.timeoutMs, this.expireMs);
    }

    public ExecutionResult wrap(String lockKey, CommandWrapper wrapper) {
        setLockKey(lockKey);
        return wrap(wrapper, DEFAULT_LOCK_FAILED, this.timeoutMs, this.expireMs);
    }

    private ExecutionResult wrap(CommandWrapper wrapper, Integer lockFailedCode, int timeoutMs, int expireMs) {
        try {
            if (acquire(timeoutMs, expireMs)) {
                return wrapper.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            release();
        }
        return new ExecutionResult(lockFailedCode, "zzlock failed", null);
    }

    private void checkRedisExecutor(){
        if (null == executor) {
            throw new RuntimeException("LockExecutor can not be null!");
        }
    }

}
