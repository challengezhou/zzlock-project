package top.zzspace.zzlock;

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
    private int expireMs = 10 * 1000;
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
        this.lockKeyPrefix = lockKeyPrefix;
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
        lockKeyHolder.set(lockKeyPrefix + lockKey);
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
     * @return true if lock is acquired, false acquire timeout
     */
    public boolean acquire(String lockKey) throws InterruptedException {
        setLockKey(lockKey);
        return acquire(0, 0);
    }

    /**
     * grab lock. will return immediately
     *
     * @return true if lock is grabbed, false grab failed
     */
    public boolean grab(String lockKey) {
        setLockKey(lockKey);
        return grab(0);
    }

    /**
     * Acquire lock.
     *
     * @return true if lock is acquired, false acquire timeout
     */
    private boolean acquire(int timeoutMs, int expireMs) throws InterruptedException {
        if (null == executor) {
            throw new RuntimeException("RedisExecutor can not be null!");
        }
        int timeout = this.timeoutMs;
        int expire = this.expireMs;
        if (0 != timeoutMs) {
            timeout = timeoutMs;
        }
        if (0 != expireMs) {
            expire = expireMs;
        }
        while (timeout >= 0) {
            long currentLockTimeoutStamp = System.currentTimeMillis() + expire + 1;
            //锁到期时间
            String currentLockTimeoutStampStr = String.valueOf(currentLockTimeoutStamp);
            boolean quickLock = executor.setIfNotExist(getLockKey(), currentLockTimeoutStampStr);
            if (quickLock) {
                // lock acquired
                setLocked(true);
                return true;
            }
            //原有锁的过期时间
            String oldLockTimeoutStampStr = executor.get(getLockKey());
            boolean grabbed = checkGrabbed(oldLockTimeoutStampStr, currentLockTimeoutStampStr);
            if (grabbed) {
                return true;
            }
            timeout -= 100;
            Thread.sleep(100);
        }
        return false;
    }

    /**
     * grab lock.
     *
     * @return true if lock is grab, false grab failed
     */
    private boolean grab(int expireMs) {
        if (null == executor) {
            throw new RuntimeException("LockExecutor can not be null!");
        }
        int expire = this.expireMs;
        if (0 != expireMs) {
            expire = expireMs;
        }
        long expires = System.currentTimeMillis() + expire + 1;
        //lock timeout
        String expiresStr = String.valueOf(expires);

        if (executor.setIfNotExist(getLockKey(), expiresStr)) {
            // lock grabbed
            setLocked(true);
            return true;
        }
        //lock time
        String currentValueStr = executor.get(getLockKey());
        return checkGrabbed(currentValueStr, expiresStr);
    }

    /**
     * 进行抢锁的检查
     *
     * @param oldLockTimeoutStampStr     原有锁存储的时间戳字符串，用于过期检查
     * @param currentLockTimeoutStampStr 当前锁要设置的时间戳字符串，此锁的过期时间
     * @return 是否得到锁
     */
    private boolean checkGrabbed(String oldLockTimeoutStampStr, String currentLockTimeoutStampStr) {
        //如果被其他线程设置了值，存储的过期时间不会超时
        if (oldLockTimeoutStampStr != null && Long.parseLong(oldLockTimeoutStampStr) < System.currentTimeMillis()) {
            //获取上一个锁到期时间，并设置现在的锁到期时间，
            //只有一个线程能获取上一个抢锁操作设置的过期时间，因此getAndSet需要是原子操作
            String oldValueStr = executor.getAndSet(getLockKey(), currentLockTimeoutStampStr);
            //如过这个时候，多个线程恰好都到了这里，但是只有一个线程的设置值和当前值相同，他才有权利获取锁
            if (oldValueStr != null && oldValueStr.equals(oldLockTimeoutStampStr)) {
                // lock grabbed
                setLocked(true);
                return true;
            }
        }
        return false;
    }

    public void release() {
        release(executor);
        lockKeyHolder.remove();
        lockedHolder.remove();
    }

    /**
     * release the locked although the locked is false
     */
    public void forceRelease() {
        setLocked(true);
        release(executor);
    }

    /**
     * release the lock
     */
    private void release(LockExecutor executor) {
        if (null == executor) {
            throw new RuntimeException("RedisExecutor can not be null!");
        }
        if (isLocked()) {
            executor.del(getLockKey());
            setLocked(false);
        }
    }

    public ExecutionResult wrap(String lockKey, CommandWrapper wrapper, int timeoutMs, int expireMs) {
        lockKeyHolder.set(lockKey);
        return wrap(wrapper, DEFAULT_LOCK_FAILED, timeoutMs, expireMs);
    }

    public ExecutionResult wrap(String lockKey, CommandWrapper wrapper, Integer lockFailedCode) {
        lockKeyHolder.set(lockKey);
        return wrap(wrapper, lockFailedCode, 0, 0);
    }

    public ExecutionResult wrap(String lockKey, CommandWrapper wrapper) {
        lockKeyHolder.set(lockKey);
        return wrap(wrapper, DEFAULT_LOCK_FAILED,0,0);
    }

    private ExecutionResult wrap(CommandWrapper wrapper, Integer lockFailedCode, int timeoutMs, int expireMs) {
        try {
            String lockKey = getLockKey();
            if (null == lockKey || "".equals(lockKey)) {
                return wrapper.execute();
            }
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

}
