package com.xuecheng.ucenter.service;

import com.xuecheng.ucenter.model.po.XcUser;

public interface WxAuthService {

    public XcUser wxAuth(String code);
}
