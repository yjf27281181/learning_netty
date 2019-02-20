package server;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import lombok.Getter;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KidsContext {
    private ChannelHandlerContext ctx;
    private ITemplateEngine templateEngine;
    @Getter
    private String contextRoot;

    private Map<String,Cookie> cookies = new HashMap();

    public KidsContext(ChannelHandlerContext ctx, String contextRoot,
                       ITemplateEngine templateEngine) {
        this.ctx = ctx;
        this.templateEngine = templateEngine;
        this.contextRoot = contextRoot;
    }
    public void send(Object... response) {
        for(Object o: response) {
            ctx.write(o);
        }
        ctx.flush();
        cookies.clear();
    }
    public KidsContext addCookie(String name, String value) {
        return this.addCookie(name, value, null, this.contextRoot, -1, false, false);
    }
    public KidsContext addCookie(String name, String value, String domain, String path, long maxAge, boolean httpOnly, boolean isSecure) {

        if(cookies.containsKey(name)) return this;
        Cookie cookie = new DefaultCookie(name, value);
        if(domain != null) cookie.setDomain(domain);
        if(path != null) cookie.setPath(path);
        if(maxAge>=0) cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(isSecure);
        cookies.put(name, cookie);
        return this;
    }

    public ByteBufAllocator alloc() {
        return ctx.alloc();
    }

    public void redirect(String location) {
        redirect(location, true);
    }

    public void redirect(String location, boolean withinContext) {
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        if(location.startsWith("/")) {
            location = withinContext? Paths.get(contextRoot, location).toString(): location;
        }
        res.headers().add(HttpHeaderNames.LOCATION, location);
        for(Map.Entry<String, Cookie> entry: cookies.entrySet()) {
            Cookie cookie = entry.getValue();
            res.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
        }
        cookies.clear();
        ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
    }

    public void text(String context, String contentType, int statusCode, boolean isError) {
        var buf = ByteBufAllocator.DEFAULT.buffer();
        var bytes = context.getBytes(KidsUtils.UTF8);
        buf.writeBytes(bytes);
        DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(statusCode), buf);
        res.headers().add(HttpHeaderNames.CONTENT_TYPE, String.format("%s; charset=utf-8", contentType));
        res.headers().add(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        for(Map.Entry<String, Cookie> entry: cookies.entrySet()) {
            Cookie cookie = entry.getValue();
            res.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
        }
        if(!isError)
            ctx.writeAndFlush(res);
        else
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        cookies.clear();
    }
    public void text(String context, String contentType, int statusCode) {
        text(context, contentType, statusCode, false);
    }
    public void error(String msg, String contentType, int statusCode) {
        text(msg, contentType, statusCode, true);
    }

    public void abort(int code, String content) throws AbortException {
        throw new AbortException(HttpResponseStatus.valueOf(code), content);
    }

    public void html(String html) {
        html(html, 200);
    }

    public void html(String html, int statusCode) {
        text(html, "text/html", statusCode);
    }

    public void error(int statusCode) {
        error(HttpResponseStatus.valueOf(statusCode).reasonPhrase(), statusCode);
    }

    public void error(String msg, int statusCode) {
        error(msg, "text/plain", statusCode);
    }


    public void render(String path) throws AbortException {
        render(path, 200);
    }

    public void render(String path, Map<String, Object> context) throws AbortException {
        render(path, context, 200);
    }

    public void render(String path, int statusCode) throws AbortException {
        render(path, Collections.emptyMap(), statusCode);
    }

    public void render(String path, Map<String, Object> context, int statusCode) throws AbortException {
        html(templateEngine.render(path, context), statusCode);
    }

    public void json(Object o, int statusCode) {
        text(JSON.toJSONString(o), "application/json", statusCode);
    }

    public void json(Object o) {
        json(o, 200);
    }

    public void close() {
        this.ctx.close();
    }


}
