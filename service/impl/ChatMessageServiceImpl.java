package com.easychat.service.impl;

import com.easychat.entity.config.AppConfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.SysSettingDto;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.ChatSession;
import com.easychat.entity.po.ChatSessionUser;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.ChatMessage;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.ChatMessageMapper;
import com.easychat.mappers.ChatSessionMapper;
import com.easychat.mappers.ChatSessionUserMapper;
import com.easychat.mappers.UserContactMapper;
import com.easychat.redis.RedisComponet;
import com.easychat.service.ChatMessageService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.DateUtils;
import com.easychat.utils.StringTools;
import com.easychat.websocket.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
  * @Desoription:聊天消息表Service
  * 
  * @author:enjoying
  * @data:2024/08/14
  */
@Service("chatMessageService")
public class ChatMessageServiceImpl implements ChatMessageService{

	private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

	@Resource
	private ChatMessageMapper<ChatMessage,ChatMessageQuery>chatMessageMapper;
    @Resource
    private RedisComponet redisComponet;

	@Resource
    private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

	@Resource
    private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

	@Resource
    private MessageHandler messageHandler;

	@Resource
    private AppConfig appConfig;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	/**
	  * 根据条件查询列表
	  */
	@Override
	public List<ChatMessage> findListByParam(ChatMessageQuery query) {
		return this.chatMessageMapper.selectList(query);
	}

	/**
	  * 根据条件查询数量
	  */
	@Override
	public Integer findCountByParam(ChatMessageQuery query) {
		return this.chatMessageMapper.selectCount(query);
	}

	/**
	  * 分页查询
	  */
	@Override
	public PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null?PageSize.SIZE15.getSize():query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<ChatMessage> list = this.findListByParam(query);
		PaginationResultVO<ChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	  * 新增
	  */
	@Override
	public Integer add(ChatMessage bean) {
		return this.chatMessageMapper.insert(bean);
	}

