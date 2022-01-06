package dev.nova.gameapi.game.base.init;

public class InitResultInfo {

    private final Result result;
    private final Throwable throwable;

    public enum Result {
        
        SUCCESS,
        FAILED
        
    }
    
    public InitResultInfo(Result result, Throwable throwable){
        this.result = result;
        this.throwable = throwable;
    }
    
    public InitResultInfo() {
        this(Result.SUCCESS,null);
    }
    
    public InitResultInfo(Result result){
        this(result,null);
    }

    public Result getResult() {
        return result;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
