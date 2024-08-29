package com.easychat.controller;

import com.easychat.annotation.GlobalIntercepor;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.GroupInfo;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.query.GroupInfoQuery;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.GroupInfoVO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.GroupInfoMapper;
import com.easychat.service.GroupInfoService;
import com.easychat.service.UserContactApplyService;
import com.easychat.service.UserContactService;
import com.easychat.service.impl.UserContactApplyServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@RestController("groupInfoController")
@RequestMapping("/group")
public class GroupInfoController extends ABaseController {

    @Resource
    private GroupInfoService groupInfoService;

    @Resource
    private UserContactService userContactService;


    /**
     * 创建我的群组
     * */
    @RequestMapping(value = "/saveGroup")
    @GlobalIntercepor
    public ResponseVO saveGroup(HttpServletRequest request,
                                String groupId,
                                @NotEmpty String groupName,
                                String groupNotice,
                                @NotNull Integer joinType,
                                MultipartFile avatarFile,
                                MultipartFile avatarCover) throws BusinessException, IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupId(groupId);
        groupInfo.setGroupOwnerId(tokenUserInfoDto.getUserId());
        groupInfo.setGroupName(groupName);
        groupInfo.setGroupNotice(groupNotice);
        groupInfo.setJoinType(joinType);
        this.groupInfoService.saveGroup(groupInfo, avatarFile, avatarCover);
        return getSuccessResponseVO(null);
    }

    @RequestMapping(value = "/loadMyGroup")
    @GlobalIntercepor
    public ResponseVO loadMyGroup(HttpServletRequest request){
        TokenUserInfoDto userInfoDto = getTokenUserInfo(request);

        GroupInfoQuery groupInfoQuery = new GroupInfoQuery();
        groupInfoQuery.setGroupOwnerId(userInfoDto.getUserId());
        groupInfoQuery.setOrderBy("create_time desc");
        List<GroupInfo> groupInfoList = this.groupInfoService.findListByParam(groupInfoQuery);

        return getSuccessResponseVO(groupInfoList);
    }

    @RequestMapping(value = "/getGroupInfo")
    @GlobalIntercepor
    public ResponseVO getGroupInfo(HttpServletRequest request,
                                   @NotEmpty String groupId) throws BusinessException {

        GroupInfo groupInfo = getGroupDetailCommon(request, groupId);
        UserContactQuery userContactQuery = new UserContactQuery();
        userContactQuery.setContactId(groupId);
        //成员数
        Integer memberCount = this.userContactService.findCountByParam(userContactQuery);
        groupInfo.setMemberCount(memberCount);
        return getSuccessResponseVO(groupInfo);
    }

    private GroupInfo getGroupDetailCommon(HttpServletRequest request, String groupId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);

        UserContact userContact = this.userContactService.getUserContactByUserIdAndContactId(tokenUserInfoDto.getUserId(), groupId);
        if(null == userContact || !UserContactStatusEnum.FRIEND.getStatus().equals(userContact.getStatus())){
            throw new BusinessException("你不在群聊或者群聊不存在或者已经解散");
        }
        GroupInfo groupInfo = this.groupInfoService.getGroupInfoByGroupId(groupId);
        if(null == groupInfo || !GroupStatusEum.NORMAL.getStatus().equals(groupInfo.getStatus())){
            throw new BusinessException("群聊不存在或已解散");
        }
        return groupInfo;
    }

    @RequestMapping(value = "/getGroupInfo4Chat")
    @GlobalIntercepor
    public ResponseVO getGroupInfo4Chat(HttpServletRequest request,
                                   @NotEmpty String groupId) throws BusinessException {

        GroupInfo groupInfo = getGroupDetailCommon(request, groupId);

        UserContactQuery userContactQuery = new UserContactQuery();
        userContactQuery.setContactId(groupId);
        //关联查询
        userContactQuery.setQueryUserInfo(true);
        //根据加入的顺序查询
        userContactQuery.setOrderBy("create_time asc");
        userContactQuery.setStatus(UserContactStatusEnum.FRIEND.getStatus());
        List<UserContact> userContactList = this.userContactService.findListByParam(userContactQuery);
        GroupInfoVO groupInfoVo = new GroupInfoVO();
        groupInfoVo.setGroupInfo(groupInfo);
        groupInfoVo.setUserContactList(userContactList);
        return getSuccessResponseVO(groupInfoVo);
    }

    /**
     * 退群
     *
     * @param request
     * @param groupId
     * @return
     */
    @RequestMapping(value = "/leaveGroup")
    @GlobalIntercepor
    public ResponseVO leaveGroup(HttpServletRequest request, @NotEmpty String groupId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        groupInfoService.leaveGroup(tokenUserInfoDto.getUserId(), groupId, MessageTypeEnum.LEAVE_GROUP);
        return getSuccessResponseVO(null);
    }

    /**
     * 解散群
     *
     * @param request
     * @param groupId
     * @return
     */
    @RequestMapping(value = "/dissolutionGroup")
    @GlobalIntercepor
    public ResponseVO dissolutionGroup(HttpServletRequest request, @NotEmpty String groupId) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        groupInfoService.dissolutionGroup(tokenUserInfoDto.getUserId(), groupId);
        return getSuccessResponseVO(null);
    }

    /**
     * 添加或者移除人员
     *
     * @param request
     * @param groupId
     * @param selectContacts
     * @param opType
     * @return
     */
    @RequestMapping(value = "/addOrRemoveGroupUser")
    @GlobalIntercepor
    public ResponseVO addOrRemoveGroupUser(HttpServletRequest request, @NotEmpty String groupId, @NotEmpty String selectContacts,
                                           @NotNull Integer opType) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        groupInfoService.addOrRemoveGroupUser(tokenUserInfoDto, groupId, selectContacts, opType);
        return getSuccessResponseVO(null);
    }



}
