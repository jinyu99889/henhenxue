package com.hengxue.auth.application.service;

import com.hengxue.auth.interfaces.rest.response.LoginResponse;

/** 登录用例的应用服务端口。 */
public interface LoginService {

    /**
     * 校验账号密码并创建 Sa-Token 会话。
     *
     * @param account 用户名或邮箱
     * @param password 明文密码
     * @param sourceIp 请求来源 IP
     * @return 会话令牌和用户摘要
     */
    LoginResponse login(String account, String password, String sourceIp);

    /**
     * 注销当前请求对应的设备会话。
     */
    void logout();
}
