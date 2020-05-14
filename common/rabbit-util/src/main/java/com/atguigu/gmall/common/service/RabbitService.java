package com.atguigu.gmall.common.service;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author gao
 * @create 2020-05-07 19:20
 */
@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //发送非延迟消息
    public boolean sendMessage(String exchange,String routingKey,Object message){

        //发送消息
        rabbitTemplate.convertAndSend(exchange,routingKey,message);
        return true;
    }

    //发送延迟消息
    public boolean sendDelayMessage(String exchange,String routingKey,Object message,int delayTime){
        //设置发送消息
        rabbitTemplate.convertAndSend(exchange, routingKey, message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                //设置延迟时间，单位时毫秒
                message.getMessageProperties().setDelay(delayTime*1000);
                return message;
            }
        });
        return true;
    }
}
