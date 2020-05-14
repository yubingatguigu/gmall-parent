package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author gao
 * @create 2020-05-08 0:07
 * //编写取消订单的配置
 */
@Configuration
public class OrderCanelMqConfig {

    @Bean
    public Queue delayQueue(){
        //声明一个队列
        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true);
    }

    @Bean
    public CustomExchange delayExchange(){
        HashMap<String, Object> map = new HashMap<>();
        //基于插件时指定的参数
        map.put("x-delayed-type","direct");
        //基于插件的交换机
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,map);
    }

    @Bean
    public Binding delayBinding(){
        //绑定交换机和队列
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
