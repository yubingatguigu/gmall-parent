package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author gao
 * @create 2020-05-03 9:26
 */
public interface CartService {

    // 添加购物车 用户Id，商品Id，商品数量。
    void addToCart(Long skuId, String userId, Integer skuNum);

    //通过用户id查询购物车列表
    List<CartInfo> getCartList(String userId,String userTempId);

    //更改选中状态
    void checkCart(String userId,Integer isChecked,Long skuId);

    //删除购物车数据
    void deleteCart(Long skuId, String userId);

    //根据用户id，查询购物车列表
    List<CartInfo> getCartCheckedList(String userId);

    //根据用户id，加载购物车数据并放入缓存
    List<CartInfo> loadCartCache(String userId);
}
