package com.app.mdc.serviceImpl.system;


import com.app.mdc.config.mail.MailProperties;
import com.app.mdc.exception.BusinessException;
import com.app.mdc.mapper.system.VerificationCodeMapper;
import com.app.mdc.model.system.User;
import com.app.mdc.model.system.VerificationCode;
import com.app.mdc.service.system.UserService;
import com.app.mdc.service.system.VerificationCodeService;
import com.app.mdc.utils.httpclient.HttpUtil;
import com.app.mdc.utils.verification.RandomValidateCodeUtil;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;

@Service
public class VerificationCodeServiceImpl extends ServiceImpl<VerificationCodeMapper, VerificationCode> implements VerificationCodeService {

    @Autowired
    private UserService userService;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private MailProperties mailProperties;

    @Override
    public Integer getVerificationCode(Integer userId) throws BusinessException {
        //获取当前用户
        User user = userService.selectById(userId);
        if(user == null){
            throw new BusinessException("用户不存在");
        }

        //生成验证码入库
        String randcode = RandomValidateCodeUtil.getRandcode();
        VerificationCode verificationCode = new VerificationCode(randcode, user.getUserName(), new Date());
        this.baseMapper.insert(verificationCode);

        //推送验证码给用户邮箱
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getUsername());
        message.setTo(user.getEmail()); // 接收地址,可传入数组进行群发
        message.setSubject("请妥善保管好您的验证码"); // 标题
        String content = "尊敬的用户您的验证码是"+ randcode +"请不要把验证码泄漏给其他人,如非本人请勿操作。";
        message.setText(content); // 内容
        javaMailSender.send(message);
        return verificationCode.getId();
    }

    @Override
    public Integer getEmailVerificationCode(String email) throws BusinessException {
        //生成验证码入库
        String randcode = RandomValidateCodeUtil.getRandcode();
        VerificationCode verificationCode = new VerificationCode(randcode, "游客", new Date());
        this.baseMapper.insert(verificationCode);

        //推送验证码给用户邮箱
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getUsername());
        message.setTo(email); // 接收地址,可传入数组进行群发
        message.setSubject("请妥善保管好您的验证码"); // 标题
        message.setText("尊敬的用户您好,您的验证码为["+ randcode +"]"); // 内容
        javaMailSender.send(message);
        return verificationCode.getId();
    }

    @Override
    public Integer getPhoneVerificationCode(String phone) throws BusinessException {
        //生成验证码入库
        String randcode = RandomValidateCodeUtil.getRandcode();
        VerificationCode verificationCode = new VerificationCode(randcode, "游客", new Date());
        this.baseMapper.insert(verificationCode);

        //推送验证码
        String content = "尊敬的用户您的验证码是"+ randcode +"请不要把验证码泄漏给其他人,如非本人请勿操作。";
        String url = "https://mb345.com/ws/BatchSend2.aspx?CorpID=XALKJ0006852&Pwd=zh9527@&Mobile="+ phone +"&Content=" + content;
        try{
            HttpUtil.doGet(url,new HashMap<>());
        }
        catch (Exception e){
            throw  new BusinessException("验证码获取失败");
        }
        return verificationCode.getId();
    }


    @Override
    public boolean validateVerCode(String verCode, String verId) {
        VerificationCode verificationCode = this.baseMapper.selectById(verId);
        if(verificationCode == null){
            return false;
        }
        return verCode.equals(verificationCode.getCode());
    }

}
