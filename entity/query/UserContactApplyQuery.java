package com.easychat.entity.query;



/**
  * @Desoription:联系人申请查询对象
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
public class UserContactApplyQuery extends BaseQuery{
	/**
	  * 自增ID
	  */
	private Integer applyId;

	/**
	  * 申请人ID
	  */
	private String applyUserId;

	private String applyUserIdFuzzy;
	/**
	  * 接收人ID
	  */
	private String receiveUserId;

	private String receiveUserIdFuzzy;
	/**
	  * 联系人类型 0：好友 1：群组
	  */
	private Integer contactType;

	/**
	  * 如果是群组，则为群组ID；否则可以为NULL
	  */
	private String contactId;

	private String contactIdFuzzy;
	/**
	  * 最后申请时间
	  */
	private Long lastApplyTime;

	private Long lastApplyTimestamp;

	public Long getLastApplyTimestamp() {
		return lastApplyTimestamp;
	}

	public void setLastApplyTimestamp(Long lastApplyTimestamp) {
		this.lastApplyTimestamp = lastApplyTimestamp;
	}

	/**
	  * 状态 0：待处理 1：已同意 2：已拒绝 3：已拉黑
	  */
	private Integer status;

	/**
	  * 申请信息
	  */
	private String applyInfo;

	private Boolean queryContactInfo;

	public Boolean getQueryContactInfo() {
		return queryContactInfo;
	}

	public void setQueryContactInfo(Boolean queryContactInfo) {
		this.queryContactInfo = queryContactInfo;
	}

	private String applyInfoFuzzy;
	public void setApplyId(Integer applyId) {
		this.applyId = applyId;
	}

	public Integer getApplyId() {
		 return this.applyId;
	}

	public void setApplyUserId(String applyUserId) {
		this.applyUserId = applyUserId;
	}

	public String getApplyUserId() {
		 return this.applyUserId;
	}

	public void setReceiveUserId(String receiveUserId) {
		this.receiveUserId = receiveUserId;
	}

	public String getReceiveUserId() {
		 return this.receiveUserId;
	}

	public void setContactType(Integer contactType) {
		this.contactType = contactType;
	}

	public Integer getContactType() {
		 return this.contactType;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getContactId() {
		 return this.contactId;
	}

	public void setLastApplyTime(Long lastApplyTime) {
		this.lastApplyTime = lastApplyTime;
	}

	public Long getLastApplyTime() {
		 return this.lastApplyTime;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getStatus() {
		 return this.status;
	}

	public void setApplyInfo(String applyInfo) {
		this.applyInfo = applyInfo;
	}

	public String getApplyInfo() {
		 return this.applyInfo;
	}

	public void setApplyUserIdFuzzy(String applyUserIdFuzzy) {
		this.applyUserIdFuzzy = applyUserIdFuzzy;
	}

	public String getApplyUserIdFuzzy() {
		 return this.applyUserIdFuzzy;
	}

	public void setReceiveUserIdFuzzy(String receiveUserIdFuzzy) {
		this.receiveUserIdFuzzy = receiveUserIdFuzzy;
	}

	public String getReceiveUserIdFuzzy() {
		 return this.receiveUserIdFuzzy;
	}

	public void setContactIdFuzzy(String contactIdFuzzy) {
		this.contactIdFuzzy = contactIdFuzzy;
	}

	public String getContactIdFuzzy() {
		 return this.contactIdFuzzy;
	}

	public void setApplyInfoFuzzy(String applyInfoFuzzy) {
		this.applyInfoFuzzy = applyInfoFuzzy;
	}

	public String getApplyInfoFuzzy() {
		 return this.applyInfoFuzzy;
	}

}