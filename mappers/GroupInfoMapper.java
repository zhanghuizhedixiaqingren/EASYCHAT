package com.easychat.mappers;

import com.easychat.entity.po.GroupInfo;
import org.apache.ibatis.annotations.Param;

/**
  * @Desoription:Mapper
  * 
  * @author:enjoying
  * @data:2024/08/07
  */
public interface GroupInfoMapper<T, P> extends BaseMapper {
	/**
	  * 根据GroupId查询
	  */
	T selectByGroupId(@Param("groupId") String groupId);

	/**
	  * 根据GroupId更新
	  */
	Integer updateByGroupId(@Param("bean") T t, @Param("groupId") String groupId);

	/**
	  * 根据GroupId删除
	  */
	Integer deleteByGroupId(@Param("groupId") String groupId);


}