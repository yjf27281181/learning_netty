package server.internal;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {
    private final static Logger LOG = LoggerFactory.getLogger(HttpServer.class);

    private String ip;
    private int port;
    private int ioThreads;
    private int workerThreads;
    private IRequestDispatcher dispatcher;

    public HttpServer(String ip, int port, int ioThreads, int workerThreads, IRequestDispatcher dispatcher) {
        this.ip = ip;
        this.port = port;
        this.ioThreads = ioThreads;
        this.workerThreads = workerThreads;
        this.dispatcher = dispatcher;
    }

    private ServerBootstrap bootstrap;
    private EventLoopGroup loopGroup;
    private Channel serverChannel;
    private MessageCollector collector;

    public void start() {
        bootstrap = new ServerBootstrap();
        loopGroup = new NioEventLoopGroup(ioThreads);
        bootstrap.group(loopGroup);
        collector = new MessageCollector(workerThreads, dispatcher);
        bootstrap.channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                var pipe = socketChannel.pipeline();
                pipe.addLast(new ReadTimeoutHandler(10));
                pipe.addLast(new HttpServerCodec());
                pipe.addLast(new HttpObjectAggregator(1<<30));
                pipe.addLast(new ChunkedWriteHandler());
                pipe.addLast(collector);
            }
        });



    }
}
