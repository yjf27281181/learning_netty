package server;

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
}
