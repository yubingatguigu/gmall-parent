package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author gao
 * @create 2020-05-08 20:56
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {

        //查询交易记录，不应存在支付类型，订单id，是多条记录的数据
        Integer count = paymentInfoMapper.selectCount(new QueryWrapper<PaymentInfo>().eq("order_id", orderInfo.getId()).eq("payment_type", paymentType));
        //如果存在该数据，不能再次插入数据
        if(count>0) return;


        //创建一个PaymentInfo对象
        PaymentInfo paymentInfo = new PaymentInfo();
        Long orderId = orderInfo.getId();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setPaymentType(paymentType);
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        //select * from payment_info where out_trade_no = outTradeNo and payment_type = name 根据out_trade_no，查询数据
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }

    @Override
    public void paySuccess(String outTradeNo, String name) {

        PaymentInfo paymentInfoUpd = new PaymentInfo();
        //更新状态
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID.name());
        //更新回调时间
        paymentInfoUpd.setCallbackTime(new Date());
        //更新回调内容
        paymentInfoUpd.setCallbackContent("异步回调！！！");
        //构建更新条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        paymentInfoMapper.update(paymentInfoUpd,paymentInfoQueryWrapper);
    }

    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramsMap) {

        PaymentInfo paymentInfoUpd = new PaymentInfo();
        //更新状态
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID.name());
        //更新回调时间
        paymentInfoUpd.setCallbackTime(new Date());
        //更新回调内容
        paymentInfoUpd.setCallbackContent(paramsMap.toString());
        //追加更新  支付宝的交易号
        String trade_no = paramsMap.get("trade_no");
        paymentInfoUpd.setTradeNo(trade_no);
        //构建更新条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        paymentInfoMapper.update(paymentInfoUpd,paymentInfoQueryWrapper);
        //查询订单id
        PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, name);
        //支付成功后发送消息通知订单，更改订单状态
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
    }

    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        paymentInfoMapper.update(paymentInfo,new QueryWrapper<PaymentInfo>().eq("out_trade_no",outTradeNo));
    }

    @Override
    public void closePayment(Long orderId) {

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());

        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderId);

        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        //如果交易记录中没有当前数据，则返回，不执行关闭
        //当用户点击支付宝生成支付二维码的时候paymentInfo才会记录，只下单，不点击二维码 表中没有数据
        if(null != count || count.intValue()==0){
            return;
        }

        //更新内容，更新条件
        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
    }
}
