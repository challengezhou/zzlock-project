package top.zzspace.zzlock;

/**
 * 
 * @author zujool  At 2018/9/19 16:09
**/
public interface LockExecutor {

    /**
     * 如果key不存在，将key设置为value
     *
     * @param key   key
     * @param value value
     * @return 1 成功 0 失败
     */
    Boolean setIfNotExist(String key, String value);

    /**
     * 获取key中存储的时间
     *
     * @param key key
     * @return key
     */
    String get(String key);

    /**
     * 把key设置为value返回原有值
     *
     * @param key   key
     * @param value value
     * @return key原有值
     */
    String getAndSet(String key, String value);

    /**
     * 删除key
     *
     * @param key 删除
     */
    void del(String key);

}
