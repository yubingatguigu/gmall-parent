package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author gao
 * @create 2020-04-21 22:10
 */
@Api(tags = "商品详情数据内部接口")
@RestController
@RequestMapping("api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;

    @GetMapping("inner/getSkuInfo/{skuId}")//inner内部访问接口
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        //根据skuId获取skuInfo信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        return skuInfo;
    }

    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        //根据三级分类id 获取分类信息
        BaseCategoryView categoryViewByCategory3Id = manageService.getCategoryViewByCategory3Id(category3Id);
        return categoryViewByCategory3Id;
    }

    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        //根据skuId查询价格
        BigDecimal skuPrice = manageService.getSkuPrice(skuId);
        return skuPrice;
    }

    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,@PathVariable Long spuId){
        //根据spuId ，skuId 获取销售属性和销售属性值的信息
        return manageService.getselectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        //根据spuId 查询集合属性
       return manageService.getSkuValueIdsMap(spuId);
    }

    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList(){
        List<JSONObject> list = manageService.getBaseCategoryList();
        return Result.ok(list);
    }

    //通过品牌id，查询数据
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){
        return manageService.getTrademarkByTmId(tmId);
    }

    //通过skuId 集合来查询数据
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
         return manageService.getAttrList(skuId);
    }

}
