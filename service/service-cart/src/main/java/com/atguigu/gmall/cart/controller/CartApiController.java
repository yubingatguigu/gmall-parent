package com.atguigu.gmall.cart.controller;

import brave.http.HttpServerRequest;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import javassist.compiler.ast.StringL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author gao
 * @create 2020-05-03 10:07
 */
@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum, HttpServletRequest request){

        //获取登录时用户id
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            //未登录的临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        //添加到购物车
        cartService.addToCart(skuId,userId,skuNum);
        return Result.ok();
    }

    //获取购物车列表
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //获取临时用户id
        String userTempId = AuthContextHolder.getUserTempId(request);
        //返回购物车的集合
        List<CartInfo> cartList = cartService.getCartList(userId, userTempId);
        return Result.ok(cartList);
    }

    //更改选中状态
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId, @PathVariable Integer isChecked,HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            //未登录,获取临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(userId,isChecked,skuId);
        return Result.ok();
    }

    //删除购物车数据
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,HttpServletRequest request){
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        if(StringUtils.isEmpty(userId)){
            //获取临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(skuId,userId);
        return Result.ok();
    }

    //从购物车中获取送货清单
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }
}