	/**
	  * 批量新增
	  */
	@Override
	public Integer addBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertBatch(listBean);
	}

	/**
	  * 批量新增或修改
	  */
	@Override
	public Integer addOrUpdateBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	  * 根据MessageId查询
	  */
	@Override
	public ChatMessage getChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.selectByMessageId(messageId);
	}

	/**
	  * 根据MessageId更新
	  */
	@Override
	public Integer updateChatMessageByMessageId( ChatMessage bean, Long messageId) {
		return this.chatMessageMapper.updateByMessageId(bean, messageId);
	}

	/**
	  * 根据MessageId删除
	  */
	@Override
	public Integer deleteChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.deleteByMessageId(messageId);
	}

	@Override
	public MessageSendDto saveMessage(ChatMessage chatMessage, TokenUserInfoDto tokenUserInfoDto) throws BusinessException {
		//不是机器人回复，判断好友状态
		if(!Constants.ROBOT_UID.equals(tokenUserInfoDto.getUserId())){
			//从redis获取好友集合
			List<String> contactList = redisComponet.getUserContactList(tokenUserInfoDto.getUserId());
			//如果不在联系人列表中
			if(!contactList.contains(chatMessage.getContactId())){
				UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(chatMessage.getContactId());
				if(UserContactTypeEnum.USER == userContactTypeEnum){
					throw new BusinessException(ResponseCodeEnum.CODE_902);
				}else {
					throw new BusinessException(ResponseCodeEnum.CODE_903);
				}
			}
		}
		String sessionId = null;

		String sendUserId = tokenUserInfoDto.getUserId();
		String  contactId = chatMessage.getContactId();
		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);

		if(UserContactTypeEnum.USER==contactTypeEnum){
			sessionId = StringTools.getChatSessionId4User(new String[]{sendUserId, contactId});
		}else {
			sessionId =StringTools.getChatSessionId4Group(contactId);
		}
		chatMessage.setSessionId(sessionId);

		//发送时间
		Long curTime = System.currentTimeMillis();
		chatMessage.setSendTime(curTime);

		//处理状态
		MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByType(chatMessage.getMessageType());
		if(null == messageTypeEnum || !ArrayUtils.contains(new Integer[]{MessageTypeEnum.CHAT.getType(), MessageTypeEnum.MEDIA_CHAT.getType()}, chatMessage.getMessageType())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//如果是媒体文件，状态设置为发送中，否则状态设置为发送完成
		Integer status = MessageTypeEnum.MEDIA_CHAT == messageTypeEnum ? MessageStatusEnum.SENDING.getStatus() : MessageStatusEnum.SENDED.getStatus();
		chatMessage.setStatus(status);

		//防止注入，处理短信内容
		String messageContent = StringTools.cleanHtmlTag(chatMessage.getMessageContent());
		chatMessage.setMessageContent(messageContent);

		//更新会话，chatSession表
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		//如果是群聊类型
		if(UserContactTypeEnum.GROUP==contactTypeEnum){
			//用户+说的话
			chatSession.setLastMessage(tokenUserInfoDto.getNickName() + ":" + messageContent);
		}
		//更新会话信息
		chatSession.setLastReceiveTime(curTime);
		chatSessionMapper.updateBySessionId(chatSession, sessionId);

		//记录消息表
		chatMessage.setSendUserId(sendUserId);
		chatMessage.setSendUserNickName(tokenUserInfoDto.getNickName());
		chatMessage.setContactType(contactTypeEnum.getType());
		chatMessageMapper.insert(chatMessage);
		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);

		//如果和机器人聊天
		if(Constants.ROBOT_UID.equals(contactId)){
			SysSettingDto sysSettingDto =  redisComponet.getsysSettingDto();
			TokenUserInfoDto robot = new TokenUserInfoDto();
			robot.setUserId(sysSettingDto.getRobotUid());
			robot.setNickName(sysSettingDto.getRobotNickName());
			ChatMessage robotChatMessage = new ChatMessage();
			robotChatMessage.setContactId(sendUserId);
			//可以调用ai接口
			robotChatMessage.setMessageContent("我只是一个机器人");
			robotChatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
			saveMessage(robotChatMessage,robot);
		}else {
			messageHandler.sendMessage(messageSendDto);
		}
		return messageSendDto;
	}

	@Override
	public void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover) throws BusinessException {
		ChatMessage chatMessage = chatMessageMapper.selectByMessageId(messageId);
		if(chatMessage==null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(!chatMessage.getSendUserId().equals(userId)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		SysSettingDto sysSettingDto = redisComponet.getsysSettingDto();
		String fileSuffix = StringTools.getFileSuffix(file.getOriginalFilename());
		if(!StringTools.isEmpty(fileSuffix)
				&&ArrayUtils.contains(Constants.IMAGE_SUFFIX_LIST,fileSuffix.toUpperCase())
				&&file.getSize()>sysSettingDto.getMaxImageSize()*Constants.FILE_SIZE_MB
		){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}else if(!StringTools.isEmpty(fileSuffix)
				&&ArrayUtils.contains(Constants.VIDEO_SUFFIX_LIST,fileSuffix.toUpperCase())
				&&file.getSize()>sysSettingDto.getMaxVideoSize()*Constants.FILE_SIZE_MB){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}else if (!StringTools.isEmpty(fileSuffix)
				&&ArrayUtils.contains(Constants.IMAGE_SUFFIX_LIST,fileSuffix.toUpperCase())
				&&ArrayUtils.contains(Constants.VIDEO_SUFFIX_LIST,fileSuffix.toUpperCase())
				&&file.getSize()>sysSettingDto.getMaxFileSize()*Constants.FILE_SIZE_MB){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}

		String fileName = file.getOriginalFilename();
		String fileExtName = StringTools.getFileSuffix(fileName);
		String fileRealName = messageId + fileExtName;
		String month = DateUtils.format(new Date(chatMessage.getSendTime()),DateTimePatternEnum.YYYYMM.getPattern());
		File folder = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + month);
		if(!folder.exists()){
			folder.mkdirs();
		}
		File updateFile = new File(folder.getPath() + "/" + fileRealName);
        try {
            file.transferTo(updateFile);
			cover.transferTo(new File(updateFile.getPath() + Constants.COVER_IMAGE_SUFFIX));
        } catch (IOException e) {
            logger.error("上传文件失败");
			throw new BusinessException("文件上传失败");
        }
		ChatMessage uploadInfo = new ChatMessage();
		uploadInfo.setStatus(MessageStatusEnum.SENDED.getStatus());
		ChatMessageQuery messageQuery = new ChatMessageQuery();
		messageQuery.setMessageId(messageId);
		chatMessageMapper.updateByParam(uploadInfo, messageQuery);

		MessageSendDto messageSendDto = new MessageSendDto();
		messageSendDto.setStatus(MessageStatusEnum.SENDED.getStatus());
		messageSendDto.setMessageId(messageId);
		messageSendDto.setMessageType(MessageTypeEnum.FILE_UPLOAD.getType());
		messageSendDto.setContactId(chatMessage.getContactId());
		messageHandler.sendMessage(messageSendDto);

    }

	@Override
	public File downloadFile(TokenUserInfoDto userInfoDto, Long messageId, Boolean showCover) throws BusinessException {
		ChatMessage message = chatMessageMapper.selectByMessageId(messageId);
		String contactId = message.getContactId();
		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if(UserContactTypeEnum.USER==contactTypeEnum && !userInfoDto.getUserId().equals(message.getContactId())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(UserContactTypeEnum.GROUP==contactTypeEnum){
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setUserId(userInfoDto.getUserId());
			userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			Integer contactCount = userContactMapper.selectCount(userContactQuery);
			if(contactCount==0){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}

		String month = DateUtils.format(new Date(message.getSendTime()), DateTimePatternEnum.YYYYMM.getPattern());
		File folder = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + month);
		if(!folder.exists()){
			folder.mkdirs();
		}
		String fileName = message.getFileName();
		String fileExtName = StringTools.getFileSuffix(fileName);
		String fileRealName = messageId + fileExtName;
		if(showCover!=null && showCover){
			fileRealName = fileRealName + Constants.COVER_IMAGE_SUFFIX;
		}
		File file = new File(folder.getPath() + "/" + fileRealName);
		if(!file.exists()){
			logger.error("文件不存在{}", messageId);
			throw new BusinessException(ResponseCodeEnum.CODE_602);
		}
		return file;
	}
}