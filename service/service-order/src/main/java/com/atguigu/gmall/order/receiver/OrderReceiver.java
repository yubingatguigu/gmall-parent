package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author gao
 * @create 2020-05-08 0:26
 * 取消订单
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel){
        //判断订单id是否为空
        if(null != orderId){
            //根据订单id查询订单是否有当前记录
            OrderInfo orderInfo = orderService.getById(orderId);
            if(null!=orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                //先关闭paymentInfo，后关闭orderInfo，因为支付成功后异步回调先修改paymentInfo后再修改orderInfo
                //关闭流程，先看电商平台中的交易记录是否有数据，如果有则关闭
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                //判断电脑交易记录，交易记录表中有数据，说明一定走到了二维码那一步
                if(null!=paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
                    //检查支付宝中是否有交易记录
                    Boolean flag = paymentFeignClient.checkPayment(orderId);
                    //说明用户在支付宝中产生了交易记录，用户扫了
                    if(flag){
                        //关闭支付宝
                        Boolean result = paymentFeignClient.closePay(orderId);
                        //判断是否关闭成功
                        if(result){
                            //关闭支付宝的订单成功，  关闭orderInfo表，paymentInfo
                            orderService.execExpiredOrder(orderId,"2");
                        }else {
                            //关闭支付宝订单失败，如果用户付款成功我们是调用接口失败的
                            //如果成功走正常流程
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,orderId);
                        }
                    }else {
                        //用户没有扫描，到了二维码
                        //关闭支付宝的订单成功，  关闭orderInfo表，paymentInfo
                        orderService.execExpiredOrder(orderId,"2");
                    }
                }else {
                    //说明paymentInfo中根本就没有数据,没有数据那就只需要关闭paymentInfo
                    orderService.execExpiredOrder(orderId,"1");
                }
            }
        }

        //手动确认消息处理
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

//        if(null!=orderId){
//            //查询是否有当前订单
//            OrderInfo orderInfo = orderService.getById(orderId);
//            //关闭订单之前先查询订单的状态
//            if(null!=orderInfo){
//                if(orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
//                    //满足需求，关闭订单
//                    orderService.execExpiredOrder(orderId);
//                }
//            }
//            //手动确认消息处理
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
//        }
    }

    //监听消息，更改订单的状态
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void getMeg(Long orderId,Message message,Channel channel){
        //判断orderId不能为空
        if(null != orderId){
            //判断支付状态是未付款
            OrderInfo orderInfo = orderService.getById(orderId);
            if(null != orderId){
                if(orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                    //更新订单状态
                    orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
                    //发送消息给库存
                    orderService.sendOrderStatus(orderId);
                }
            }
            //手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    //监听库存系统，减库存的消息队列
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson,Message message,Channel channel){
        //判断发送消息不能为空
        if(!StringUtils.isEmpty(msgJson)){
            //msgJson是由map组成，将字符串在转为map，获取里面的orderId，status
            Map map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            //判断减库存是否成功
            if("DEDUCTED".equals(status)){
                //减库存成功，将订单状态改为等待发货
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
            }else {
                //减库存失败，订单中的商品数量>库存数量，会发生超卖  调用其他库存补货，人工客服进行沟通
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
            }
        }
    }
}
