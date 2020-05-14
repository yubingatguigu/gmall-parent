package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.impl.ListDegradeFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author gao
 * @create 2020-04-28 1:08
 */
@FeignClient(name = "service-list",fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {

    //更新商品incrHotScore
    @GetMapping("/api/list/inner/incrHotScore/{skuId}")
    Result incrHotScore(@PathVariable Long skuId);

    //上架
    @GetMapping("/api/list/inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId);

    //下架
    @GetMapping("/api/list/inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId);

    //搜索商品
    @PostMapping("/api/list")
    public Result list(@RequestBody SearchParam listParam);

}
