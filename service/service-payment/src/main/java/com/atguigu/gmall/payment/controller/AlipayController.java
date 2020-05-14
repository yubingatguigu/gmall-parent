package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author gao
 * @create 2020-05-08 22:10
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    //根据订单id，完成支付二维码显示
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable Long orderId){
        String aliPay="";
        try {
             aliPay = alipayService.createaliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return aliPay;
    }

    //同步回调
    @RequestMapping("callback/return")
    public String callBack(){
        //同步回调给用户展示信息
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    //异步回调
    @RequestMapping("callback/notify")
    @ResponseBody
    public String aliPayNotify(@RequestParam Map<String,String> paramsMap){
        //Map<String, String> paramsMap = ...  //将异步通知中收到的所有参数都存放到 map 中
        boolean  signVerified = false;

        //将支付宝通知的参数封装到paramsMap集合中
        String trade_status = paramsMap.get("trade_status");
        String out_trade_no = paramsMap.get("out_trade_no");

        try {
            //验证签名成功
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);  //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功
            if("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                //支付成功，更改支付状态
                //查询支付交易记录状态，如果为PAID,CLOSED返回failure,只有为UNPAID为成功，目的：防止重复付款
                //查询支付状态
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
                if(paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name())||paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())){
                    return "failure";
                }
                //验证金额
//                String total_amount = paramsMap.get("total_amount");
//                int amount = Integer.parseInt(total_amount);
//                BigDecimal totalAmount = new BigDecimal(amount);
//                if(paymentInfo.getTotalAmount().compareTo(totalAmount)==0&&paymentInfo.getOutTradeNo().equals(out_trade_no)){
//                    //处理PAID,CLOSED之外，更新交易记录
//                    paymentService.paySuccess(out_trade_no,PaymentType.ALIPAY.name(),paramsMap);
//                    return "success";
//                }

                //处理PAID,CLOSED之外，更新交易记录
                paymentService.paySuccess(out_trade_no,PaymentType.ALIPAY.name(),paramsMap);

                return "success";
            }
        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    //退款
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId){
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    //查看是否由交易记录
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){
        Boolean aBoolean = alipayService.checkPayment(orderId);
        return aBoolean;
    }

    //根据outTradeNo获取支付信息
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if(null != paymentInfo){
            return paymentInfo;
        }
        return null;
    }

    //根据订单id关闭订单
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){
        Boolean aBoolean = alipayService.closePay(orderId);
        return aBoolean;
    }

}
