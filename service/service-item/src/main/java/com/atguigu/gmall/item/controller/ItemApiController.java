package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author gao
 * @create 2020-04-21 21:51
 */
@RestController
@RequestMapping("api/admin")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    @GetMapping("{skuId}")
    public Result getItem(@PathVariable Long skuId){
        //根据skuId 获取商品详情信息
        Map<String, Object> result = itemService.getBySkuId(skuId);
        return Result.ok(result);
    }

}
