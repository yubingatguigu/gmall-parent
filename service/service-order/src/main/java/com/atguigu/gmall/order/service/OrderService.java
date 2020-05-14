package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author gao
 * @create 2020-05-05 0:04
 */
public interface OrderService extends IService<OrderInfo> {

    //保存订单  传入json对象
    Long saveOrderInfo(OrderInfo orderInfo);

    //生成流水号，同时放入缓存
    String getTradeNo(String userId);

    //比较流水号
    boolean checkTradeNo(String tradeNo,String userId);

    //删除缓存的流水号
    void deleteTradeNo(String userId);

    //根据skuId,skuNum,判断是否有足够的库存
    boolean checkStock(Long skuId, Integer skuNum);

    //根据orderId,关闭过期订单
    void execExpiredOrder(Long orderId);

    //根据orderId，修改订单的状态
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    //根据orderId，查询订单数据
    OrderInfo getOrderInfo(Long orderId);

    //通过orderId，发送消息给库存，通知减库存
    void sendOrderStatus(Long orderId);

    //将orderInfo变为map集合
    Map initWareOrder(OrderInfo orderInfo);

    //拆单
    List<OrderInfo> orderSplit(long orderId, String wareSkuMap);

    //关闭过期订单
    void execExpiredOrder(Long orderId, String flag);
}
