package server;

import java.util.Map;

import io.netty.handler.codec.http.HttpResponseStatus;

public interface ITemplateEngine {

    default String render(String path, Map<String, Object> context) throws AbortException {
        throw new AbortException(HttpResponseStatus.INTERNAL_SERVER_ERROR, "template root not provided");
    }

}