package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author gao
 * @create 2020-05-04 21:32
 */
public interface UserAddressService extends IService<UserAddress> {

    //根据用户id，查询用户的收货地址列表
    List<UserAddress> findUserAddressListByUserId(String userId);

}
