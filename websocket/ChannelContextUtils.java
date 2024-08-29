package com.easychat.websocket;

import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.WsInitData;
import com.easychat.entity.enums.MessageTypeEnum;
import com.easychat.entity.enums.UserContactApplyStatusEnum;
import com.easychat.entity.enums.UserContactStatusEnum;
import com.easychat.entity.enums.UserContactTypeEnum;
import com.easychat.entity.po.*;
import com.easychat.entity.query.ChatMessageQuery;
import com.easychat.entity.query.ChatSessionUserQuery;
import com.easychat.entity.query.UserContactApplyQuery;
import com.easychat.entity.query.UserInfoQuery;
import com.easychat.mappers.ChatMessageMapper;
import com.easychat.mappers.ChatSessionUserMapper;
import com.easychat.mappers.UserContactApplyMapper;
import com.easychat.mappers.UserInfoMapper;
import com.easychat.redis.RedisComponet;
import com.easychat.service.ChatSessionUserService;
import com.easychat.utils.JsonUtils;
import com.easychat.utils.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sun.util.calendar.BaseCalendar;

import javax.annotation.Resource;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChannelContextUtils {

    private static final Logger logger = LoggerFactory.getLogger(ChannelContextUtils.class);

    private static final ConcurrentHashMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ChannelGroup> GROUP_CONTEXT_MAP = new ConcurrentHashMap<>();

    @Resource
    private RedisComponet redisComponet;

    @Resource
    private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

    @Resource
    private UserContactApplyMapper<UserContactApply,UserContactApplyQuery> userContactApplyMapper;

    public void addContext(String userId, Channel channel){
        //用户和通道进行对应
        String channelId = channel.id().toString();
        logger.info("channelId:{}", channelId);
        AttributeKey attributeKey = null;
        if(!AttributeKey.exists(channelId)){
            //创建一个attributeKey
            attributeKey = AttributeKey.newInstance(channelId);
        }else {
            attributeKey = AttributeKey.valueOf(channelId);
        }
        //在通道中设置userId
        channel.attr(attributeKey).set(userId);

        List<String> contactIdList = redisComponet.getUserContactList(userId);
        for(String groupId : contactIdList){
            //查看是否时好友或者群组
            if(groupId.startsWith(UserContactTypeEnum.GROUP.getPrefix())){
                add2Group(groupId, channel);
            }
        }
        //连接后更新用户Id
        USER_CONTEXT_MAP.put(userId, channel);
        redisComponet.saveHeartBeat(userId);

        //更新用户最后连接时间
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastLoginTime(new Date());
        userInfoMapper.updateByUserId(updateInfo, userId);

        //给用户发送消息
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        //最后离线时间
        Long sourceLastoffTime = userInfo.getLastOffTime();
        //如果是在三天内取sourceLastoffTime
        Long lastOffTime = sourceLastoffTime;
        //超过三天取三天
        if(sourceLastoffTime != null && System.currentTimeMillis() - Constants.MILLISECOND_3DAYS_AGO > sourceLastoffTime){
            lastOffTime = Constants.MILLISECOND_3DAYS_AGO;
        }
        /**
         * 1.查询会话信息，查询用户所有的会话信息，保证了设备会话同步
         * */
        ChatSessionUserQuery sessionUserQuery = new ChatSessionUserQuery();
        sessionUserQuery.setUserId(userId);
        sessionUserQuery.setOrderBy("last_receive_time desc");
        List<ChatSessionUser> chatSessionUserlist = chatSessionUserMapper.selectList(sessionUserQuery);

        WsInitData wsInitData = new WsInitData();
        wsInitData.setChatSessionList(chatSessionUserlist);

        /**
         * 2.查询聊天消息
         * */
        //查询所有的联系人，从redis中找到所有的联系人
        //筛选所有的群组
        List<String> groupIdList = contactIdList.stream().filter(item->item.startsWith(UserContactTypeEnum.GROUP.getPrefix())).collect(Collectors.toList());
        groupIdList.add(userId);

        ChatMessageQuery messageQuery = new ChatMessageQuery();
        messageQuery.setContactIdList(contactIdList);
        messageQuery.setLastReceiveTime(lastOffTime);
        List<ChatMessage> chatMessageList = chatMessageMapper.selectList(messageQuery);

        wsInitData.setChatMessageList(new ArrayList<>());
        /**
         * 3.查询好友申请
         * */
        UserContactApplyQuery applyQuery = new UserContactApplyQuery();
        applyQuery.setReceiveUserId(userId);
        applyQuery.setLastApplyTime(lastOffTime);
        applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
        Integer applyCount = userContactApplyMapper.selectCount(applyQuery);
        wsInitData.setApplyCount(applyCount);

        //发送消息
        MessageSendDto messageSendDto = new MessageSendDto();
        messageSendDto.setMessageType(MessageTypeEnum.INIT.getType());
        messageSendDto.setContactId(userId);
        //保存数据
        messageSendDto.setExtendData(wsInitData);

        senMsg(messageSendDto, userId);


    }

    private void add2Group(String groupId, Channel channel){
        //内存中获取group
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        if(group == null){
            //创建一个group
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            GROUP_CONTEXT_MAP.put(groupId, group);
        }
        if(channel==null){
            return;
        }
        group.add(channel);
    }

    public void removeContext(Channel channel){
        //从ctx中获取userId
        Attribute<String> attribute =  channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
        if(StringTools.isEmpty(userId)){
            USER_CONTEXT_MAP.remove(userId);
        }
        redisComponet.removeUserHeartBeat(userId);
        //更新用户最后离线时间
        UserInfo updateInfo = new UserInfo();
        updateInfo.setLastOffTime(System.currentTimeMillis());
        userInfoMapper.updateByUserId(updateInfo, userId);
    }

    public void sendMessage(MessageSendDto messageSendDto){
        //读取最前面的一个字母，查看是好友还是群聊
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(messageSendDto.getContactId());
        switch(contactTypeEnum){
            case USER:
                send2User(messageSendDto);
                break;
            case GROUP:
                send2Group(messageSendDto);
                break;
        }
    }

    //单聊发送给用户
    private void send2User(MessageSendDto messageSendDto){
        String contactId = messageSendDto.getContactId();
        if(StringTools.isEmpty(contactId)){
            return;
        }
        senMsg(messageSendDto, contactId);
        //强制下线
        if(MessageTypeEnum.FORCE_OFF_LINE.getType().equals(messageSendDto.getMessageType())){
            closeContext(contactId);
        }
    }

    //关闭通道
    public void closeContext(String userId){
        if(StringTools.isEmpty(userId)){
            return;
        }
        redisComponet.cleanUserTokenByUserId(userId);
        Channel channel = USER_CONTEXT_MAP.get(userId);
        if(channel==null){
            return;
        }
        channel.close();
    }


    //好友间发送消息
    public void senMsg(MessageSendDto messageSendDto, String receiveId){
        Channel userChannel = USER_CONTEXT_MAP.get(receiveId);
        if(userChannel == null){
            return;
        }

        if (MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(messageSendDto.getMessageType())){

            UserInfo userInfo = (UserInfo) messageSendDto.getExtendData();
            messageSendDto.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            messageSendDto.setContactId(userInfo.getUserId());
            messageSendDto.setContactName(userInfo.getNickName());
            messageSendDto.setExtendData(null);
        }else {
            //相对于客户端而言，联系人就是发送人，这里转换一下再发送
            messageSendDto.setContactId(messageSendDto.getSendUserId());
            messageSendDto.setContactName(messageSendDto.getSendUserNickName());
        }
        //给接收人，发送文本，把文本转换为JSON进行发送
        userChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDto)));


    }

    private void send2Group(MessageSendDto messageSendDto){
        if (StringTools.isEmpty(messageSendDto.getContactId())){
            return;
        }
        //从这个查找通道
        ChannelGroup channelGroup = GROUP_CONTEXT_MAP.get(messageSendDto.getContactId());
        if(channelGroup == null){
            return;
        }
        //开始写入
        channelGroup.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageSendDto)));

        //移除群聊
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(messageSendDto.getMessageType());
        if(MessageTypeEnum.LEAVE_GROUP==messageTypeEnum||MessageTypeEnum.REMOVE_GROUP==messageTypeEnum){
            String userId = (String) messageSendDto.getExtendData();
            redisComponet.removeUserContact(userId,messageSendDto.getContactId());
            //关闭通道
            Channel channel = USER_CONTEXT_MAP.get(userId);
            if(channel==null){
                return;
            }
            channelGroup.remove(channel);
        }
        if(MessageTypeEnum.DISSOLUTION_GROUP==messageTypeEnum){
            GROUP_CONTEXT_MAP.remove(messageSendDto.getContactId());
            channelGroup.close();
        }
    }

    public void addUser2Group(String userId, String groupId) {
        Channel channel = USER_CONTEXT_MAP.get(userId);
        add2Group(groupId, channel);
    }

}
