package com.atguigu.gmall.payment.client;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.impl.PaymentFeignClientImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author gao
 * @create 2020-05-11 19:55
 */
@FeignClient(value = "service-payment",fallback = PaymentFeignClientImpl.class)
public interface PaymentFeignClient {

    @GetMapping("/api/payment/alipay/checkPayment/{orderId}")
    Boolean checkPayment(@PathVariable Long orderId);

    @GetMapping("/api/payment/alipay/getPaymentInfo/{outTradeNo}")
    PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);

    @GetMapping("\"/api/payment/alipay/closePay/{orderId}")
    Boolean closePay(@PathVariable Long orderId);
}
