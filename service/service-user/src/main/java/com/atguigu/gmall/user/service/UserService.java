package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author gao
 * @create 2020-04-29 22:47
 */
public interface UserService {

    //登录方法
    UserInfo login(UserInfo userInfo);
}
