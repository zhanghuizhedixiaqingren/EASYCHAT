package com.easychat.service;

import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.vo.PaginationResultVO;import com.easychat.entity.po.ChatMessage;
import com.easychat.entity.query.ChatMessageQuery;
import com.easychat.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
  * @Desoription:聊天消息表Service
  * 
  * @author:enjoying
  * @data:2024/08/14
  */
public interface ChatMessageService {

	/**
	  * 根据条件查询列表
	  */
	List<ChatMessage> findListByParam(ChatMessageQuery query);

	/**
	  * 根据条件查询数量
	  */
	Integer findCountByParam(ChatMessageQuery query);

	/**
	  * 分页查询
	  */
	PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery query);

	/**
	  * 新增
	  */
	Integer add(ChatMessage bean);

	/**
	  * 批量新增
	  */
	Integer addBatch(List<ChatMessage> listBean);

	/**
	  * 批量新增或修改
	  */
	Integer addOrUpdateBatch(List<ChatMessage> listBean);

	/**
	  * 根据MessageId查询
	  */
	ChatMessage getChatMessageByMessageId(Long messageId);

	/**
	  * 根据MessageId更新
	  */
	Integer updateChatMessageByMessageId( ChatMessage bean, Long messageId);

	/**
	  * 根据MessageId删除
	  */
	Integer deleteChatMessageByMessageId(Long messageId);

	MessageSendDto saveMessage(ChatMessage chatMessage, TokenUserInfoDto tokenUserInfoDto) throws BusinessException;

	void saveMessageFile(String userId, Long messageId, MultipartFile file, MultipartFile cover) throws BusinessException;

	File downloadFile(TokenUserInfoDto tokenUserInfoDto, Long fileId, Boolean showCover) throws BusinessException;

}