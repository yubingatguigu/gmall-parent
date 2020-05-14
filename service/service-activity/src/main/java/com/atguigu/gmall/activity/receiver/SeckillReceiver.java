package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @author gao
 * @create 2020-05-11 22:46
 * 监听消息
 */
@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    //监听消息，获取商品哪些是秒杀商品，并将商品添加到缓存中
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importGoodsToRedis(Message message, Channel channel){
        //秒杀商品：审核状态status = 1，startTime：new Date() 当天
        //查询所有的秒杀商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        //导入时间比较工具，时间只比较年月日
        seckillGoodsQueryWrapper.eq("status",1).eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> secKillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        //有秒杀商品
        if(null!=secKillGoodsList && secKillGoodsList.size()>0){
            //循环秒杀商品放入缓存
            for (SeckillGoods seckillGoods : secKillGoodsList) {
                //放入秒杀商品之前，先判断缓存中是否已经存在，如果已经存在就不需要放入
                //hset(key,field,value) key=seckill:goods  field=skuId  value=秒杀的商品对象
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                //当前缓存中秒杀商品
                if(flag){
                    //不放入数据
                    continue;
                }
                //缓存中没有数据，秒杀商品放入缓存
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);
                //分析商品数量如何存储和超卖
                //使用redis中list的数据类型redis是单线程
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    //key= seckill:skuId  value= skuId
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }
                //将所有的商品状态位初始化为1，状态位只有为1 的是时候商品才能秒，如果为0，不能秒
                redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
            }
            //手动确认消息成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    //监听用户放过来的消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillGoods(UserRecode userRecode,Message message,Channel channel){
        //判断用户信息不能为空
        if(null!=userRecode){
            //预下单处理
            seckillGoodsService.seckillOrder(userRecode.getSkuId(),userRecode.getUserId());
            //手动确认消息被处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    //监听用户放过来的消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void delSeckillGoods(Message message,Channel channel){
        //删除操作，查询结束的秒杀商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        //结束时间
        seckillGoodsQueryWrapper.eq("status",1).le("end_time",new Date());
        //获取结束的秒杀商品
        List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        //删除缓存中的数据
        if(!CollectionUtils.isEmpty(seckillGoods)){
            for (SeckillGoods seckillGood : seckillGoods) {
                //删除缓存的秒杀数量
                redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGood.getSkuId());
            }
        }
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);

        //变更数据库状态
        SeckillGoods seckillGoods1 = new SeckillGoods();
        seckillGoods1.setStatus("2");
        seckillGoodsMapper.update(seckillGoods1,seckillGoodsQueryWrapper);
        //手动确认消息已被消费
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
