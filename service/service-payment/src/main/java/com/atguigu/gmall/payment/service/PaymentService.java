package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author gao
 * @create 2020-05-08 20:53
 */
public interface PaymentService {

    //保存交易记录
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    //根据out_trade_no，付款方式查询交易记录
    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    //根据out_trade_no，付款方式更新交易记录
    void paySuccess(String outTradeNo, String name);

    //支付成功
    void paySuccess(String outTradeNo, String name, Map<String, String> paramsMap);

    //更新方法
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    //关闭交易记录
    void closePayment(Long orderId);
}
