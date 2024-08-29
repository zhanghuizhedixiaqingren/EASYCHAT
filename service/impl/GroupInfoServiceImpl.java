package com.easychat.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import com.easychat.entity.config.AppConfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.SysSettingDto;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.*;
import com.easychat.entity.query.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.*;
import com.easychat.redis.RedisComponet;
import com.easychat.service.ChatSessionUserService;
import com.easychat.service.GroupInfoService;
import com.easychat.service.UserContactApplyService;
import com.easychat.service.UserContactService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import com.easychat.websocket.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
  * @Desoription:Service
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
@Service("groupInfoService")
public class GroupInfoServiceImpl implements GroupInfoService{

	@Resource
	private GroupInfoMapper<GroupInfo,GroupInfoQuery>groupInfoMapper;

	@Resource
	private RedisComponet redisComponet;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	@Resource
	private AppConfig appConfig;

	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	@Autowired
	private ChatMessageMapper chatMessageMapper;

	@Autowired
	private MessageHandler messageHandler;

	@Resource
	private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

	@Resource
	private ChannelContextUtils channelContextUtils;

	@Resource
	private ChatSessionUserService chatSessionUserService;

	@Resource
	@Lazy
	private GroupInfoService groupInfoService;

	@Resource
	private UserContactApplyService userContactApplyService;


	/**
	  * 根据条件查询列表
	  */
	@Override
	public List<GroupInfo> findListByParam(GroupInfoQuery query) {
		return this.groupInfoMapper.selectList(query);
	}

	/**
	  * 根据条件查询数量
	  */
	@Override
	public Integer findCountByParam(GroupInfoQuery query) {
		return this.groupInfoMapper.selectCount(query);
	}

	/**
	  * 分页查询
	  */
	@Override
	public PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null?PageSize.SIZE15.getSize():query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<GroupInfo> list = this.findListByParam(query);
		PaginationResultVO<GroupInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	  * 新增
	  */
	@Override
	public Integer add(GroupInfo bean) {
		return this.groupInfoMapper.insert(bean);
	}

	/**
	  * 批量新增
	  */
	@Override
	public Integer addBatch(List<GroupInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertBatch(listBean);
	}

	/**
	  * 批量新增或修改
	  */
	@Override
	public Integer addOrUpdateBatch(List<GroupInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.groupInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	  * 根据GroupId查询
	  */
	@Override
	public GroupInfo getGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.selectByGroupId(groupId);
	}

	/**
	  * 根据GroupId更新
	  */
	@Override
	public Integer updateGroupInfoByGroupId( GroupInfo bean, String groupId) {
		return this.groupInfoMapper.updateByGroupId(bean, groupId);
	}

