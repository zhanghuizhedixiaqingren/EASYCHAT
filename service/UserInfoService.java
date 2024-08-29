package com.easychat.service;

import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.exception.BusinessException;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.query.UserInfoQuery;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
  * @Desoription:用户信息Service
  * 
  * @author:enjoying
  * @data:2024/08/06
  */
public interface UserInfoService {

	/**
	  * 根据条件查询列表
	  */
	List<UserInfo> findListByParam(UserInfoQuery query);

	/**
	  * 根据条件查询数量
	  */
	Integer findCountByParam(UserInfoQuery query);

	/**
	  * 分页查询
	  */
	PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query);

	/**
	  * 新增
	  */
	Integer add(UserInfo bean);

	/**
	  * 批量新增
	  */
	Integer addBatch(List<UserInfo> listBean);

	/**
	  * 批量新增或修改
	  */
	Integer addOrUpdateBatch(List<UserInfo> listBean);

	/**
	  * 根据UserId查询
	  */
	UserInfo getUserInfoByUserId(String userId);

	/**
	  * 根据UserId更新
	  */
	Integer updateUserInfoByUserId( UserInfo bean, String userId);

	/**
	  * 根据UserId删除
	  */
	Integer deleteUserInfoByUserId(String userId);

	/**
	  * 根据Email查询
	  */
	UserInfo getUserInfoByEmail(String email);

	/**
	  * 根据Email更新
	  */
	Integer updateUserInfoByEmail( UserInfo bean, String email);

	/**
	  * 根据Email删除
	  */
	Integer deleteUserInfoByEmail(String email);

	//注册
	void register(String email, String nickName, String password) throws BusinessException;

	UserInfoVO login(String email, String password) throws BusinessException;

	void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException;

}