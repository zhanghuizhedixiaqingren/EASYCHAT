package com.easychat.service.impl;


import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.SysSettingDto;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.*;
import com.easychat.entity.query.*;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.*;
import com.easychat.redis.RedisComponet;
import com.easychat.service.ChatSessionService;
import com.easychat.service.UserContactApplyService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import com.easychat.websocket.MessageHandler;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
  * @Desoription:联系人申请Service
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
@Service("userContactApplyService")
public class UserContactApplyServiceImpl implements UserContactApplyService{

	@Resource
	private UserContactApplyMapper<UserContactApply,UserContactApplyQuery>userContactApplyMapper;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	@Resource
	private RedisComponet redisComponet;

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
	private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

	@Resource
	private ChannelContextUtils channelContextUtils;



	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserContactApply> findListByParam(UserContactApplyQuery param) {
		return this.userContactApplyMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserContactApplyQuery param) {
		return this.userContactApplyMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserContactApply> list = this.findListByParam(param);
		PaginationResultVO<UserContactApply> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}


	/**
	  * 新增
	  */
	@Override
	public Integer add(UserContactApply bean) {
		return this.userContactApplyMapper.insert(bean);
	}

	/**
	  * 批量新增
	  */
	@Override
	public Integer addBatch(List<UserContactApply> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertBatch(listBean);
	}

	/**
	  * 批量新增或修改
	  */
	@Override
	public Integer addOrUpdateBatch(List<UserContactApply> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userContactApplyMapper.insertOrUpdateBatch(listBean);
	}

