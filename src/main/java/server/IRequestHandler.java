package server;

public interface IRequestHandler {
    public void handle(KidsContext ctx, KidsRequest req);
}
