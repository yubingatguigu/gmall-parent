package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author gao
 * @create 2020-04-21 21:37
 */
public interface ItemService {
    //商品详情页面想要获取数据，必须有一个skuId ，商品详情页面是从list.html检索页面传过来的
    //https://item.jd.com/100006947212.html  item.jd.com是域名  100006947212.html是控制器，并不是一个单纯的页面
    //获取sku的详情信息
    Map<String,Object> getBySkuId(Long skuId);
}
