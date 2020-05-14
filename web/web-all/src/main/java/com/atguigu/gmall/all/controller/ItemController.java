package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * @author gao
 * @create 2020-04-23 14:42
 */
@Controller
public class ItemController {

    //通过feign远程调用service_item
    @Autowired
    private ItemFeignClient itemFeignClient;

    //用户怎样访问到详情 https://item.jd.com/100005304631.html 前面是域名，后面是{skuId}.html 控制器的地址
    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model){
        //通过feign远程调用获取商品详情，用户调用itemFeignClient.getItem()方法，本质就是调用ItemApiController.getItem()方法
        Result<Map> result = itemFeignClient.getItem(skuId);
        //将商品详情页面保存在后台
        model.addAllAttributes(result.getData());
        //返回商品详情页面
        return "item/index";
    }

}
