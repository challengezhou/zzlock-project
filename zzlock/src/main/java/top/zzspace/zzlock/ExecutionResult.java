package top.zzspace.zzlock;

/**
 * 
 * @author zujool  At 2018/9/19 16:17
**/
public class ExecutionResult {

    private int code;

    private String msg;

    private Object body;

    public ExecutionResult() {
    }

    public ExecutionResult(int code, String msg, Object body) {
        this.code = code;
        this.msg = msg;
        this.body = body;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
