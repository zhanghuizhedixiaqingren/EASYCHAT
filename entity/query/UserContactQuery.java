package com.easychat.entity.query;



/**
  * @Desoription:联系人查询对象
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
public class UserContactQuery extends BaseQuery{
	/**
	  * 用户ID
	  */
	private String userId;

	private String userIdFuzzy;
	/**
	  * 联系人ID或者群组ID
	  */
	private String contactId;

	private String contactIdFuzzy;
	/**
	  * 联系人类型 0：好友 1：群组
	  */
	private Integer contactType;

	/**
	  * 创建时间
	  */
	private java.util.Date createTime;

	private String createTimeStart;
	private String createTimeEnd;
	/**
	  * 状态 0：非好友 1：好友 2：已删除好友 3：被好友删除 4：已拉黑好友 5：被好友拉黑
	  */
	private Integer status;

	private boolean queryUserInfo;

	private boolean queryGroupInfo;

	private boolean excludeMyGroup;

	private Integer[] statusArray;

	public Integer[] getStatusArray() {
		return statusArray;
	}

	public void setStatusArray(Integer[] statusArray) {
		this.statusArray = statusArray;
	}

	public boolean isExcludeMyGroup() {
		return excludeMyGroup;
	}

	public void setExcludeMyGroup(boolean excludeMyGroup) {
		this.excludeMyGroup = excludeMyGroup;
	}

	public boolean isQueryGroupInfo() {
		return queryGroupInfo;
	}

	public boolean queryContactUserInfo;

	public boolean isQueryContactUserInfo() {
		return queryContactUserInfo;
	}

	public void setQueryContactUserInfo(boolean queryContactUserInfo) {
		this.queryContactUserInfo = queryContactUserInfo;
	}

	public void setQueryGroupInfo(boolean queryGroupInfo) {
		this.queryGroupInfo = queryGroupInfo;
	}

	public boolean isQueryUserInfo() {
		return queryUserInfo;
	}

	public void setQueryUserInfo(boolean queryUserInfo) {
		this.queryUserInfo = queryUserInfo;
	}

	/**
	  * 最后更新时间
	  */
	private java.util.Date lastUpdateTime;

	private String lastUpdateTimeStart;
	private String lastUpdateTimeEnd;
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		 return this.userId;
	}

	public void setContactId(String contactId) {
		this.contactId = contactId;
	}

	public String getContactId() {
		 return this.contactId;
	}

	public void setContactType(Integer contactType) {
		this.contactType = contactType;
	}

	public Integer getContactType() {
		 return this.contactType;
	}

	public void setCreateTime(java.util.Date createTime) {
		this.createTime = createTime;
	}

	public java.util.Date getCreateTime() {
		 return this.createTime;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getStatus() {
		 return this.status;
	}

	public void setLastUpdateTime(java.util.Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	public java.util.Date getLastUpdateTime() {
		 return this.lastUpdateTime;
	}

	public void setUserIdFuzzy(String userIdFuzzy) {
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getUserIdFuzzy() {
		 return this.userIdFuzzy;
	}

	public void setContactIdFuzzy(String contactIdFuzzy) {
		this.contactIdFuzzy = contactIdFuzzy;
	}

	public String getContactIdFuzzy() {
		 return this.contactIdFuzzy;
	}

	public void setCreateTimeStart(String createTimeStart) {
		this.createTimeStart = createTimeStart;
	}

	public String getCreateTimeStart() {
		 return this.createTimeStart;
	}

	public void setCreateTimeEnd(String createTimeEnd) {
		this.createTimeEnd = createTimeEnd;
	}

	public String getCreateTimeEnd() {
		 return this.createTimeEnd;
	}

	public void setLastUpdateTimeStart(String lastUpdateTimeStart) {
		this.lastUpdateTimeStart = lastUpdateTimeStart;
	}

	public String getLastUpdateTimeStart() {
		 return this.lastUpdateTimeStart;
	}

	public void setLastUpdateTimeEnd(String lastUpdateTimeEnd) {
		this.lastUpdateTimeEnd = lastUpdateTimeEnd;
	}

	public String getLastUpdateTimeEnd() {
		 return this.lastUpdateTimeEnd;
	}

}