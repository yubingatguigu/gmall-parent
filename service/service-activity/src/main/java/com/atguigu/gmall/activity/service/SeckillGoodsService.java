package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @author gao
 * @create 2020-05-12 0:12
 */
public interface SeckillGoodsService {

    //查询所有的秒杀商品
    List<SeckillGoods> findAll();

    //根据skuId，查询商品详情
    SeckillGoods getSeckillGoodsById(Long skuId);

    //预下单处理
    void seckillOrder(Long skuId, String userId);

    //检查订单
    Result checkOrder(Long skuId, String userId);
}
