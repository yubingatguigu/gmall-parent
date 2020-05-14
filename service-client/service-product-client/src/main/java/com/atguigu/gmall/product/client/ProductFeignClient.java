package com.atguigu.gmall.product.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author gao
 * @create 2020-04-23 13:12
 */
@FeignClient(value = "service-product" , fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {

    //根据skuId获取sku信息
    @GetMapping("api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable Long skuId);

    //通过三级分类id查询分类信息
    @GetMapping("api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable Long category3Id);

    //获取sku最新价格
    @GetMapping("api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable Long skuId);

    //根据spuId，skuId 查询销售属性集合
    @GetMapping("api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId, @PathVariable Long spuId);

    //根据spuId 查询map 集合属性
    @GetMapping("api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable Long spuId);

    //获取全部分类信息
    @GetMapping("api/product/getBaseCategoryList")
    public Result getBaseCategoryList();

    //根据品牌id，查询数据
    @GetMapping("api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademark(@PathVariable Long tmId);

    //根据sku集合查询数据
    @GetMapping("api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable Long skuId);

}
