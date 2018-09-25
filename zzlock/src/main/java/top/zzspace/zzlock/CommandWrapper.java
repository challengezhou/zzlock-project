package top.zzspace.zzlock;

/**
 * 执行包装
 * @author zujool  At 2018/9/19 16:14
**/
public interface CommandWrapper {

    /**
     * execute the command
     * @return RespMsg
     */
    ExecutionResult execute();

}
