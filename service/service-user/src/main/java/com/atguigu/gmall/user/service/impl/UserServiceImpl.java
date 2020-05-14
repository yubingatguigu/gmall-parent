package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author gao
 * @create 2020-04-29 22:48
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {

        //获取密码
        String passwd = userInfo.getPasswd();
        //密码加密
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name",userInfo.getLoginName()).eq("passwd",newPwd);
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        if(null!=info){
            //说明数据库有当前用户
            return info;
        }
        //数据库没有当前用户
        return null;
    }
}
