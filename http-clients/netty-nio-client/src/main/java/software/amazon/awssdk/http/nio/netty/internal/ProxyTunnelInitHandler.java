/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Handler that initializes the HTTP tunnel.
 */
@SdkInternalApi
public final class ProxyTunnelInitHandler extends ChannelDuplexHandler {
    
    public static final NettyClientLogger log = NettyClientLogger.getLogger(ProxyTunnelInitHandler.class);
    private final ChannelPool sourcePool;
    private final String username;
    private final String password;
    private final URI remoteHost;
    private final Promise<Channel> initPromise;
    private final Supplier<HttpClientCodec> httpCodecSupplier;

    public ProxyTunnelInitHandler(ChannelPool sourcePool, String proxyUsername, String proxyPassword, URI remoteHost,
                                  Promise<Channel> initPromise) {
        this(sourcePool, proxyUsername, proxyPassword, remoteHost, initPromise, HttpClientCodec::new);
    }

    public ProxyTunnelInitHandler(ChannelPool sourcePool, URI remoteHost, Promise<Channel> initPromise) {
        this(sourcePool, null, null, remoteHost, initPromise, HttpClientCodec::new);
    }

    @SdkTestInternalApi
    public ProxyTunnelInitHandler(ChannelPool sourcePool, String prosyUsername, String proxyPassword,
                                  URI remoteHost, Promise<Channel> initPromise, Supplier<HttpClientCodec> httpCodecSupplier) {
        this.sourcePool = sourcePool;
        this.remoteHost = remoteHost;
        this.initPromise = initPromise;
        this.username = prosyUsername;
        this.password = proxyPassword;
        this.httpCodecSupplier = httpCodecSupplier;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addBefore(ctx.name(), null, httpCodecSupplier.get());
        HttpRequest connectRequest = connectRequest();
        ctx.channel().writeAndFlush(connectRequest).addListener(f -> {
            if (!f.isSuccess()) {
                handleConnectRequestFailure(ctx, f.cause());
            }
        });
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (ctx.pipeline().get(HttpClientCodec.class) != null) {
            ctx.pipeline().remove(HttpClientCodec.class);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            if (response.status().code() == 200) {
                ctx.pipeline().remove(this);
                // Note: we leave the SslHandler here (if we added it)
                initPromise.setSuccess(ctx.channel());
                return;
            }
        }

        // Fail if we received any other type of message or we didn't get a 200 from the proxy
        ctx.pipeline().remove(this);
        ctx.close();
        sourcePool.release(ctx.channel());
        initPromise.setFailure(new IOException("Could not connect to proxy"));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (!initPromise.isDone()) {
            handleConnectRequestFailure(ctx, null);
        } else {
            log.debug(ctx.channel(), () -> "The proxy channel (" + ctx.channel().id() + ") is inactive");
            closeAndRelease(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!initPromise.isDone()) {
            handleConnectRequestFailure(ctx, cause);
        } else {
            log.debug(ctx.channel(), () -> "An exception occurred on the proxy tunnel channel (" + ctx.channel().id() + "). " +
                                           "The channel has been closed to prevent any ongoing issues.", cause);
            closeAndRelease(ctx);
        }
    }

    private void handleConnectRequestFailure(ChannelHandlerContext ctx, Throwable cause) {
        closeAndRelease(ctx);
        String errorMsg = "Unable to send CONNECT request to proxy";
        IOException ioException = cause == null ? new IOException(errorMsg) :
                                  new IOException(errorMsg, cause);
        initPromise.setFailure(ioException);
    }

    private void closeAndRelease(ChannelHandlerContext ctx) {
        ctx.close();
        sourcePool.release(ctx.channel());
    }

    private HttpRequest connectRequest() {
        String uri = getUri();
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, uri,
                                                         Unpooled.EMPTY_BUFFER);
        request.headers().add(HttpHeaderNames.HOST, uri);

        if (!StringUtils.isEmpty(this.username) && !StringUtils.isEmpty(this.password)) {
            String authToken = String.format("%s:%s", this.username, this.password);
            String authB64 = Base64.getEncoder().encodeToString(authToken.getBytes(CharsetUtil.UTF_8));
            request.headers().add(HttpHeaderNames.PROXY_AUTHORIZATION, String.format("Basic %s", authB64));
        }
        
        return request;
    }

    private String getUri() {
        return remoteHost.getHost() + ":" + remoteHost.getPort();
    }
}

