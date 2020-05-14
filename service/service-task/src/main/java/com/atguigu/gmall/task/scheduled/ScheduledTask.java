package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author gao
 * @create 2020-05-11 22:31
 */
@Component
@EnableScheduling //开启定时任务
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    //定时任务开启的时间是凌晨1点，
    //@Scheduled(cron = "0 0 1 * * ?")
    @Scheduled(cron = "0/30 * * * * ?") //每30秒触发当前任务
    public void task(){
        //发送消息，发送内容为空，处理消息的时候扫描秒杀商品
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"");
    }

    @Scheduled(cron = "* * 18 * * ?") //每30秒触发当前任务 每天晚上18点删除
    public void taskDelRedis(){
        //发送消息，发送内容为空，处理消息的时候扫描秒杀商品
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_18,"");
    }
}
