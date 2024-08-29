package com.easychat.service;

import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.dto.UserContactSearchResultDto;
import com.easychat.entity.enums.UserContactStatusEnum;
import com.easychat.entity.vo.PaginationResultVO;import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.exception.BusinessException;

import java.util.List;

/**
  * @Desoription:联系人Service
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
public interface UserContactService {

	/**
	  * 根据条件查询列表
	  */
	List<UserContact> findListByParam(UserContactQuery query);

	/**
	  * 根据条件查询数量
	  */
	Integer findCountByParam(UserContactQuery query);

	/**
	  * 分页查询
	  */
	PaginationResultVO<UserContact> findListByPage(UserContactQuery query);

	/**
	  * 新增
	  */
	Integer add(UserContact bean);

	/**
	  * 批量新增
	  */
	Integer addBatch(List<UserContact> listBean);

	/**
	  * 批量新增或修改
	  */
	Integer addOrUpdateBatch(List<UserContact> listBean);

	/**
	  * 根据UserIdAndContactId查询
	  */
	UserContact getUserContactByUserIdAndContactId(String userId, String contactId);

	/**
	  * 根据UserIdAndContactId更新
	  */
	Integer updateUserContactByUserIdAndContactId( UserContact bean, String userId, String contactId);

	/**
	  * 根据UserIdAndContactId删除
	  */
	Integer deleteUserContactByUserIdAndContactId(String userId, String contactId);

	UserContactSearchResultDto searchContact(String userId, String contactId);

	Integer applyAdd(TokenUserInfoDto tokenUserInfoDto, String contactId, String applyInfo) throws BusinessException;

	void removeUserContact(String userId, String contactId, UserContactStatusEnum statusEnum);

	void addContact4Robot(String userId);

}