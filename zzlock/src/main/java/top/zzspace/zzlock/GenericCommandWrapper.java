package top.zzspace.zzlock;

/**
 * 执行包装
 * @author zujool  At 2018/9/19 16:14
**/
@FunctionalInterface
public interface GenericCommandWrapper<T>{

    T executeGeneric() throws Exception;

}
