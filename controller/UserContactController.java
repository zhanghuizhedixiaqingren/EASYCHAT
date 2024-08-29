package com.easychat.controller;


import com.easychat.annotation.GlobalIntercepor;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.dto.UserContactSearchResultDto;
import com.easychat.entity.enums.PageSize;
import com.easychat.entity.enums.ResponseCodeEnum;
import com.easychat.entity.enums.UserContactStatusEnum;
import com.easychat.entity.enums.UserContactTypeEnum;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.po.UserContactApply;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.query.UserContactApplyQuery;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.exception.BusinessException;
import com.easychat.service.UserContactApplyService;
import com.easychat.service.UserContactService;
import com.easychat.service.UserInfoService;
import com.easychat.utils.CopyTools;
import jodd.util.ArraysUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/contact")
public class UserContactController extends ABaseController{

    @Resource
    private UserContactService userContactService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserContactApplyService userContactApplyService;


    /**
     *搜索好友或者群聊
     * */
    @RequestMapping("/search")
    @GlobalIntercepor
    public ResponseVO search(HttpServletRequest request,
                             @NotEmpty String contactId){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactSearchResultDto resultDto = userContactService.searchContact(tokenUserInfoDto.getUserId(), contactId);

        return getSuccessResponseVO(resultDto);
    }


    /**
     * 添加用户
     * */
    @RequestMapping("/applyAdd")
    @GlobalIntercepor
    public ResponseVO applyAdd(HttpServletRequest request,
                             @NotEmpty String contactId, String applyInfo) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        Integer joinType = this.userContactService.applyAdd(tokenUserInfoDto, contactId, applyInfo);

        return getSuccessResponseVO(joinType);
    }

    /**
     * 获取好友申请列表
     * */
    @RequestMapping("/loadApply")
    @GlobalIntercepor
    public ResponseVO loadApply(HttpServletRequest request, Integer pageNo) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);

        UserContactApplyQuery applyQuery = new UserContactApplyQuery();
        applyQuery.setOrderBy("last_apply_time desc");
        applyQuery.setReceiveUserId(tokenUserInfoDto.getUserId());
        applyQuery.setPageNo(pageNo);
        applyQuery.setPageSize(PageSize.SIZE15.getSize());
        applyQuery.setQueryContactInfo(true);
        //关联查询
        PaginationResultVO resultVO = userContactApplyService.findListByPage(applyQuery);

        return getSuccessResponseVO(resultVO);
    }

    /**
     * 处理联系人信息
     * */
    @RequestMapping("/dealWithApply")
    @GlobalIntercepor
    public ResponseVO dealWithApply(HttpServletRequest request,
                                    @NotNull Integer applyId,
                                    @NotNull Integer status) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        this.userContactApplyService.dealWidthApply(tokenUserInfoDto.getUserId(), applyId, status);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取联系人列表
     * */
    @RequestMapping("/loadContact")
    @GlobalIntercepor
    public ResponseVO loadContact(HttpServletRequest request,
                                    @NotNull String contactType) throws BusinessException {
        UserContactTypeEnum contactTypeEnum = UserContactTypeEnum.getByName(contactType);
        if(null == contactTypeEnum){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserContactQuery contactQuery = new UserContactQuery();
        contactQuery.setUserId(tokenUserInfoDto.getUserId());
        contactQuery.setContactType(contactTypeEnum.getType());
        if (contactTypeEnum.USER == contactTypeEnum){
            contactQuery.setQueryUserInfo(true);
        }else if(contactTypeEnum.GROUP == contactTypeEnum){
            contactQuery.setQueryGroupInfo(true);
            contactQuery.setExcludeMyGroup(true);
        }
        contactQuery.setOrderBy("last_update_time desc");
        //能被查询出来的状态,朋友，被删除，被拉黑 !理解过程，如何加mybatis
        contactQuery.setStatusArray(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE.getStatus(),
        });
        List<UserContact> contactList = userContactService.findListByParam(contactQuery);

        return getSuccessResponseVO(contactList);
    }


    /**
     * 获取联系人信息，不一定是好友
     *  */
    @RequestMapping("/getContactInfo")
    @GlobalIntercepor
    public ResponseVO getContactInfo(HttpServletRequest request, @NotEmpty String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(contactId);
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        userInfoVO.setContactStatus(UserContactStatusEnum.NOT_FRIEND.getStatus());
        //判断是否是联系人
        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), contactId);
        if (userContact != null) {
            userInfoVO.setContactStatus(userContact.getStatus());
        }
        return getSuccessResponseVO(userInfoVO);
    }


    /**
     * 获取联系人信息，一定是好友
     *
     *  */
    @RequestMapping("/getContactUserInfo")
    @GlobalIntercepor
    public ResponseVO getContactUserInfo(HttpServletRequest request, String contactId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        //获取联系人

        UserContact userContact = userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), contactId);
        if(null == userContact || ArraysUtil.contains(new Integer[]{
                UserContactStatusEnum.FRIEND.getStatus(),
                UserContactStatusEnum.DEL_BE.getStatus(),
                UserContactStatusEnum.BLACKLIST_BE.getStatus(),
                }, userContact.getStatus())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        UserInfo userInfo = userInfoService.getUserInfoByUserId(tokenUserInfoDto.getUserId());
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);

        return getSuccessResponseVO(userInfoVO);
    }


    /**
     * 删除联系人
     * */
    @RequestMapping("/delContact")
    @GlobalIntercepor
    public ResponseVO delContact(HttpServletRequest request, String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userContactService.removeUserContact(tokenUserInfoDto.getUserId(), contactId, UserContactStatusEnum.DEL);
        return getSuccessResponseVO(null);
    }

    /**
     * 拉黑联系人
     * */
    @RequestMapping("/addContact2BlackList")
    @GlobalIntercepor
    public ResponseVO addContact2BlackList(HttpServletRequest request, String contactId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userContactService.removeUserContact(tokenUserInfoDto.getUserId(), contactId, UserContactStatusEnum.BLACKLIST);
        return getSuccessResponseVO(null);
    }

}
