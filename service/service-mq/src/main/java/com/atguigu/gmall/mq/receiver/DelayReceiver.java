package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author gao
 * @create 2020-05-07 23:42
 */
@Configuration
@Component
public class DelayReceiver {

    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void getMsg(String msg){
        System.out.println("接收数据："+msg);
        //声明一个时间格式的对象
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //看接收到消息的数据
        System.out.println("Receive queue_delay_1: " + simpleDateFormat.format(new Date()) + " Delay rece." + msg);
    }
}
