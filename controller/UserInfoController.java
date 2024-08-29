package com.easychat.controller;

import com.easychat.annotation.GlobalIntercepor;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.service.UserInfoService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.StringTools;
import com.easychat.websocket.ChannelContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.IOException;

@RestController
@RequestMapping("/userInfo")
public class UserInfoController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @RequestMapping("/getUserInfo")
    @GlobalIntercepor
    public ResponseVO getUserInfo(HttpServletRequest request){
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo = userInfoService.getUserInfoByUserId(tokenUserInfoDto.getUserId());
        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        userInfoVO.setAdmin(tokenUserInfoDto.getAdmin());

        return getSuccessResponseVO(userInfoVO);
    }

    @RequestMapping("/saveUserInfo")
    @GlobalIntercepor
    public ResponseVO saveUserInfo(HttpServletRequest request,
                                   UserInfo userInfo,
                                   MultipartFile avatarFile,
                                   MultipartFile avatarCover) throws IOException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        userInfo.setUserId(tokenUserInfoDto.getUserId());
        //设置为null，不能从这个界面中修改密码等数据
        userInfo.setPassword(null);
        userInfo.setStatus(null);
        userInfo.setCreateTime(null);
        userInfo.setLastOffTime(null);

        this.userInfoService.updateUserInfo(userInfo, avatarFile, avatarCover);
        //保存后重新获取信息
        return getUserInfo(request);
    }

    @RequestMapping("/updatePassword")
    @GlobalIntercepor
    public ResponseVO updatePassword(HttpServletRequest request,
                                     @NotEmpty @Pattern(regexp = Constants.REGEX_PASSWORD) String password) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);

        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMD5(password));
        this.userInfoService.updateUserInfoByUserId(userInfo, tokenUserInfoDto.getUserId());
        //强制退出，重新登录
        channelContextUtils.closeContext(tokenUserInfoDto.getUserId());
        //保存后重新获取信息
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/logout")
    @GlobalIntercepor
    public ResponseVO logout(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        //退出登录 关闭ws连接
        channelContextUtils.closeContext(tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }



}
