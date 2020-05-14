package com.atguigu.gmall.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author gao
 * @create 2020-05-07 18:58
 * 发送消息配置类
 * ConfirmCallback：发送确认，消息是否正确到达交换机
 * ReturnCallback：消息没有正确到达队列出发回调，如果正确到达就不会执行该方法
 */
@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate; //消息队列模板

    @PostConstruct //和构造方法一致  初始化
    public void init(){
        //先初始化确认，回调方法，传入当前类
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }

    //如果消息没有到达交换机，则会执行该方法 ack=false,反之ack=true
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if(ack){//true 到达交换机
            log.info("消息成功发送到交换机！");
        }else{
            log.info("消息没有发送到交换机！");
        }
    }

    /**
     * @param message    消息的内容
     * @param replyCode  消息码
     * @param replyText  消息码的对应内容
     * @param exchange   绑定交换机
     * @param routingKey 绑定的routingkey
     */
    //交换机与队列绑定判断，消息从交换机正确绑定到队列不会执行该方法，反之会执行该方法
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {

        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);


    }
}
