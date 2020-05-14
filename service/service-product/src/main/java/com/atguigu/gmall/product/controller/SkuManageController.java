package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author gao
 * @create 2020-04-20 20:47
 */
@RestController
@RequestMapping("/admin/product/")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    //http://api.gmall.com/admin/product/spuImageList/5  查询图片列表
    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId){
        //根据spuId查询spuImageList
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }

    //加载动态销售属性
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId){
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }

    //保存
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    //分页查询 库存单元表
    @GetMapping("/list/{page}/{size}")
    public Result skuInfoList(@PathVariable Long page,@PathVariable Long size){
        Page<SkuInfo> skuInfoParam = new Page<>(page, size);
        IPage<SkuInfo> pageParam =manageService.selectPage(skuInfoParam);
        return Result.ok(pageParam);
    }

    //上架  http://api.gmall.com/admin/product/onSale/14
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        manageService.onSale(skuId);
        return Result.ok();
    }

    //下架 http://api.gmall.com/admin/product/cancelSale/13
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}
