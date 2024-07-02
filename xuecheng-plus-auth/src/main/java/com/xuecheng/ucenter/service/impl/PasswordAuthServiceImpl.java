package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //校验验证码
        String checkcode = authParamsDto.getCheckcode();
        String checkcodekey = authParamsDto.getCheckcodekey();

        if(StringUtils.isBlank(checkcodekey) || StringUtils.isBlank(checkcode)){
            throw new RuntimeException("验证码为空");

        }
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if(!verify || verify == null){
            throw new RuntimeException("验证码输入错误");
        }

        //账号
        String username = authParamsDto.getUsername();
        //账号是否存在
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>()
                                             .eq(XcUser::getUsername, username));
        if (xcUser==null){
            throw new RuntimeException("站好不存在");
        }

        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);

        //取出数据库存储的正确密码
        String passwordDb = xcUser.getPassword();
        //用户输入的密码
        String passwordForm = authParamsDto.getPassword();
        //校验密码
        boolean matches = passwordEncoder.matches(passwordForm, passwordDb);
        if(!matches){
            throw new RuntimeException("账号或密码错误");
        }
        return xcUserExt;
    }
}
