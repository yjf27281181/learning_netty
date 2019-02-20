package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.internal.IRequestDispatcher;

import java.util.HashMap;
import java.util.Map;

public class KidsRequestDispatcher implements IRequestDispatcher {
    private final static Logger LOG = LoggerFactory.getLogger(KidsRequestDispatcher.class);

    private String contextRoot;
    private Router router;
    private Map<Integer, IExceptionHandler> exceptionHandlers = new HashMap();
    private IExceptionHandler defaultExceptionHandler = new DefaultExceptionHandler();
    private ITemplateEngine templateEngine = new ITemplateEngine() {
    };

    static class DefaultExceptionHandler implements IExceptionHandler {
        @Override
        public void handle(KidsContext ctx, AbortException e) {
            if(e.getStatus().code() == 500) {
                LOG.error("Internal Server Error", e);
            }
            ctx.error(e.getContent(), e.getStatus().code());
        }
    }

    public KidsRequestDispatcher(Router router) {
        this("/", router);
    }

    public KidsRequestDispatcher(String contextRoot, Router router) {
        this.contextRoot = KidsUtils.normalize(contextRoot);
        this.router = router;
    }

    public KidsRequestDispatcher template(String contextRoot, Router router) {
        this.contextRoot = KidsUtils.normalize(contextRoot);
        return this;
    }
    public String root() {
        return contextRoot;
    }

    public KidsRequestDispatcher exception(int code, IExceptionHandler handler) {
        this.exceptionHandlers.put(code, handler);
        return this;
    }

    @Override
    public void dispatch(ChannelHandlerContext channelCtx, FullHttpRequest req) {
        var ctx = new KidsContext(channelCtx, contextRoot, templateEngine);
        try {
            this.handleImpl(ctx, new KidsRequest(req));
        } catch (Exception e) {
            this.handleException(ctx, new AbortException(HttpResponseStatus.INTERNAL_SERVER_ERROR, e));
        } catch (AbortException e) {
            this.handleException(ctx, e);
        } finally {
            req.release();
        }

    }

    private void handleException(KidsContext ctx, AbortException e) {
        var handler = this.exceptionHandlers.getOrDefault(e.getStatus().code(), defaultExceptionHandler);
        try {
            handler.handle(ctx, e);
        } catch (Exception ex) {
            this.defaultExceptionHandler.handle(ctx, new AbortException(HttpResponseStatus.INTERNAL_SERVER_ERROR, ex));
        }
    }

    private void handleImpl(KidsContext ctx, KidsRequest req) throws Exception, AbortException {
        if(req.decoderResult().isFailure()) {
            ctx.abort(400, "http protocol decode failed");
        }

        if(req.relativeUri().contains("./") || req.relativeUri().contains(".\\")) {
            ctx.abort(400, "insecure url");
        }
        if(!req.relativeUri().startsWith(contextRoot)) {
            throw new AbortException(HttpResponseStatus.NOT_FOUND);
        }
        req.popRootUri(contextRoot);
        router.handle(ctx, req);
    }
}
