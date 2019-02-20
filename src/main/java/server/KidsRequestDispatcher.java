package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import server.internal.IRequestDispatcher;

public class KidsRequestDispatcher implements IRequestDispatcher {
    @Override
    public void dispatch(ChannelHandlerContext ctx, FullHttpRequest req) {

    }
}
