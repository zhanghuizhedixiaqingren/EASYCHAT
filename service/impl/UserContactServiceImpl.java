package com.easychat.service.impl;

import java.io.Serializable;

import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.SysSettingDto;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.dto.UserContactSearchResultDto;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.*;
import com.easychat.entity.query.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.*;
import com.easychat.redis.RedisComponet;
import com.easychat.service.ChatSessionService;
import com.easychat.service.GroupInfoService;
import com.easychat.service.UserContactApplyService;
import com.easychat.service.UserContactService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import com.easychat.websocket.MessageHandler;
import jodd.util.ArraysUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Arrays;
import java.util.List;

/**
  * @Desoription:联系人Service
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
@Service("userContactService")
public class UserContactServiceImpl implements UserContactService{

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	@Resource
	private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

	@Resource
	private UserContactMapper<UserContact,UserContactQuery>userContactMapper;

	@Resource
	private UserContactApplyMapper<UserContactApply, UserContactApplyQuery>userContactApplyMapper;

	@Resource
	private UserContactApplyService userContactApplyService;

	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

	@Resource
	private ChatSessionUserMapper<ChatSessionUser, ChatSessionUserQuery> chatSessionUserMapper;

	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

	@Resource
	private MessageHandler messageHandler;
    @Autowired
    private RedisComponet redisComponet;

	/**
	  * 根据条件查询列表
	  */
	@Override
	public List<UserContact> findListByParam(UserContactQuery query) {
		return this.userContactMapper.selectList(query);
	}

	/**
	  * 根据条件查询数量
	  */
	@Override
	public Integer findCountByParam(UserContactQuery query) {
		return this.userContactMapper.selectCount(query);
	}


	/**
	  * 分页查询
	  */
	@Override
	public PaginationResultVO<UserContact> findListByPage(UserContactQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null?PageSize.SIZE15.getSize():query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserContact> list = this.findListByParam(query);
		PaginationResultVO<UserContact> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	  * 新增
	  */
	@Override
	public Integer add(UserContact bean) {
		return this.userContactMapper.insert(bean);
	}

	/**
	  * 批量新增
	  */
	@Override
	public Integer addBatch(List<UserContact> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactMapper.insertBatch(listBean);
	}

	/**
	  * 批量新增或修改
	  */
	@Override
	public Integer addOrUpdateBatch(List<UserContact> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactMapper.insertOrUpdateBatch(listBean);
	}

	/**
	  * 根据UserIdAndContactId查询
	  */
	@Override
	public UserContact getUserContactByUserIdAndContactId(String userId, String contactId) {
		return this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
	}

	/**
	  * 根据UserIdAndContactId更新
	  */
	@Override
	public Integer updateUserContactByUserIdAndContactId( UserContact bean, String userId, String contactId) {
		return this.userContactMapper.updateByUserIdAndContactId(bean, userId, contactId);
	}

	/**
	  * 根据UserIdAndContactId删除
	  */
	@Override
	public Integer deleteUserContactByUserIdAndContactId(String userId, String contactId) {
		return this.userContactMapper.deleteByUserIdAndContactId(userId, contactId);
	}

	@Override
	public UserContactSearchResultDto searchContact(String userId, String contactId) {
		//根据前缀（U或者G）
		UserContactTypeEnum typeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if(typeEnum==null){
			return null;
		}
		UserContactSearchResultDto resultDto = new UserContactSearchResultDto();
		switch (typeEnum){
			case USER:
				UserInfo userInfo = userInfoMapper.selectByUserId(contactId);
				if(userInfo ==null){
					return null;
				}
				resultDto = CopyTools.copy(userInfo, UserContactSearchResultDto.class);
				break;
			case GROUP:
				GroupInfo groupInfo = groupInfoMapper.selectByGroupId(contactId);
				if(groupInfo ==null){
					return null;
				}
				resultDto.setNickName(groupInfo.getGroupName());
				break;
		}
		resultDto.setContactType(typeEnum.toString());
		resultDto.setContactId(contactId);

		if(userId.equals(contactId)){
			resultDto.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			return resultDto;
		}
		//查询是否是好友
		UserContact userContact = this.userContactMapper.selectByUserIdAndContactId(userId, contactId);
		resultDto.setStatus(userContact==null?null:userContact.getStatus());

		return resultDto;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer applyAdd(TokenUserInfoDto tokenUserInfoDto, String contactId, String applyInfo) throws BusinessException {
		UserContactTypeEnum userContactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);
		if(userContactTypeEnum == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//申请人信息
		String applyUserId = tokenUserInfoDto.getUserId();

		//默认申请人信息 (我是%s + 昵称)
		applyInfo= StringTools.isEmpty(applyInfo) ? String.format(Constants.APPLY_INFO_TEMPLATE, tokenUserInfoDto.getNickName()) : applyInfo;

		//系统当前时间
		Long curTime = System.currentTimeMillis();

		Integer joinType = null;
		//接收人信息
		String receiveUserId = contactId;

		//查询对方好友是否已经添加，如果已经拉黑无法添加
		UserContact userContact = userContactMapper.selectByUserIdAndContactId(applyUserId, contactId);
		if((userContact != null) &&
                ArraysUtil.contains(new Integer[]{UserContactStatusEnum.BLACKLIST_BE.getStatus(),
                                UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus()
						}, userContact.getStatus())){
			throw new BusinessException("对方已把你拉黑，无法添加");
		}

		if (UserContactTypeEnum.GROUP == userContactTypeEnum){
			GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
			if(groupInfo == null || GroupStatusEum.DISSOLUTION.getStatus().equals(groupInfo.getStatus())){
				throw new BusinessException("群聊不存在或者已解散");
			}
			//群主作为接收者
			receiveUserId = groupInfo.getGroupOwnerId();
			joinType = groupInfo.getJoinType();
		}else {
			UserInfo userInfo = this.userInfoMapper.selectByUserId(contactId);
			if(userInfo == null){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			joinType = userInfo.getJoinType();
		}
		//直接加入不用申请记录
		if(JoinTypeEnum.JOIN.equals(joinType)){
			//添加联系人
			userContactApplyService.addContact(applyUserId, receiveUserId, contactId, userContactTypeEnum.getType(), applyInfo);
			return joinType;
		}
		//
		UserContactApply dbApply = this.userContactApplyMapper.selectByApplyUserIdAndReceiveUserIdAndContactId(applyUserId, receiveUserId, contactId);
		if(dbApply == null){
			UserContactApply contactApply = new UserContactApply();
			contactApply.setApplyUserId(applyUserId);
			contactApply.setContactType(userContactTypeEnum.getType());
			contactApply.setReceiveUserId(receiveUserId);
			contactApply.setLastApplyTime(curTime);
			contactApply.setContactId(contactId);
			contactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
			contactApply.setApplyInfo(applyInfo);
			this.userContactApplyMapper.insert(contactApply);
		}else {
			//更新状态
			UserContactApply contactApply = new UserContactApply();
			contactApply.setStatus(UserContactApplyStatusEnum.INIT.getStatus());
			contactApply.setLastApplyTime(curTime);
			contactApply.setApplyInfo(applyInfo);
			this.userContactApplyMapper.updateByApplyId(contactApply, dbApply.getApplyId());
		}
		if(dbApply ==null || !UserContactApplyStatusEnum.INIT.getStatus().equals(dbApply.getStatus())){
			MessageSendDto messageSendDto = new MessageSendDto();
			//设置类型为申请好友
			messageSendDto.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
			messageSendDto.setMessageContent(applyInfo);
			messageSendDto.setContactId(receiveUserId);
			//发给接收人,支持集群化部署
			messageHandler.sendMessage(messageSendDto);

		}
		return joinType;
	}

	@Override
	public void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum) {
		//自己中移除好友
		UserContact userContact = new UserContact();
		userContact.setStatus(statusEnum.getStatus());
		userContactMapper.updateByUserIdAndContactId(userContact, userId, contactId);

		//好友中移除自己
		UserContact friendContact = new UserContact();
		//如果是删除了好友,好友显示被删除
		if(UserContactStatusEnum.DEL == statusEnum){
			friendContact.setStatus(UserContactStatusEnum.DEL_BE.getStatus());
		}else if(UserContactStatusEnum.BLACKLIST == statusEnum){
			friendContact.setStatus(UserContactStatusEnum.BLACKLIST_BE.getStatus());
		}
		//更新朋友联系列表
		userContactMapper.updateByUserIdAndContactId(friendContact, contactId, userId);
		//从我的好友列表缓存中删除好友
		redisComponet.removeUserContact(contactId, userId);

		//从好友缓存中删除我
		redisComponet.removeUserContact(userId,contactId);
	}


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addContact4Robot(String userId) {
		Date curDate = new Date();
		SysSettingDto sysSettingDto = new SysSettingDto();
		String contactId = sysSettingDto.getRobotUid();
		String contactName = sysSettingDto.getRobotNickName();
		String sendMessage = sysSettingDto.getRobotWelcome();
		sendMessage = StringTools.cleanHtmlTag(sendMessage);
		//添加机器人好友
		UserContact userContact = new UserContact();
		userContact.setUserId(userId);
		userContact.setContactId(contactId);
		userContact.setContactType(UserContactTypeEnum.USER.getType());
		userContact.setCreateTime(curDate);
		userContact.setLastUpdateTime(curDate);
		userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		userContactMapper.insert(userContact);
		//增加会话信息
		String sessionId = StringTools.getChatSessionId4User(new String[]{userId, contactId});
		ChatSession chatSession = new ChatSession();
		chatSession.setLastMessage(sendMessage);
		chatSession.setSessionId(sessionId);
		chatSession.setLastReceiveTime(curDate.getTime());
		this.chatSessionMapper.insert(chatSession);

		//增加会话人信息
		ChatSessionUser chatSessionUser = new ChatSessionUser();
		chatSessionUser.setUserId(userId);
		chatSessionUser.setContactId(contactId);
		chatSessionUser.setContactName(contactName);
		chatSessionUser.setSessionId(sessionId);
		this.chatSessionUserMapper.insert(chatSessionUser);

		//增加聊天消息
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setSessionId(sessionId);
		//设置为普通聊天消息
		chatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
		chatMessage.setMessageContent(sendMessage);
		chatMessage.setSendUserId(contactId);
		chatMessage.setSendUserNickName(contactName);
		chatMessage.setSendTime(curDate.getTime());
		chatMessage.setContactId(userId);
		chatMessage.setContactType(UserContactTypeEnum.USER.getType());
		chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
		chatMessageMapper.insert(chatMessage);

	}
}