package com.easychat.controller;


import com.easychat.annotation.GlobalIntercepor;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.po.UserInfo;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.exception.BusinessException;
import com.easychat.redis.RedisComponet;
import com.easychat.redis.RedisUtils;
import com.easychat.service.UserInfoService;
import com.easychat.utils.CopyTools;
import com.easychat.websocket.MessageHandler;
import com.wf.captcha.ArithmeticCaptcha;
import org.apache.tomcat.util.bcel.classfile.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.annotation.Resource;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.executable.ValidateOnExecution;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController("accountController")
@RequestMapping("/account")
//校验
@Validated
public class AccountController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private RedisComponet redisComponet;

    @Resource
    private MessageHandler messageHandler;


    /**
     * 验证码
     */
    @RequestMapping(value = "/checkCode")
    public ResponseVO checkCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        String code = captcha.text();
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setex(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey, code, 60 * 10);
        String checkCodeBase64 = captcha.toBase64();
        Map<String, String> result = new HashMap<>();
        result.put("checkCode", checkCodeBase64);
        logger.info("code:"+code);
        result.put("checkCodeKey", checkCodeKey);
        return getSuccessResponseVO(result);
    }

    @RequestMapping("/register")
    public ResponseVO register(@NotEmpty String checkCodeKey,
                               @NotEmpty @Email String email,
                               @NotEmpty String password,
                               @NotEmpty String nickName,
                               @NotEmpty String checkCode) throws BusinessException {

        try{
            if(!checkCode.equalsIgnoreCase((String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey))){
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.register(email, nickName, password);
            return getSuccessResponseVO(null);

        }finally {
            redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);

        }

    }

    @RequestMapping("/login")
    public ResponseVO login(@NotEmpty String checkCodeKey,
                               @NotEmpty @Email String email,
                               @NotEmpty String password,
                               @NotEmpty String checkCode) throws BusinessException {

        try{
            if(!checkCode.equalsIgnoreCase((String) redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey))){
                throw new BusinessException("图片验证码不正确");
            }
            UserInfoVO userInfoVO  = userInfoService.login(email, password);
            return getSuccessResponseVO(userInfoVO);
        }finally {
            redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
        }
    }

    @GlobalIntercepor
    @RequestMapping("/getSysSetting")
    public ResponseVO getSysSetting(){
        return getSuccessResponseVO(redisComponet.getsysSettingDto());
    }

    @RequestMapping("/test")
    public ResponseVO test(){

        MessageSendDto sendDto = new MessageSendDto();
        sendDto.setMessageContent("现在时间是" + System.currentTimeMillis());
        messageHandler.sendMessage(sendDto);
        return getSuccessResponseVO(null);
    }
}
