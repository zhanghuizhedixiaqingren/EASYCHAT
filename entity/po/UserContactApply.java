package com.easychat.entity.po;

import java.io.Serializable;

import com.easychat.entity.enums.UserContactApplyStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;


/**
  * @Desoription:联系人申请
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
public class UserContactApply implements Serializable {
	/**
	  * 自增ID
	  */
	private Integer applyId;

	/**
	  * 申请人ID
	  */
	private String applyUserId;

	/**
	  * 接收人ID
	  */
	private String receiveUserId;

	/**
	  * 联系人类型 0：好友 1：群组
	  */
	private Integer contactType;

	/**
	  * 如果是群组，则为群组ID；否则可以为NULL
	  */
	private String contactId;

	/**
	  * 最后申请时间
	  */
	private Long lastApplyTime;

	/**
	  * 状态 0：待处理 1：已同意 2：已拒绝 3：已拉黑
	  */
	@JsonIgnore
	private Integer status;

	/**
	  * 申请信息
	  */
	private String applyInfo;

	private String contactName;

	private String statusName;

	public String getStatusName() {
		//返回状态名称
		UserContactApplyStatusEnum statusEnum = UserContactApplyStatusEnum.getByStatus(status);
		return statusEnum == null ? null : statusEnum.getDesc();
	}

	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public void setApplyId(Integer applyId){
		this.applyId = applyId;
	}

	public Integer getApplyId(){
		 return this.applyId;
	}

	public void setApplyUserId(String applyUserId){
		this.applyUserId = applyUserId;
	}

	public String getApplyUserId(){
		 return this.applyUserId;
	}

	public void setReceiveUserId(String receiveUserId){
		this.receiveUserId = receiveUserId;
	}

	public String getReceiveUserId(){
		 return this.receiveUserId;
	}

	public void setContactType(Integer contactType){
		this.contactType = contactType;
	}

	public Integer getContactType(){
		 return this.contactType;
	}

	public void setContactId(String contactId){
		this.contactId = contactId;
	}

	public String getContactId(){
		 return this.contactId;
	}

	public void setLastApplyTime(Long lastApplyTime){
		this.lastApplyTime = lastApplyTime;
	}

	public Long getLastApplyTime(){
		 return this.lastApplyTime;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		 return this.status;
	}

	public void setApplyInfo(String applyInfo){
		this.applyInfo = applyInfo;
	}

	public String getApplyInfo(){
		 return this.applyInfo;
	}

	@Override
	 public String toString (){
		return "自增ID:" + (applyId == null ? "空" : applyId) + ", 申请人ID:" + (applyUserId == null ? "空" : applyUserId) + ", 接收人ID:" + (receiveUserId == null ? "空" : receiveUserId) + ", 联系人类型 0：好友 1：群组:" + (contactType == null ? "空" : contactType) + ", 如果是群组，则为群组ID；否则可以为NULL:" + (contactId == null ? "空" : contactId) + ", 最后申请时间:" + (lastApplyTime == null ? "空" : lastApplyTime) + ", 状态 0：待处理 1：已同意 2：已拒绝 3：已拉黑:" + (status == null ? "空" : status) + ", 申请信息:" + (applyInfo == null ? "空" : applyInfo);
	}
}