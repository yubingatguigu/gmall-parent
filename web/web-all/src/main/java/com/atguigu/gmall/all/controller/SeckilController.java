package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author gao
 * @create 2020-05-12 0:31
 */
@Controller
public class SeckilController {

    @Autowired
    private ActivityFeignClient activityFeignClient;


    @GetMapping("seckill.html")
    public String getAll(Model model){
        //获取秒杀商品数据
        Result result = activityFeignClient.findAll();
        //后台存储一个list集合
        model.addAttribute("list",result.getData());
        //返回秒杀商品页面
        return "seckill/index";
    }

    @GetMapping("seckill/{skuId}.html")
    public String getItem(@PathVariable Long skuId,Model model){
        //获取秒杀详情数据
        Result result = activityFeignClient.getSeckillGoods(skuId);
        //存储一个item对象
        model.addAttribute("item",result.getData());
        //返回秒杀商品详情页面
        return "seckill/item";
    }

    @GetMapping("seckill/queue.html")
    public String queue(@RequestParam(name="skuId") Long skuId, @RequestParam(name = "skuIdStr") String skuIdStr, HttpServletRequest request){
        //存储skuId，skuIdStr
        request.setAttribute("skuId",skuId);
        request.setAttribute("skuIdStr",skuIdStr);
        return "seckill/queue";
    }

    @GetMapping("seckill/trade.html")
    public String trade(Model model){
        //获取下单数据
        Result<Map<String,Object>> result = activityFeignClient.trade();
        if(result.isOk()){
            //将数据保存，给页面提供渲染
            model.addAllAttributes(result.getData());
            //返回订单页面
            return "seckill/trade";
        }else {
            //存储失败信息
            model.addAttribute("message",result.getMessage());
            //返回订单失败页面
            return "seckill/fail";
        }
    }
}
