package com.easychat.mappers;

import org.apache.ibatis.annotations.Param;

/**
  * @Desoription:会话信息Mapper
  * 
  * @author:enjoying
  * @data:2024/08/14
  */
public interface ChatSessionMapper<T, P> extends BaseMapper {
	/**
	  * 根据SessionId查询
	  */
	T selectBySessionId(@Param("sessionId") String sessionId);

	/**
	  * 根据SessionId更新
	  */
	Integer updateBySessionId(@Param("bean") T t, @Param("sessionId") String sessionId);

	/**
	  * 根据SessionId删除
	  */
	Integer deleteBySessionId(@Param("sessionId") String sessionId);

}