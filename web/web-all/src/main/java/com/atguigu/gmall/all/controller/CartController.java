package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

/**
 * @author gao
 * @create 2020-05-04 20:14
 */
@Controller
public class CartController {

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @RequestMapping("addCart.html")
    public String addCart(@RequestParam(name = "skuId") Long skuId,@RequestParam(name = "skuNum") Integer skuNum,HttpServletRequest request){

        cartFeignClient.addToCart(skuId,skuNum);
        //存储skuInfo，skuNum
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        //返回成功添页面
        return "cart/addCart";
    }

    @RequestMapping("cart.html")
    public String cart(){
        return "cart/index";
    }
}
