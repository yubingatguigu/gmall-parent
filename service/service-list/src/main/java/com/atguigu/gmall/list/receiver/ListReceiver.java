package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author gao
 * @create 2020-05-07 20:57
 */
@Component
public class ListReceiver {//监听service—product发过来的消息

    @Autowired
    private SearchService searchService;

    //监听商品的上架
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel){
        if(null != skuId){//判断skuId不能为空
            //有商品的id，调用商品的上架操作，将数据库从mysql-es中
            searchService.upperGoods(skuId);
        }
        //手动签收确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //监听商品的下架
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lower(Long skuId,Message message,Channel channel){
        if(null!=skuId){//判断skuId不能为空
            //有商品的id，调用商品的上架操作，将数据库从mysql-es中
            searchService.lowerGoods(skuId);
        }
        //手动签收确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
