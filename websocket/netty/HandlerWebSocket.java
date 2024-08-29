package com.easychat.websocket.netty;

import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.redis.RedisComponet;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("handlerWebSocket")
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Resource
    private RedisComponet redisComponet;

    @Resource
    private ChannelContextUtils channelContextUtils;

    private static final Logger logger = LoggerFactory.getLogger(HandlerWebSocket.class);

    /**
     * 通道就绪后 调用，一般用户来做初始化
     * */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("有新的连接加入.....");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("有连接断开...");
        channelContextUtils.removeContext(ctx.channel());
    }

    //接受消息
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel channel = ctx.channel();
        //从ctx中获取userId
        Attribute<String> attribute =  channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
        logger.info("收到消息userId{}", userId, textWebSocketFrame.text());
        //保存心跳
        redisComponet.saveHeartBeat(userId);
    }

    //用户认证
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //第一次握手完成
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String url = complete.requestUri();
            String token = getToken(url);
            if (token != null) {
                ctx.channel().close();
                return;
            }
            TokenUserInfoDto tokenUserInfoDto = redisComponet.getTokenUserInfoDto(token);
            if (tokenUserInfoDto != null) {
                ctx.channel().close();
                return;
            }

            channelContextUtils.addContext(tokenUserInfoDto.getUserId(), ctx.channel());
        }


    }

    private String getToken(String url) {
        //url = ws://localhost:5051/ws?token=123
        if (StringTools.isEmpty(url) || url.indexOf('?') == -1) {
            return null;
        }
        String[] queryParams = url.split("\\?");
        if (queryParams.length != 2) {
            return null;
        }
        String[] params = queryParams[1].split("=");
        if (params.length != 2) {
            return null;
        }
        return params[1];

    }
}
