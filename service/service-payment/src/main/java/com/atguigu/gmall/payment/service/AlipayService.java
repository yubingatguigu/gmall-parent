package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @author gao
 * @create 2020-05-08 21:50
 */
public interface AlipayService {

    String createaliPay(Long orderId) throws AlipayApiException;

    //根据orderId退款
    boolean refund(Long orderId);

    //关闭支付宝交易记录
    Boolean closePay(Long orderId);

    //检查是否在支付宝中由交易记录
    Boolean checkPayment(Long orderId);
}
