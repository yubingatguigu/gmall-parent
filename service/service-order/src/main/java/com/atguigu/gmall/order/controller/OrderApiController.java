package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gao
 * @create 2020-05-04 22:57
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private OrderService orderService;

    @GetMapping("auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //获取用户的地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //获取送货清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        //声明一个OrderDetail送货清单集合
        List<OrderDetail> orderDetailList = new ArrayList<>();

        int totalNum = 0;
        //送货清单是OrderDetail
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            //记录件数，每个商品的skuNum相加即可
            totalNum+=cartInfo.getSkuNum();
            //将每个订单明细添加到当前的集合中
            orderDetailList.add(orderDetail);
        }
        //算出当前订单的总金额
        OrderInfo orderInfo = new OrderInfo();
        //将订单明细赋值给orderInfo
        orderInfo.setOrderDetailList(orderDetailList);
        //计算总金额
        orderInfo.sumTotalAmount();
        //将数据封装到map集合中
        HashMap<String, Object> map = new HashMap<>();
        //保存总金额，通过页面trade.html 可以找到页面对应存储的key
        map.put("totalAmount",orderInfo.getTotalAmount());
        //保存userAddressList
        map.put("userAddressList",userAddressList);
        //保存totalNum
        //map.put("totalNum",orderDetailList.size());
        map.put("totalNum",totalNum);//以sku的件数为总数
        //保存detailArrayList
        map.put("detailArrayList",orderDetailList);

        //生成一个流水号，并保存到作用域，给页面使用
        String tradeNo = orderService.getTradeNo(userId);
        //保存tredeNo
        map.put("tradeNo",tradeNo);

        return Result.ok(map);
    }

    //下订单的控制器
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //再保存之前将用户id赋值给orderInfo
        orderInfo.setUserId(Long.parseLong(userId));

        //在下订单之前进行校验，流水号不能无刷新重复提交
        String tradeNo = request.getParameter("tradeNo");
        //调用比较方法
        boolean flag = orderService.checkTradeNo(tradeNo,userId);
        //判断比较结果
        if(!flag){
            //提示不能下单
            return  Result.fail().message("不能无刷新回退重复下订单！");
        }
        //删除流水号
        orderService.deleteTradeNo(userId);
        //验证库存：用户购买的每个商品都必须验证，循环订单明细中的每一个商品
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if(null != orderDetailList && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                //循环判断 result=true表示有足够的库存
                boolean result = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
                if(!result){
                    //没有足够的库存
                    return Result.fail().message(orderDetail.getSkuName()+"没有足够的库存！");
                }
                //检查价格是否有变动，orderDetail.getOrdrePrice()==skuInfo.getPrice();
                //如果比较结果一致，价格有变动，提示用户重新下订单，购物车的价格有变动
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if(orderDetail.getOrderPrice().compareTo(skuPrice)!=0){
                    //判断不等于0，说明价格有变动
                    //更新购物车中的价格，重新查一遍
                    cartFeignClient.loadCartCache(userId);
                    return Result.fail().message(orderDetail.getSkuName()+"商品价格有变动!");
                }
            }
        }
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }

    //获取订单
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    //拆分订单
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        //获取传过来的参数
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        //获取到的子订单集合
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId),wareSkuMap);
        //声明一个map集合
        ArrayList<Map> mapArrayList = new ArrayList<>();
        //获取子订单集合的字符串
        for (OrderInfo orderInfo : subOrderInfoList) {
            //将子订单的部分数据变为map，再将map转换为字符串
            Map map = orderService.initWareOrder(orderInfo);
            mapArrayList.add(map);
        }
        //返回子订单的集合字符串
        return JSON.toJSONString(mapArrayList);
    }

    //提交秒杀订单
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){
        //保存订单数据
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }
}
