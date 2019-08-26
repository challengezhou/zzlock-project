package top.zzspace.zzlock.exception;

/**
 * 
 * @author zujool  At 2019/8/16 19:20
**/
public class ZzLockFailedException extends RuntimeException{

    public ZzLockFailedException(String errorMsg){
        super(errorMsg);
    }

}
