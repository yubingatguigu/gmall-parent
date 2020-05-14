package com.atguigu.gmall.item.service.Impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author gao
 * @create 2020-04-21 21:49
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {

        Map<String,Object> map = new HashMap<>();
        //通过skuId获取skuInfo信息
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            return skuInfo;
        },threadPoolExecutor);

        // 销售属性-销售属性值回显并锁定
        CompletableFuture<Void> spuSalaAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
            map.put("spuSaleAttrList", spuSaleAttrListCheckBySku);
        }, threadPoolExecutor);

        //根据skuId获取价格信息
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            map.put("price", skuPrice);
        }, threadPoolExecutor);

        //获取商品分类信息
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            map.put("categoryView", categoryView);
        }, threadPoolExecutor);

        //根据spuId 查询map 集合属性
        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            map.put("valuesSkuJson", valuesSkuJson);
        }, threadPoolExecutor);

        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        },threadPoolExecutor);

//        Map<String,Object> map = new HashMap<>();
//        //通过skuId获取skuInfo信息
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//        // 销售属性-销售属性值回显并锁定
//        List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//        //根据skuId获取价格信息
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        //获取商品分类信息
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//        //根据spuId 查询map 集合属性
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//        //保存json字符串
//        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
//        map.put("skuInfo",skuInfo);
//        map.put("spuSaleAttrList",spuSaleAttrListCheckBySku);
//        map.put("price",skuPrice);
//        map.put("categoryView",categoryView);
//        map.put("valuesSkuJson",valuesSkuJson);
        CompletableFuture.allOf(skuInfoCompletableFuture,spuSalaAttrCompletableFuture,skuPriceCompletableFuture,categoryViewCompletableFuture,skuValueIdsMapCompletableFuture,incrHotScoreCompletableFuture).join();
        return map;
    }
}
