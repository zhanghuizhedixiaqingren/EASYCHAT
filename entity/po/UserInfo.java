package com.easychat.entity.po;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import com.easychat.entity.enums.DateTimePatternEnum;
import com.easychat.utils.DateUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
  * @Desoription:用户信息
  * 
  * @author:enjoying
  * @data:2024/08/06
  */
public class UserInfo implements Serializable {
	/**
	  * 用户ID
	  */
	private String userId;

	/**
	  * 邮箱
	  */
	private String email;

	/**
	  * 昵称
	  */
	private String nickName;

	/**
	  * 0：直接加入 1：统一后加好友
	  */
	private Integer joinType;

	/**
	  * 0: 男 1：女
	  */
	private Integer sex;

	/**
	  * 密码
	  */
	private String password;

	/**
	  * 个性签名
	  */
	private String personalSignature;

	/**
	  * 状态
	  */
	@JsonIgnore
	private Integer status;

	/**
	  * 创建时间
	  */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT + 8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private java.util.Date createTime;

	/**
	  * 最后登陆时间
	  */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT + 8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private java.util.Date lastLoginTime;

	/**
	  * 地区
	  */
	private String areaName;

	/**
	  * 地区编号
	  */
	private String areaCode;

	/**
	  * 最后离线时间
	  */
	private Long lastOffTime;

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		 return this.userId;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		 return this.email;
	}

	public void setNickName(String nickName){
		this.nickName = nickName;
	}

	public String getNickName(){
		 return this.nickName;
	}

	public void setJoinType(Integer joinType){
		this.joinType = joinType;
	}

	public Integer getJoinType(){
		 return this.joinType;
	}

	public void setSex(Integer sex){
		this.sex = sex;
	}

	public Integer getSex(){
		 return this.sex;
	}

	public void setPassword(String password){
		this.password = password;
	}

	public String getPassword(){
		 return this.password;
	}

	public void setPersonalSignature(String personalSignature){
		this.personalSignature = personalSignature;
	}

	public String getPersonalSignature(){
		 return this.personalSignature;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		 return this.status;
	}

	public void setCreateTime(java.util.Date createTime){
		this.createTime = createTime;
	}

	public java.util.Date getCreateTime(){
		 return this.createTime;
	}

	public void setLastLoginTime(java.util.Date lastLoginTime){
		this.lastLoginTime = lastLoginTime;
	}

	public java.util.Date getLastLoginTime(){
		 return this.lastLoginTime;
	}

	public void setAreaName(String areaName){
		this.areaName = areaName;
	}

	public String getAreaName(){
		 return this.areaName;
	}

	public void setAreaCode(String areaCode){
		this.areaCode = areaCode;
	}

	public String getAreaCode(){
		 return this.areaCode;
	}

	public void setLastOffTime(Long lastOffTime){
		this.lastOffTime = lastOffTime;
	}

	public Long getLastOffTime(){
		 return this.lastOffTime;
	}

	@Override
	 public String toString (){
		return "用户ID:" + (userId == null ? "空" : userId) + ", 邮箱:" + (email == null ? "空" : email) + ", 昵称:" + (nickName == null ? "空" : nickName) + ", 0：直接加入 1：统一后加好友:" + (joinType == null ? "空" : joinType) + ", 0: 男 1：女:" + (sex == null ? "空" : sex) + ", 密码:" + (password == null ? "空" : password) + ", 个性签名:" + (personalSignature == null ? "空" : personalSignature) + ", 状态:" + (status == null ? "空" : status) + ", 创建时间:" + (createTime == null ? "空" : DateUtils.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ", 最后登陆时间:" + (lastLoginTime == null ? "空" : DateUtils.format(lastLoginTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + ", 地区:" + (areaName == null ? "空" : areaName) + ", 地区编号:" + (areaCode == null ? "空" : areaCode) + ", 最后离线时间:" + (lastOffTime == null ? "空" : lastOffTime);
	}
}