package server;

public interface IExceptionHandler {
    public void handle(KidsContext ctx, AbortException e);
}
