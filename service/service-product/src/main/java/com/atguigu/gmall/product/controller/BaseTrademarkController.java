package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author gao
 * @create 2020-04-19 23:35
 */
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //http://api.gmall.com/admin/product/baseTrademark/1/10  分页查询商品
    @GetMapping("{page}/{size}")
    public Result index(@PathVariable Long page,@PathVariable Long size){
        //商品品牌的分页查询
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page,size);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.selectPage(baseTrademarkPage);
        return Result.ok(baseTrademarkIPage);
    }

    //根据id获取商品品牌
    @GetMapping("get/{id}")
    public Result get(@PathVariable String id){
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    //保存商品品牌
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    //修改商品
    @PutMapping("update")
    public Result updateById(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }
    //删除商品品牌
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    //查询商品品牌
    @GetMapping("getTrademarkList")
    public Result getTrademarkList(){
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.getTrademarkList();
        return Result.ok(baseTrademarkList);
    }

}
