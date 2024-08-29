package com.easychat.service;

import com.easychat.entity.vo.PaginationResultVO;import com.easychat.entity.po.ChatSessionUser;
import com.easychat.entity.query.ChatSessionUserQuery;
import java.util.List;

/**
  * @Desoription:会话用户Service
  * 
  * @author:enjoying
  * @data:2024/08/14
  */
public interface ChatSessionUserService {

	/**
	  * 根据条件查询列表
	  */
	List<ChatSessionUser> findListByParam(ChatSessionUserQuery query);

	/**
	  * 根据条件查询数量
	  */
	Integer findCountByParam(ChatSessionUserQuery query);

	/**
	  * 分页查询
	  */
	PaginationResultVO<ChatSessionUser> findListByPage(ChatSessionUserQuery query);

	/**
	  * 新增
	  */
	Integer add(ChatSessionUser bean);

	/**
	  * 批量新增
	  */
	Integer addBatch(List<ChatSessionUser> listBean);

	/**
	  * 批量新增或修改
	  */
	Integer addOrUpdateBatch(List<ChatSessionUser> listBean);

	/**
	  * 根据UserIdAndContactId查询
	  */
	ChatSessionUser getChatSessionUserByUserIdAndContactId(String userId, String contactId);

	/**
	  * 根据UserIdAndContactId更新
	  */
	Integer updateChatSessionUserByUserIdAndContactId( ChatSessionUser bean, String userId, String contactId);

	/**
	  * 根据UserIdAndContactId删除
	  */
	Integer deleteChatSessionUserByUserIdAndContactId(String userId, String contactId);

	void updateRedundanceInfo(String contactName, String contactId);

}