package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author gao
 * @create 2020-05-07 19:28
 */
@RestController
@RequestMapping("/mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("sendConfirm")
    public Result sendConfirm(){//发送消息
        String message = "hello RabbitMQ !!!";
        rabbitService.sendMessage("exchange.confirm","routing.confirm",message);
        return Result.ok();
    }

    //规定单独消息发送数据的延迟时间
//    @GetMapping("sendDeadLettle")
//    public Result sendDeadLettle(){
//        //声明一个时间格式的对象
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        //发送消息
//        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"hello world",
//               message -> {
//                    //定义发送的内容及消息TTL  消息的存活时间为10秒
//                   message.getMessageProperties().setExpiration(1000*10+"");
//                   System.out.println(simpleDateFormat.format(new Date())+"Delay sent.");
//                   return message;
//               } );
//        return Result.ok();
//    }

    //通过规定队列的延迟时间
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        //声明一个时间格式的对象
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"atguigu");
        System.out.println(simpleDateFormat.format(new Date())+"Delay sent.");
        return Result.ok();
    }

    @GetMapping("sendDelay")
    public Result sendDelay(){
        //声明一个时间格式的对象
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay,
                simpleDateFormat.format(new Date()), new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        //设置延迟时间
                        message.getMessageProperties().setDelay(10*1000);
                        System.out.println(simpleDateFormat.format(new Date())+"delay send ...");
                        return message;
                    }
                });
        return Result.ok();
    }
}