	/**
	  * 根据GroupId删除
	  */
	@Override
	public Integer deleteGroupInfoByGroupId(String groupId) {
		return this.groupInfoMapper.deleteByGroupId(groupId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveGroup(GroupInfo groupInfo, MultipartFile avararFile, MultipartFile avatarCover) throws BusinessException, IOException {
		if(StringTools.isEmpty(groupInfo.getGroupId())){
			Date curDate = new Date();
			GroupInfoQuery groupQuery = new GroupInfoQuery();
			groupQuery.setGroupOwnerId(groupQuery.getGroupOwnerId());
			Integer count = this.groupInfoMapper.selectCount(groupQuery);

			SysSettingDto sysSettingDto = redisComponet.getsysSettingDto();
			if(count >= sysSettingDto.getMaxGroupCount()){
                try {
                    throw new BusinessException("最多支持能创建" + sysSettingDto.getMaxGroupCount() + "群聊");
                } catch (BusinessException e) {
                    throw new RuntimeException(e);
                }
            }
			if(null == avararFile){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}

			groupInfo.setCreateTime(curDate);
			groupInfo.setGroupId(StringTools.getGroupId());
			this.groupInfoMapper.insert(groupInfo);

			//将群组添加未联系人
			UserContact userContact = new UserContact();
			userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			userContact.setContactType(UserContactTypeEnum.GROUP.getType());
			userContact.setContactId(groupInfo.getGroupId());
			userContact.setUserId(groupInfo.getGroupOwnerId());
			userContact.setCreateTime(curDate);
			userContact.setLastUpdateTime(curDate);
			this.userContactMapper.insert(userContact);

			//创建会话
			String sessionId = StringTools.getChatSessionId4Group(groupInfo.getGroupId());
			ChatSession chatSession = new ChatSession();
			chatSession.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatSession.setLastReceiveTime(curDate.getTime());
			this.chatSessionMapper.insertOrUpdate(chatSession);

			ChatSessionUser chatSessionUser = new ChatSessionUser();
			chatSessionUser.setUserId(groupInfo.getGroupOwnerId());
			chatSessionUser.setSessionId(groupInfo.getGroupId());
			chatSessionUser.setContactName(groupInfo.getGroupName());
			chatSessionUser.setSessionId(sessionId);
			this.chatSessionUserMapper.insert(chatSessionUser);

			//创建消息
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSessionId(sessionId);
			chatMessage.setMessageType(MessageTypeEnum.GROUP_CREATE.getType());
			chatMessage.setMessageContent(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatMessage.setSendTime(curDate.getTime());
			chatMessage.setContactId(groupInfo.getGroupId());
			chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
			chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
			chatMessageMapper.insert(chatMessage);

			//将群组添加到联系人
			redisComponet.addUserContact(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());
			//将联系人通道添加到群组通道
			channelContextUtils.addUser2Group(groupInfo.getGroupOwnerId(), groupInfo.getGroupId());

			//发送ws消息
			chatSessionUser.setLastMessage(MessageTypeEnum.GROUP_CREATE.getInitMessage());
			chatSessionUser.setLastReceiveTime(curDate.getTime());
			chatSessionUser.setMemberCount(1);

			MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
			messageSendDto.setExtendData(chatSessionUser);
			messageSendDto.setLastMessage(chatSession.getLastMessage());

			messageHandler.sendMessage(messageSendDto);

		}else {
			//根据用户查找列表
			GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupInfo.getGroupId());
			//查找是否属于该用户
			if(!dbInfo.getGroupId().equals(groupInfo.getGroupOwnerId())){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			//更新用户信息
			this.groupInfoMapper.updateByGroupId(groupInfo, groupInfo.getGroupId());
			//更新相关表冗余信息
			String contactNameUpdate = null;
			if(!dbInfo.getGroupName().equals(groupInfo.getGroupName())){
				contactNameUpdate = groupInfo.getGroupName();
			}
			if(contactNameUpdate==null){
				return;
			}
			chatSessionUserService.updateRedundanceInfo(contactNameUpdate, groupInfo.getGroupId());

		}

		if(null == avararFile){
			return;
		}
		String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
		//保存文件目录
		File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
		if(!targetFileFolder.exists()){
			targetFileFolder.mkdirs();
		}
		String filePath = targetFileFolder.getPath() + "/" + groupInfo.getGroupId() + Constants.IMAGE_SUFFIX;
		avararFile.transferTo(new File(filePath));
		//保存缩略图
		avatarCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void leaveGroup(String userId, String groupId, MessageTypeEnum messageTypeEnum) throws BusinessException {
		GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
		if (groupInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//创建者不能退出群聊，只能解散群
		if (userId.equals(groupInfo.getGroupOwnerId())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		Integer count = userContactMapper.deleteByUserIdAndContactId(userId, groupId);
		if (count == 0) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		UserInfo userInfo = userInfoMapper.selectByUserId(userId);

		String sessionId = StringTools.getChatSessionId4Group(groupId);
		Date curTime = new Date();
		String messageContent = String.format(messageTypeEnum.getInitMessage(), userInfo.getNickName());
		//更新会话消息
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		chatSession.setLastReceiveTime(curTime.getTime());
		chatSessionMapper.updateBySessionId(chatSession, sessionId);
		//记录消息消息表
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setSessionId(sessionId);
		chatMessage.setSendTime(curTime.getTime());
		chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
		chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		chatMessage.setMessageType(messageTypeEnum.getType());
		chatMessage.setContactId(groupId);
		chatMessage.setMessageContent(messageContent);
		chatMessageMapper.insert(chatMessage);

		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		Integer memberCount = this.userContactMapper.selectCount(userContactQuery);

		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
		messageSendDto.setExtendData(userId);
		messageSendDto.setMemberCount(memberCount);
		messageHandler.sendMessage(messageSendDto);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void dissolutionGroup(String userId, String groupId) throws BusinessException {
		GroupInfo dbInfo = this.groupInfoMapper.selectByGroupId(groupId);
		if (null == groupId || !dbInfo.getGroupOwnerId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//删除群组
		GroupInfo updateInfo = new GroupInfo();
		updateInfo.setStatus(GroupStatusEnum.DISSOLUTION.getStatus());
		this.groupInfoMapper.updateByGroupId(updateInfo, groupId);

		UserContactQuery userContactQuery = new UserContactQuery();
		userContactQuery.setContactId(groupId);
		userContactQuery.setContactType(UserContactTypeEnum.GROUP.getType());

		UserContact updateUserContact = new UserContact();
		updateUserContact.setStatus(UserContactStatusEnum.DEL.getStatus());
		userContactMapper.updateByParam(updateUserContact, userContactQuery);

		List<UserContact> userContactList = this.userContactMapper.selectList(userContactQuery);
		for (UserContact userContact: userContactList){
			redisComponet.removeUserContact(userContact.getUserId(), userContact.getContactId());
		}

		String sessionId = StringTools.getChatSessionId4Group(groupId);
		Date cuerDate = new Date();
		String messageContent = MessageTypeEnum.DISSOLUTION_GROUP.getInitMessage();
		//更新会话消息
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(messageContent);
		chatSession.setLastReceiveTime(cuerDate.getTime());
		chatSessionMapper.updateBySessionId(chatSession, sessionId);
		//记录消息消息表
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setSessionId(sessionId);
		chatMessage.setSendTime(cuerDate.getTime());
		chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
		chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		chatMessage.setMessageType(MessageTypeEnum.DISSOLUTION_GROUP.getType());
		chatMessage.setContactId(groupId);
		chatMessage.setMessageContent(messageContent);
		chatMessageMapper.insert(chatMessage);
		//发送解散群消息
		MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
		messageHandler.sendMessage(messageSendDto);

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addOrRemoveGroupUser(TokenUserInfoDto tokenUserInfoDto, String groupId, String contactIds, Integer opType) throws BusinessException {
		GroupInfo groupInfo = groupInfoMapper.selectByGroupId(groupId);
		if (null == groupInfo || !groupInfo.getGroupOwnerId().equals(tokenUserInfoDto.getUserId())) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		String[] contactIdList = contactIds.split(",");
		for (String contactId : contactIdList) {
			//移除群员
			if (Constants.ZERO.equals(opType)) {
				groupInfoService.leaveGroup(contactId, groupId, MessageTypeEnum.REMOVE_GROUP);
			} else {
				userContactApplyService.addContact(contactId, null, groupId, UserContactTypeEnum.GROUP.getType(), null);
			}
		}
	}

}