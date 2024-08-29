package com.easychat.service.impl;

import com.easychat.entity.config.AppConfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.po.UserInfoBeauty;
import com.easychat.entity.query.SimplePage;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.query.UserInfoBeautyQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.query.UserInfoQuery;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.UserContactMapper;
import com.easychat.mappers.UserInfoBeautyMapper;
import com.easychat.mappers.UserInfoMapper;
import com.easychat.redis.RedisComponet;
import com.easychat.service.ChatSessionUserService;
import com.easychat.service.UserContactService;
import com.easychat.service.UserInfoService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
  * @Desoription:用户信息Service
  * 
  * @author:enjoying
  * @data:2024/08/06
  */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService{

	@Resource
	private UserInfoMapper<UserInfo,UserInfoQuery>userInfoMapper;

	@Resource
	private UserInfoBeautyMapper<UserInfoBeauty, UserInfoBeautyQuery> userInfoBeautyMapper;

	@Resource
	private AppConfig appConfig;

	@Resource
	private RedisComponet redisComponet;

    @Autowired
    private UserContactMapper userContactMapper;

	@Resource
	private UserContactService userContactService;

	@Resource
	private ChatSessionUserService chatSessionUserService;

	/**
	  * 根据条件查询列表
	  */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery query) {
		return this.userInfoMapper.selectList(query);
	}

	/**
	  * 根据条件查询数量
	  */
	@Override
	public Integer findCountByParam(UserInfoQuery query) {
		return this.userInfoMapper.selectCount(query);
	}

	/**
	  * 分页查询
	  */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery query) {
		Integer count = this.findCountByParam(query);
		Integer pageSize = query.getPageSize() == null?PageSize.SIZE15.getSize():query.getPageSize();
		SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
		query.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(query);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	  * 新增
	  */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	  * 批量新增
	  */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	  * 批量新增或修改
	  */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	  * 根据UserId查询
	  */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	  * 根据UserId更新
	  */
	@Override
	public Integer updateUserInfoByUserId( UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	  * 根据UserId删除
	  */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	  * 根据Email查询
	  */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	  * 根据Email更新
	  */
	@Override
	public Integer updateUserInfoByEmail( UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	  * 根据Email删除
	  */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void register(String email, String nickName, String password) throws BusinessException {
		//查询用户是否存在
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if (userInfo != null) {
			throw new BusinessException("邮箱已经存在");
		}
		Date curdate = new Date();
		String userId = StringTools.getUserId();

		UserInfoBeauty beautyAccount = this.userInfoBeautyMapper.selectByEmail(email);
		//靓号存在且未使用
		Boolean useBeautyAccount = null != beautyAccount && BeautyAccountStatusEnum.NO_USE.getStatus().equals(beautyAccount.getStatus());
		if(useBeautyAccount){
			userId = UserContactTypeEnum.USER.getPrefix() + beautyAccount.getUserId();
		}
		userInfo = new UserInfo();
		userInfo.setUserId(userId);
		userInfo.setNickName(nickName);
		userInfo.setEmail(email);
		userInfo.setPassword(StringTools.encodeByMD5(password));
		userInfo.setCreateTime(curdate);
		userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		userInfo.setLastOffTime(curdate.getTime());
		userInfo.setJoinType(JoinTypeEnum.APPLY.getType());
		this.userInfoMapper.insert(userInfo);
		if(useBeautyAccount){
			UserInfoBeauty updateBeauty = new UserInfoBeauty();
			updateBeauty.setStatus(BeautyAccountStatusEnum.USEED.getStatus());
			this.userInfoBeautyMapper.updateById(updateBeauty, beautyAccount.getId());
		}

		userContactService.addContact4Robot(userId);

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UserInfoVO login(String email, String password) throws BusinessException {
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if(userInfo == null || !userInfo.getPassword().equals(password)){
			throw new BusinessException("账户或者密码错误");
		}
		if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
			throw new BusinessException("账号已禁用");
		}

		//查询联系人
		UserContactQuery contactQuery = new UserContactQuery();
		contactQuery.setUserId(userInfo.getUserId());
		contactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
		List<UserContact> contactList = userContactMapper.selectList(contactQuery);
		List<String> contactIdList = contactList.stream().map(item->item.getContactId()).collect(Collectors.toList());
		//清空联系人
		redisComponet.cleanUserContact(userInfo.getUserId());
		//添加联系人
		if(!contactIdList.isEmpty()){
			redisComponet.addUserContactBatch(userInfo.getUserId(), contactIdList);
		}

		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(userInfo);
		//获得用户的最后心跳
		Long lastHeartBeat = redisComponet.getUserHeartBeat(userInfo.getUserId());
		if(lastHeartBeat!=null){
			throw new BusinessException("此账号已在别处登录，退出后再次登录");
		}

		//保存登陆信息到Redis中
		String token = StringTools.encodeByMD5(tokenUserInfoDto.getUserId() + StringTools.getRandomString(Constants.LENGTH_20));
		tokenUserInfoDto.setToken(token);
		redisComponet.saveTokenUserInfoDto(tokenUserInfoDto);

		UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
		userInfoVO.setToken(tokenUserInfoDto.getToken());
		userInfoVO.setAdmin(tokenUserInfoDto.getAdmin());
		return userInfoVO;

	}

	private TokenUserInfoDto getTokenUserInfoDto(UserInfo userInfo){
		TokenUserInfoDto tokenUserInfoDto = new TokenUserInfoDto();
		tokenUserInfoDto.setUserId(userInfo.getUserId());
		tokenUserInfoDto.setNickName(userInfo.getNickName());

		String adminEmails= appConfig.getAdminEmails();
		//多个管理员判断
		if (!StringTools.isEmpty(adminEmails) && ArrayUtils.contains(adminEmails.split(","), userInfo.getEmail())) {
			tokenUserInfoDto.setAdmin(true);
		} else {
			tokenUserInfoDto.setAdmin(false);
		}
		return tokenUserInfoDto;

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateUserInfo(UserInfo userInfo, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
		if(avatarFile != null){
			//基础的文件夹
			String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
			//储存头像文件
			File targetFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
			if(!targetFolder.exists()){
				targetFolder.mkdirs();
			}
			//头像保存地址
			String filePath = targetFolder.getPath() + "/" + userInfo.getUserId() + Constants.IMAGE_SUFFIX;
			avatarFile.transferTo(new File(filePath));
			avatarCover.transferTo(new File(filePath + Constants.COVER_IMAGE_SUFFIX));
		}
		UserInfo dbInfo = this.userInfoMapper.selectByUserId(userInfo.getUserId());
		//开启事务
		this.userInfoMapper.updateByUserId(userInfo, userInfo.getUserId());
		String contactNameUpdate = null;
		if(!dbInfo.getNickName().equals(userInfo.getNickName())){
			contactNameUpdate = userInfo.getNickName();
		}
		if(contactNameUpdate == null){
			return;
		}

		//更新token中的昵称
		TokenUserInfoDto tokenUserInfoDto = redisComponet.getTokenUserInfoDtoByUserId(userInfo.getUserId());
		tokenUserInfoDto.setNickName(contactNameUpdate);
		redisComponet.saveTokenUserInfoDto(tokenUserInfoDto);

		chatSessionUserService.updateRedundanceInfo(contactNameUpdate, userInfo.getUserId());
	}
}