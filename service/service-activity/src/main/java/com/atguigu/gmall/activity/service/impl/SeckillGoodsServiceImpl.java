package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author gao
 * @create 2020-05-12 0:15
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public List<SeckillGoods> findAll() {
        //每天夜晚扫描发送消息，消费消息数据放入缓存
        return redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
    }

    @Override
    public SeckillGoods getSeckillGoodsById(Long skuId) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
    }

    @Override
    public void seckillOrder(Long skuId, String userId) {
        /*
        1，监听用户，同一个用户不能抢购两次
        2.判断状态位
        3，监听商品库存数量（redis——list）
        4，将用户秒杀记录放入缓存
         */
        //判断状态
        String status = (String) CacheHelper.get(skuId.toString());
        if("0".equals(status)){
            return;//没有商品了
        }
        //保证用户不能抢多次，第一次抢到将抢到的信息放入缓存中，利用redis--setnx()判断key是否存在
        //userSeckillKey=seckill:user:userId
        String userSeckillKey = RedisConst.SECKILL_USER+userId;
        //返回成功true，说明第一次添加key，返回false，用户不是第一次添加key
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(userSeckillKey, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if(!isExist){
            return;
        }
        //用户可以下单，减少库存
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if(StringUtils.isEmpty(goodsId)){
            //如果没有吐出来那么说明已售罄，通知其他兄弟节点，当商品没有更新内存中的状态位
            redisTemplate.convertAndSend("seckillpush",skuId+":0");
            return;
        }
        //记录订单，做一个秒杀的订单类
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        orderRecode.setSeckillGoods(getSeckillGoodsById(skuId));
        orderRecode.setNum(1);
        orderRecode.setOrderStr(MD5.encrypt(userId));

        //将用户秒杀订单放入缓存
        String orderSeckillKey = RedisConst.SECKILL_ORDERS;
        redisTemplate.boundHashOps(orderSeckillKey).put(orderRecode.getUserId(),orderRecode);

        //更新商品的数量
        updateStockCount(orderRecode.getSeckillGoods().getSkuId());
    }

    @Override
    public Result checkOrder(Long skuId, String userId) {
        //判断用户是否存在，用户不能购买两次
        Boolean isExist = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if(isExist){
            //判断订单是否存在
            Boolean isHashKey = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if(isHashKey){
                //抢单成功，获取用户订单对象
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                //返回数据
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        //判断用户是否下过订单
        Boolean isExistOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        //返回true 执行成功
        if(isExistOrder){
            //获取订单id
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            //因该是第一次下单成功
            return Result.build(orderId,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        //判断状态位
        String status = (String) CacheHelper.get(skuId.toString());
        //status=1，表示能够抢单，如果是0表示抢单失败，已经售罄
        if("0".equals(status)){
            return Result.build(null,ResultCodeEnum.SECKILL_FINISH);
        }
        //默认情况下
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

    private void updateStockCount(Long skuId) {
        //秒杀商品的库存 在缓存中有一份，在数据库中有一份
        Long count = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //防止频繁更新数据库
        if(count%2==0){
            //获取缓存中当前的秒杀商品
            SeckillGoods seckillGoods = getSeckillGoodsById(skuId);
            seckillGoods.setStockCount(count.intValue());

            //更新数据库
            seckillGoodsMapper.updateById(seckillGoods);
            //更新缓存
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(skuId.toString(),seckillGoods);

        }
    }
}
