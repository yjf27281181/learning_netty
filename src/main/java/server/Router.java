package server;

import io.netty.handler.codec.http.HttpResponseStatus;
import server.internal.IRequestFilter;

import java.util.*;

public class Router {
    private IRequestHandler wirecardHandler;

    private Map<String, IRequestHandler> subHandlers = new HashMap<>();
    private Map<String, Map<String, IRequestHandler>> subMethodHandlers = new HashMap<>();

    private Map<String, Router> subRouters = new HashMap<>();

    private List<IRequestFilter> filters = new ArrayList<>();

    private final static List<String> METHODS = Arrays
            .asList(new String[] { "get", "post", "head", "put", "delete", "trace", "options", "patch", "connect" });

    public Router() {
        this(null);
    }
    public Router(IRequestHandler wirecardHandler) {
        this.wirecardHandler = wirecardHandler;
    }

    public Router handler(String path, String method, IRequestHandler handler) throws IllegalAccessException {
        path = KidsUtils.normalize(path);
        method = method.toLowerCase();
        if(path.indexOf('/') != path.lastIndexOf('/')) {
            throw new IllegalAccessException("path at most one slash allowed");
        }
        if(!METHODS.contains(method)) {
            throw new IllegalArgumentException("illegal http method name");
        }
        var handlers = subMethodHandlers.get(path);
        if(handlers == null) {
            handlers = new HashMap<>();
            subMethodHandlers.put(path, handlers);
        }
        handlers.put(method, handler);
        return this;
    }

    public Router child(String path, Router router) {
        path = KidsUtils.normalize(path);
        if (path.equals("/")) {
            throw new IllegalArgumentException("child path should not be /");
        }
        if (path.indexOf('/') != path.lastIndexOf('/')) {
            throw new IllegalArgumentException("path at most one slash allowed");
        }
        this.subRouters.put(path, router);
        return this;
    }

    public Router child(String path, IRouteable router) {
        return child(path, router.route());
    }

    public Router resource(String path, String resourceRoot) {
        return null; //TODO
    }

    public Router filter(IRequestFilter... filters) {
        for(var filter: filters) {
            this.filters.add(filter);
        }
        return this;
    }

    public void handle(KidsContext ctx, KidsRequest req) throws AbortException {
        for(var filter: this.filters) {
            req.filter(filter);
        }
        var prefix = req.peekUriPrefix();
        var method = req.method().toLowerCase();
        var router = subRouters.get(prefix);
        if(router != null) {
            req.popUriPrefix();
            router.handle(ctx, req);
            return;
        }

        if(prefix.equals(req.relativeUri())) {
            var handlers = subMethodHandlers.get(prefix);
            IRequestHandler handler = null;
            if(handler == null) {
                handler = subHandlers.get(prefix);
            }
            if(handler != null) {
                handleImpl(handler, ctx, req);
                return;
            }
        }

        if(this.wirecardHandler != null) {
            handleImpl(wirecardHandler, ctx, req);
            return;
        }
        throw new AbortException(HttpResponseStatus.NOT_FOUND);
    }

    private void handleImpl(IRequestHandler handler, KidsContext ctx, KidsRequest req) {
        for(var filter: req.filters()) {
            if(!filter.filter(ctx, req, true)) {
                return;
            }
        }

        for(var filter: req.filters()) {
            if(!filter.filter(ctx, req, false))
                return;
        }
    }
}
