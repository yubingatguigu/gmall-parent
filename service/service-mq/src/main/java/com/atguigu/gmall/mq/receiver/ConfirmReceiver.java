package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author gao
 * @create 2020-05-07 19:33
 */
@Configuration
@Component
public class ConfirmReceiver {

    //接收处理消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",autoDelete ="false"),
            exchange = @Exchange(value = "exchange.confirm",autoDelete = "true"),
            key = {"routing.confirm"}
    ))
    public void confirmMessage(Message message, Channel channel){
        //将字节数组转换为字符串
        String str = new String(message.getBody());
        System.out.println("接收到的消息是："+str);

        //第一个 long类型的id  第二个 确认消息的形式：false为每次确认一个消息，true为批量确认
        try {
            //如果有异常
            int i = 1/0;
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("出现异常！");
            //判断消息是否处理过一次
            if(message.getMessageProperties().getRedelivered()){
                System.out.println("消息已经处理过了！！！");
                //消息不重回队列
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            }else {
                System.out.println("消息即将返回队列！");
                //第三个参数是如果消息没有正确处理则会再次回到消息队列
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            }
        }
    }
}
