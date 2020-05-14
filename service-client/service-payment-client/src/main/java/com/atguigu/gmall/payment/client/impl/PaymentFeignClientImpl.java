package com.atguigu.gmall.payment.client.impl;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import org.springframework.stereotype.Component;

/**
 * @author gao
 * @create 2020-05-11 19:56
 */
@Component
public class PaymentFeignClientImpl implements PaymentFeignClient {
    @Override
    public Boolean checkPayment(Long orderId) {
        return null;
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        return null;
    }

    @Override
    public Boolean closePay(Long orderId) {
        return null;
    }
}
