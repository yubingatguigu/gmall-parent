package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author gao
 * @create 2020-04-29 23:01
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    //登录 只做数据保存，不返回页面
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo){
        UserInfo info = userService.login(userInfo);
        if(null!=info){//用户存在数据库中，登录之后的信息放入缓存中，是每个模块都可以访问到用户信息
            //声明HashMap集合记录相关数据
            HashMap<String, Object> hashMap = new HashMap<>();
            //用户昵称，记录map中
            hashMap.put("nickName",info.getNickName());
            //声明token token是一个uuid的字符串
            String token = UUID.randomUUID().toString().replaceAll("-", "");
            //定义key = user:login:token value = userId
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            redisTemplate.opsForValue().set(userKey,info.getId().toString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            //记录token
            hashMap.put("token",token);
            return Result.ok(hashMap);
        }else{
            return Result.fail().message("用户名或密码错误");
        }
    }

    //退出登录
    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        //登录成功之后，token放入了cookie和header中,缓存中存储时需要token，删除时也需要token组成key
        //从header获取token
        String token = request.getHeader("token");
        //删除缓存中的数据
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX+token);
        return Result.ok();
    }
}
