package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author gao
 * @create 2020-05-08 20:02
 */
@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //支付页
    @GetMapping("pay.html")
    public String success(HttpServletRequest request){
        //获取订单id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        request.setAttribute("orderInfo",orderInfo);
        return "payment/pay";
    }

    @GetMapping("pay/success.html")
    public String success(){
        //支付成功页面
        return "payment/success";
    }
}
