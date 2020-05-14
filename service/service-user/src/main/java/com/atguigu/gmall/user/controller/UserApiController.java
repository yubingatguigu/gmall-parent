package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author gao
 * @create 2020-05-04 21:39
 */
@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Autowired
    private UserAddressService userAddressService;

    //根据用户id，查询用户收货地址列表
    @GetMapping("inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable String userId){
        return userAddressService.findUserAddressListByUserId(userId);
    }

    //编辑
    @GetMapping("inner/updateUserAddressById/{Id}")
    public Result updateUserAddressById(@PathVariable Long Id){
        UserAddress userAddress = new UserAddress();
        userAddress.setId(Id);
        userAddressService.updateById(userAddress);
        return Result.ok();
    }

    //删除
    @DeleteMapping("inner/removeUserAddressById/{Id}")
    public Result removeUserAddressById(@PathVariable Long Id){
        userAddressService.removeById(Id);
        return Result.ok();
    }
}
