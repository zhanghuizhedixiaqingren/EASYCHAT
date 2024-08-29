package com.easychat.controller;

import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.enums.ResponseCodeEnum;

import com.easychat.entity.vo.ResponseVO;

import com.easychat.exception.BusinessException;
import com.easychat.redis.RedisUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

public class ABaseController {

    protected static final String STATUC_SUCCESS = "success";
    protected static final String STATUC_ERROR = "error";

    @Resource
    private RedisUtils redisUtils;

    protected <T> ResponseVO<T> getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO responseVO = new ResponseVO();
        responseVO.setStatus(STATUC_ERROR);
        if (e.getCode() == null){
            responseVO.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            responseVO.setCode(e.getCode());
        }
        responseVO.setInfo(e.getMessage());
        responseVO.setData(t);
        return responseVO;
    }

    protected <T> ResponseVO getServerErrorResponseVO(T t) {
        ResponseVO responseVO = new ResponseVO();
        responseVO.setStatus(STATUC_ERROR);
        responseVO.setCode(ResponseCodeEnum.CODE_500.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected TokenUserInfoDto getTokenUserInfo(HttpServletRequest request){
        String token = request.getHeader("token");
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
        return tokenUserInfoDto;
    }
}
