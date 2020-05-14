package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;


/**
 * @author gao
 * @create 2020-05-07 23:20
 */
@Configuration
public class DelayedMqConfig {

    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    @Bean
    public Queue delayQueue(){
        //声明一个队列
        return new Queue(queue_delay_1,true);
    }

    @Bean
    public CustomExchange delayExchange(){
        HashMap<String, Object> map = new HashMap<>();
        //基于插件时指定的参数
        map.put("x-delayed-type","direct");
        //基于插件的交换机
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,map);
    }

    @Bean
    public Binding delayBinding(){
        //基于插件的绑定方式
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay).noargs();
    }
}
