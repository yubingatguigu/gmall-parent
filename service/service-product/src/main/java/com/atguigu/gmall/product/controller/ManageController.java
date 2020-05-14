package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author gao
 * @create 2020-04-17 22:06
 */
@Api(tags = "商品基础属性接口")
@RestController
@RequestMapping("admin/product")
public class ManageController {

    @Autowired
    private ManageService manageService;

    //查询所有一级分类信息
    @GetMapping("getCategory1")
    public Result getCategory1(){
        List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        return Result.ok(baseCategory1List);
    }

    //根据一级分类id查询二级分类信息
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    //根据二级分类id查询三级分类信息
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    //根据分类id获取平台属性数据
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result attrInfoList(@PathVariable Long category1Id,@PathVariable Long category2Id,@PathVariable Long category3Id){
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(baseAttrInfoList);
    }

    //接收数据应该是BaseAttrInfo中的每个属性组成的json字符串
    //保存数据,后台系统页面vue制作，vue保存的时候传过来的是json字符串
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){

        // 前台数据都被封装到该对象中baseAttrInfo
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    //根据平台属性id，查询平台属性值集合 修改平台属性
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){

        //先查询平台属性对象
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        //根据平台属性id获取平台属性值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(attrValueList);
    }

    //http://api.gmall.com/admin/product/1/10?category3Id=2 根据三级列表 查询商品列表 带分页
    @GetMapping("{page}/{size}")
    public Result index(@PathVariable Long page ,@PathVariable Long size,SpuInfo spuInfo){

        //返回结果集，传到前台
        Page<SpuInfo> pageParam = new Page<>(page,size);
        IPage<SpuInfo> spuInfoIPage = manageService.selectPage(pageParam, spuInfo);
        return  Result.ok(spuInfoIPage);
    }

}
