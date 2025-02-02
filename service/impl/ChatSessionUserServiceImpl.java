package com.easychat.service.impl;

import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.enums.MessageTypeEnum;
import com.easychat.entity.enums.UserContactStatusEnum;
import com.easychat.entity.enums.UserContactTypeEnum;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.SimplePage;
import com.easychat.entity.enums.PageSize;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.ChatSessionUser;
import com.easychat.entity.query.ChatSessionUserQuery;
import com.easychat.mappers.ChatSessionUserMapper;
import com.easychat.mappers.UserContactMapper;
import com.easychat.service.ChatSessionUserService;
import com.easychat.service.UserContactService;
import com.easychat.websocket.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
  * @Desoription:会话用户Service
  * 
  * @author:enjoying
  * @data:2024/08/14
  */
@Service("chatSessionUserService")
public class ChatSessionUserServiceImpl implements ChatSessionUserService{

	@Resource
	private ChatSessionUserMapper<ChatSessionUser,ChatSessionUserQuery>chatSessionUserMapper;

	@Resource
	private MessageHandler messageHandler;

	@Resource
	private UserContactMapper<UserContact, UserContactQuery> userContactMapper;

	/**
	  * 根据条件查询列表
	  */
	@Override
	public List<ChatSessionUser> findListByParam(ChatSessionUserQuery query) {
		return this.chatSessionUserMapper.selectList(query);
	}

	/**
	  * 根据条件查询数量
	  */
	@Override
	public Integer findCountByParam(ChatSessionUserQuery query) {
		return this.chatSessionUserMapper.selectCount(query);
	}

	/**
	  * 分页查询
	  */
	@Override
	public PaginationResultVO<ChatSessionUser> findListByPage(ChatSessionUserQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null?PageSize.SIZE15.getSize():query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<ChatSessionUser> list = this.findListByParam(query);
		PaginationResultVO<ChatSessionUser> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	  * 新增
	  */
	@Override
	public Integer add(ChatSessionUser bean) {
		return this.chatSessionUserMapper.insert(bean);
	}

	/**
	  * 批量新增
	  */
	@Override
	public Integer addBatch(List<ChatSessionUser> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatSessionUserMapper.insertBatch(listBean);
	}

	/**
	  * 批量新增或修改
	  */
	@Override
	public Integer addOrUpdateBatch(List<ChatSessionUser> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatSessionUserMapper.insertOrUpdateBatch(listBean);
	}

	/**
	  * 根据UserIdAndContactId查询
	  */
	@Override
	public ChatSessionUser getChatSessionUserByUserIdAndContactId(String userId, String contactId) {
		return this.chatSessionUserMapper.selectByUserIdAndContactId(userId, contactId);
	}

	/**
	  * 根据UserIdAndContactId更新
	  */
	@Override
	public Integer updateChatSessionUserByUserIdAndContactId( ChatSessionUser bean, String userId, String contactId) {
		return this.chatSessionUserMapper.updateByUserIdAndContactId(bean, userId, contactId);
	}

	/**
	  * 根据UserIdAndContactId删除
	  */
	@Override
	public Integer deleteChatSessionUserByUserIdAndContactId(String userId, String contactId) {
		return this.chatSessionUserMapper.deleteByUserIdAndContactId(userId, contactId);
	}

	@Override
	public void updateRedundanceInfo(String contactName, String contactId){
		ChatSessionUser updateInfo = new ChatSessionUser();
		updateInfo.setContactName(contactName);

		ChatSessionUserQuery chatSessionUserQuery = new ChatSessionUserQuery();
		chatSessionUserQuery.setContactId(contactId);
		this.chatSessionUserMapper.updateByParam(updateInfo, chatSessionUserQuery);

		UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByPrefix(contactId);

		if(contactTypeEnum == UserContactTypeEnum.GROUP){
			//修改群昵称发送ws消息
			MessageSendDto messageSendDto = new MessageSendDto();
			messageSendDto.setContactType(UserContactTypeEnum.getByPrefix(contactId).getType());
			messageSendDto.setContactId(contactId);
			messageSendDto.setExtendData(contactName);
			messageSendDto.setMessageType(MessageTypeEnum.CONTACT_NAME_UPDATE.getType());
			messageHandler.sendMessage(messageSendDto);
		}else {
			UserContactQuery userContactQuery = new UserContactQuery();
			userContactQuery.setContactType(UserContactTypeEnum.USER.getType());
			userContactQuery.setContactId(contactId);
			userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
			List<UserContact> userContactList = userContactMapper.selectList(userContactQuery);
			for(UserContact userContact : userContactList){
				MessageSendDto messageSendDto = new MessageSendDto();
				messageSendDto.setContactType(contactTypeEnum.getType());
				messageSendDto.setContactId(userContact.getUserId());
				messageSendDto.setExtendData(contactName);
				messageSendDto.setMessageType(MessageTypeEnum.CONTACT_NAME_UPDATE.getType());
				messageSendDto.setSendUserId(contactId);
				messageSendDto.setSendUserNickName(contactName);
				messageHandler.sendMessage(messageSendDto);
			}
		}
	}

}