	/**
	  * 根据ApplyId查询
	  */
	@Override
	public UserContactApply getUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.selectByApplyId(applyId);
	}

	/**
	  * 根据ApplyId更新
	  */
	@Override
	public Integer updateUserContactApplyByApplyId( UserContactApply bean, Integer applyId) {
		return this.userContactApplyMapper.updateByApplyId(bean, applyId);
	}

	/**
	  * 根据ApplyId删除
	  */
	@Override
	public Integer deleteUserContactApplyByApplyId(Integer applyId) {
		return this.userContactApplyMapper.deleteByApplyId(applyId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void dealWidthApply(String userId, Integer applyId, Integer status) throws BusinessException {
		UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
		if(statusEnum == null || UserContactApplyStatusEnum.INIT==statusEnum){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		//根据applyId查询是否有这条消息
		UserContactApply applyInfo = this.userContactApplyMapper.selectByApplyId(applyId);
		//如果不是接收者，报参数错误
		if(applyInfo == null || !userId.equals(applyInfo.getReceiveUserId())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		UserContactApply updateInfo = new UserContactApply();
		updateInfo.setStatus(statusEnum.getStatus());
		//获取当前系统时间
		updateInfo.setLastApplyTime(System.currentTimeMillis());

		//update user_contact_apply set status = 1, last_apply_time =now() where apply_id = ??? and status=0  加了一个锁，只能单向流变化
		UserContactApplyQuery applyQuery = new UserContactApplyQuery();
		applyQuery.setApplyId(applyId);
		applyQuery.setStatus(UserContactApplyStatusEnum.INIT.getStatus());

		Integer count = userContactApplyMapper.updateByParam(updateInfo, applyQuery);
		if(count == 0) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if(UserContactApplyStatusEnum.PASS.getStatus().equals(status)){
			//添加联系人
			this.addContact(applyInfo.getApplyUserId(),applyInfo.getReceiveUserId(), applyInfo.getContactId(), applyInfo.getContactType(), applyInfo.getApplyInfo());
			return;
		}

		//拉黑
		if(UserContactApplyStatusEnum.BLACKLIST.getStatus().equals(status)){
			Date curDate = new Date();
			UserContact userContact = new UserContact();
			userContact.setUserId(applyInfo.getApplyUserId());
			userContact.setContactId(applyInfo.getContactId());
			userContact.setContactType(applyInfo.getContactType());
			userContact.setStatus(UserContactStatusEnum.BLACKLIST_BE_FIRST.getStatus());
			userContact.setLastUpdateTime(curDate);
			//如果申请过就插入，
			userContactMapper.insertOrUpdate(userContact);
		}
	}

	@Override
	public void addContact(String applyUserId, String receiveUserId, String contactId, Integer contactType, String applyInfo) throws BusinessException {

		//添加群聊
		//判断群聊人数
		if(UserContactTypeEnum.GROUP.getType().equals(contactType)){
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setContactId(contactId);
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			Integer count = userContactMapper.selectCount(userContactQuery);
			SysSettingDto sysSettingDto = redisComponet.getsysSettingDto();
			if(count >= sysSettingDto.getMaxGroupCount()){
				throw new BusinessException("成员已经满员，无法加入");
			}
		}
		Date curDate = new Date();
		//同意，双方添加好友
		List<UserContact> contactList = new ArrayList<>();
		//申请人添加对方
		UserContact userContact = new UserContact();
		userContact.setUserId(applyUserId);
		userContact.setContactId(contactId);
		userContact.setContactType(contactType);
		userContact.setCreateTime(curDate);
		userContact.setLastUpdateTime(curDate);
		userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		contactList.add(userContact);
		//如果申请好友，接受人添加申请人，群组不用添加对方好友
		if(UserContactTypeEnum.USER.getType().equals(contactType)){
			userContact = new UserContact();
			//申请人和被申请人交换
			userContact.setUserId(receiveUserId);
			userContact.setContactId(applyUserId);
			userContact.setContactType(contactType);
			userContact.setCreateTime(curDate);
			userContact.setLastUpdateTime(curDate);
			userContact.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			contactList.add(userContact);
		}
		//批量加入
		userContactMapper.insertOrUpdateBatch(contactList);
		//如果是好友，添加到redis缓存中
		if(UserContactTypeEnum.USER.getType().equals(contactType)){
			//再申请人中增加好友
			redisComponet.addUserContact(receiveUserId, applyUserId);
		}
		//再接收人中增加好友
		redisComponet.addUserContact(applyUserId, contactId);

		//创建会话
		String sessionId = null;
		if(UserContactTypeEnum.USER.getType().equals(contactType)){
			sessionId = StringTools.getChatSessionId4User(new String[]{applyUserId, contactId});
		}else {
			sessionId = StringTools.getChatSessionId4Group(contactId);
		}

		List<ChatSessionUser> chatSessionUserList = new ArrayList<>();
		if(UserContactTypeEnum.USER.getType().equals(contactType)){
			//创建会话
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastMessage(applyInfo);
			chatSession.setLastReceiveTime(curDate.getTime());
			this.chatSessionMapper.insertOrUpdate(chatSession);

			//申请人session
			ChatSessionUser applySessionUser = new ChatSessionUser();
			applySessionUser.setUserId(applyUserId);
			applySessionUser.setContactId(contactId);
			applySessionUser.setSessionId(sessionId);

			UserInfo contactUser = this.userInfoMapper.selectByUserId(contactId);

			applySessionUser.setContactName(contactUser.getNickName());
			chatSessionUserList.add(applySessionUser);

			//接收人session
			ChatSessionUser contactSessionUser = new ChatSessionUser();
			//反向处理
			contactSessionUser.setUserId(contactId);
			contactSessionUser.setContactId(applyUserId);
			contactSessionUser.setSessionId(sessionId);
			contactSessionUser.setLastReceiveTime(curDate.getTime());
			contactSessionUser.setLastMessage(applyInfo);

			UserInfo applyUserInfo = this.userInfoMapper.selectByUserId(applyUserId);
			contactSessionUser.setContactName(applyUserInfo.getNickName());
			chatSessionUserList.add(contactSessionUser);
			this.chatSessionUserMapper.insertOrUpdateBatch(chatSessionUserList);

			//记录消息表
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSessionId(sessionId);
			chatMessage.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
			chatMessage.setMessageContent(applyInfo);
			chatMessage.setSendUserId(applyUserId);
			chatMessage.setSendUserNickName(applyUserInfo.getNickName());
			chatMessage.setSendTime(curDate.getTime());
			chatMessage.setContactId(contactId);
			chatMessage.setContactType(UserContactTypeEnum.USER.getType());
			chatMessageMapper.insert(chatMessage);

			MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
			//发送给接受好友申请的人
			messageHandler.sendMessage(messageSendDto);

			//发送给申请人, 发送人就是接收人,联系人就是申请人
			messageSendDto.setMessageType(MessageTypeEnum.ADD_FRIEND_SELF.getType());
			messageSendDto.setContactId(applyUserId);
			messageSendDto.setExtendData(contactUser);
			messageHandler.sendMessage(messageSendDto);

		}else{
			//申请人加入群组
			ChatSessionUser chatSessionUser = new ChatSessionUser();
			chatSessionUser.setUserId(applyUserId);
			chatSessionUser.setContactId(contactId);
			GroupInfo groupInfo = this.groupInfoMapper.selectByGroupId(contactId);
			chatSessionUser.setContactId(groupInfo.getGroupId());
			chatSessionUser.setSessionId(sessionId);
			this.chatSessionUserMapper.insertOrUpdate(chatSessionUser);

			//查询申请人消息
			UserInfo applyUserInfo = this.userInfoMapper.selectByUserId(applyUserId);
			String sendMessage = String.format(MessageTypeEnum.ADD_GROUP.getInitMessage(), applyUserInfo.getNickName());

			//增加session消息
			ChatSession chatSession = new ChatSession();
			chatSession.setSessionId(sessionId);
			chatSession.setLastReceiveTime(curDate.getTime());
			chatSession.setLastMessage(sendMessage);
			this.chatSessionMapper.insertOrUpdate(chatSession);

			//增加聊天消息
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSessionId(sessionId);
			chatMessage.setMessageType(MessageTypeEnum.ADD_GROUP.getType());
			chatMessage.setMessageContent(sendMessage);
			chatMessage.setSendTime(curDate.getTime());
			chatMessage.setContactId(contactId);
			chatMessage.setContactType(UserContactTypeEnum.GROUP.getType());
			chatMessage.setStatus(MessageStatusEnum.SENDED.getStatus());
			this.chatSessionMapper.insert(chatMessage);

			//将群组添加到联系人
			redisComponet.addUserContact(applyUserId, groupInfo.getGroupId());
			//将联系人通道添加到群组通道
			channelContextUtils.addUser2Group(applyUserId, groupInfo.getGroupId());

			//发送群消息
			MessageSendDto messageSendDto = CopyTools.copy(chatMessage, MessageSendDto.class);
			messageSendDto.setContactId(contactId);

			//获取群人数
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setContactId(contactId);
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			Integer memberCount = this.userContactMapper.selectCount(userContactQuery);
			messageSendDto.setMemberCount(memberCount);
			messageSendDto.setContactName(groupInfo.getGroupName());
			//发消息
			messageHandler.sendMessage(messageSendDto);


		}

	}

}