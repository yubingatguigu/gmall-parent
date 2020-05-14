package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author gao
 * @create 2020-04-20 1:14
 */
@RestController
@RequestMapping("admin/product/")
public class SpuManageController {

    //com/admin/product/baseSaleAttrList
    @Autowired
    private ManageService manageService;

    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){
        //查询所有的销售属性集合
        List<BaseSaleAttr> baseSaleAttrList= manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    //保存
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }
}
