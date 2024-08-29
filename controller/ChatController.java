package com.easychat.controller;


import com.easychat.annotation.GlobalIntercepor;
import com.easychat.entity.config.AppConfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.MessageSendDto;
import com.easychat.entity.dto.TokenUserInfoDto;
import com.easychat.entity.enums.MessageTypeEnum;
import com.easychat.entity.enums.ResponseCodeEnum;
import com.easychat.entity.po.ChatMessage;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.exception.BusinessException;
import com.easychat.service.ChatMessageService;
import com.easychat.service.ChatSessionUserService;
import com.easychat.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.tomcat.util.bcel.classfile.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/chat")
public class ChatController extends ABaseController{

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Resource
    private ChatMessageService chatMessageService;

    @Resource
    private ChatSessionUserService chatSessionUserService;

    @Resource
    private AppConfig appConfig;

    @RequestMapping("/sendMsg")
    @GlobalIntercepor
    public ResponseVO sendMsg(HttpServletRequest request,
                              @NotEmpty String contactId,
                              @NotEmpty @Max(500) String messsageContent,
                              @NotNull Integer messageType,
                              Long fileSize,
                              String fileName,
                              Integer fileType) throws BusinessException {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContactId(contactId);
        chatMessage.setMessageContent(messsageContent);
        chatMessage.setFileSize(fileSize);
        chatMessage.setFileName(fileName);
        chatMessage.setMessageType(messageType);
        chatMessage.setFileType(fileType);
        MessageSendDto messageSendDto = chatMessageService.saveMessage(chatMessage, tokenUserInfoDto);
        return getSuccessResponseVO(messageSendDto);
    }

    @RequestMapping("/uploadFile")
    @GlobalIntercepor
    public ResponseVO uploadFile(HttpServletRequest request,
                                 @NotNull Long messageId,
                                 @NotNull MultipartFile file,
                                 @NotNull MultipartFile cover
                                 ) throws BusinessException {
        TokenUserInfoDto userInfoDto = getTokenUserInfo(request);
        chatMessageService.saveMessageFile(userInfoDto.getUserId(), messageId, file, cover);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/downloadFile")
    @GlobalIntercepor
    public void downloadFile(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @NotEmpty String fileId,
                                   @NotNull Boolean showCover

    ) throws BusinessException {
        TokenUserInfoDto userInfoDto = getTokenUserInfo(request);
        OutputStream out = null;
        FileInputStream in = null;

        try{
            File file = null;
            if(!StringTools.isNumber(fileId)){
                String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
                String avatarPath = appConfig.getProjectFolder() + avatarFolderName + fileId + Constants.IMAGE_SUFFIX;
                if(showCover){
                    avatarPath = avatarPath + Constants.COVER_IMAGE_SUFFIX;
                }
                file = new File(avatarPath);
                if(!file.exists()){
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
            }else{
                file = chatMessageService.downloadFile(userInfoDto, Long.parseLong(fileId),showCover);
            }
            //设置为下载任务
            response.setContentType("application/x-msdownload;charset=UTF-8");
            response.setHeader("Content-Disposition","attachment");
            response.setContentLengthLong(file.length());
            //处理文件
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len;
            while ((len = in.read(byteData))!=-1){
                out.write(byteData, 0, len);
            }
            //关闭流
            out.flush();
        }catch (Exception e){
            logger.error("下载文件失败", e);
        }finally {
            if(out!=null){
                try{
                    out.close();
                }catch (Exception e){
                    logger.error("IO异常", e);
                }
            }
            if(in!=null){
                try{
                    in.close();
                }catch (Exception e){
                    logger.error("IO异常", e);
                }
            }
        }
    }

}
