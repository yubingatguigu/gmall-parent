package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author gao
 * @create 2020-05-07 21:45
 */
@Configuration
public class DeadLetterMqConfig {

    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    @Bean
    public DirectExchange exchange(){
        //返回一个交换机
        return new DirectExchange(exchange_dead,true,false,null);
    }

    @Bean
    public Queue queue1(){
        //给当前队列设置参数
        HashMap<String, Object> map = new HashMap<>();
        //设置一个死信交换机
        map.put("x-dead-letter-exchange",exchange_dead);
        //给当前死信交换机绑定一个队列  绑定死信的routingkey  死信交换机本质就是一个延时队列
        map.put("x-dead-letter-routing-key",routing_dead_2);
        //统一规定延迟时间
        map.put("x-message-ttl",1000*10);
        return new Queue(queue_dead_1,true,false,false,map);
    }

    @Bean
    public Binding binding(){
        //绑定队列
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    @Bean
    public Queue queue2(){
        //第二个队列，如果队列1出现问题，会走队列2
        return new Queue(queue_dead_2,true,false,false,null);
    }

    @Bean
    public Binding deadBingding(){
        //将队列2绑定到交换机
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}
