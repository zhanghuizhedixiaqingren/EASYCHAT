package com.easychat.websocket;

import com.easychat.entity.dto.MessageSendDto;
import com.easychat.utils.JsonUtils;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

//消息处理器
@Component("messageHandler")
public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String MESSAGE_TOPIC = "message.topic";

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ChannelContextUtils channelContextUtils;

    //启动服务，启动监听
    @PostConstruct
    public void lisMessage(){
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        //监听
        rTopic.addListener(MessageSendDto.class, (MessageSendDto, sendDto)->{
            logger.info("收到广播消息：{}", JsonUtils.convertObj2Json(sendDto));
            channelContextUtils.sendMessage(sendDto);

        });
    }

    public void sendMessage(MessageSendDto sendDto) {
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        //所有服务器都可以收到消息
        rTopic.publish(sendDto);
    }
}
