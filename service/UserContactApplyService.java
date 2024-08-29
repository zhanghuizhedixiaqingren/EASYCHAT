package com.easychat.service;

import java.io.Serializable;

import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.UserContactApply;
import com.easychat.entity.query.UserContactApplyQuery;
import com.easychat.exception.BusinessException;

import java.util.List;

/**
  * @Desoription:联系人申请Service
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
public interface UserContactApplyService {

	/**
	  * 根据条件查询列表
	  */
	List<UserContactApply> findListByParam(UserContactApplyQuery query);

	/**
	  * 根据条件查询数量
	  */
	Integer findCountByParam(UserContactApplyQuery query);

	/**
	  * 分页查询
	  */
	PaginationResultVO<UserContactApply> findListByPage(UserContactApplyQuery query);

	/**
	  * 新增
	  */
	Integer add(UserContactApply bean);

	/**
	  * 批量新增
	  */
	Integer addBatch(List<UserContactApply> listBean);

	/**
	  * 批量新增或修改
	  */
	Integer addOrUpdateBatch(List<UserContactApply> listBean);

	/**
	  * 根据ApplyId查询
	  */
	UserContactApply getUserContactApplyByApplyId(Integer applyId);

	/**
	  * 根据ApplyId更新
	  */
	Integer updateUserContactApplyByApplyId( UserContactApply bean, Integer applyId);

	/**
	  * 根据ApplyId删除
	  */
	Integer deleteUserContactApplyByApplyId(Integer applyId);

	void dealWidthApply(String userId, Integer applyId, Integer status) throws BusinessException;

	void addContact(String applyUserId, String receiveUserId, String contactId, Integer contactType, String applyInfo) throws BusinessException;

}