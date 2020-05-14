package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author gao
 * @create 2020-05-05 0:06
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String WARE_URL;

    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {

        //保存orderInfo，缺少总金额，userId，订单状态，第三方交易编号，创建订单时间，订单过期时间，进程状态
        orderInfo.sumTotalAmount();
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //第三方交易编号
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间 先获取日历对象
        Calendar calendar = Calendar.getInstance();
        //在日历对象的基础上添加1天
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        //订单的主题描述，获取订单明细中的商品名称，将商品名称拼接在一起
        //订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuilder sb = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            sb.append(orderDetail.getSkuName()+"");
        }
        //做个字串串的长度处理
        if(sb.toString().length()>100){
            orderInfo.setTradeBody(sb.toString().substring(0,100));
        }else {
            orderInfo.setTradeBody(sb.toString());
        }
        orderInfoMapper.insert(orderInfo);


        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }
        //发送消息队列
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,MqConst.ROUTING_ORDER_CANCEL,orderInfo.getId(),MqConst.DELAY_TIME);

        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        //获取流水号
        String tradeNo = UUID.randomUUID().toString().replace("-","");
        //将流水号放入缓存
        String tradeNoKey = "user:"+userId+":tradeCode";
        //使用String 数据类型
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo;
    }

    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        //将流水号放入缓存
        String tradeNoKey = "user:"+userId+":tradeCode";
        //获取缓存的流水号
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(redisTradeNo);
    }

    @Override
    public void deleteTradeNo(String userId) {
        String tradeNoKey = "user:"+userId+":tradeCode";
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        //远程调用库存系统httpClientUtil工具类远程调用 是一个单独的Springboot
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //0表示没有库存，1表示有足够的库存
        return "1".equals(result);
    }

    @Override
    public void execExpiredOrder(Long orderId) {
        //关闭订单
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //发送消息关闭支付宝订单，或关闭交易记录
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
    }

    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {

        OrderInfo orderInfo = new OrderInfo();
        //赋值更新条件
        orderInfo.setId(orderId);
        //赋值更新内容
        orderInfo.setProcessStatus(processStatus.name());
        //订单状态可以从进程状态获取
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());

        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        //select * from order_info where id = orderId 订单信息
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //select * from order_detail where order_id = orderId  订单详情信息
        QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
        orderDetailQueryWrapper.eq("order_id",orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailQueryWrapper);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void sendOrderStatus(Long orderId) {

        //更改订单状态
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        //发送一个JSON字符串
        String wareJson = initWareOrder(orderId);
        //发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,wareJson);
    }

    //获取发送的json字符串
    public String initWareOrder(Long orderId) {

        //json字符串是由orderInfo组成，先获取orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        //将orderInfo对象中的部分字符串转为Map集合，在转换为json字符串
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    //将orderInfo对象中的部分字符串转换为map集合
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        ArrayList<Object> mapArrayList = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("skuId",orderDetail.getSkuId());
            hashMap.put("skuName",orderDetail.getSkuName());
            hashMap.put("skuNum",orderDetail.getSkuNum());
            mapArrayList.add(hashMap);
        }
        map.put("details",mapArrayList);
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(long orderId, String wareSkuMap) {
        /*
        1.获取原始订单
        2.将wareSkuMap参数转化为我们程序可操作的对象
        3.创建一个新的子订单
        4.给子订单进行赋值
        5.保存子订单
        6.将子订单添加到集合
        7.修改订单的状态
         */
        ArrayList<OrderInfo> subOrderInfoList = new ArrayList<>();
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        //判断当前map集合是否为空
        if(null != mapList && mapList.size()>0){
            for (Map map : mapList) {
                //获取仓库id
                String wareId = (String) map.get("wareId");
                List<String> skuIds = (List<String>) map.get("skuIds");
                OrderInfo subOrderInfo = new OrderInfo();
                //通过属性拷贝
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                //拷贝时注意id，设置为主键自增
                subOrderInfo.setId(null);
                //原始订单的id
                subOrderInfo.setParentOrderId(orderId);
                //赋值仓库id
                subOrderInfo.setWareId(wareId);
                //声明一个子订单明细
                ArrayList<OrderDetail> orderDetailLists = new ArrayList<>();
                //赋值子订单的明细表，先获取原始订单明细的集合
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                for (OrderDetail orderDetail : orderDetailList) {
                    //对比条件商品id
                    for (String skuId : skuIds) {
                        //判断skuId是否存在
                        if(Long.parseLong(skuId)==orderDetail.getSkuId().intValue()){
                            //将子订单明细保存上
                            orderDetailLists.add(orderDetail);
                        }
                    }
                }
                //子订单明细赋值给子订单
                subOrderInfo.setOrderDetailList(orderDetailLists);
                //计算子订单的金额
                subOrderInfo.sumTotalAmount();
                //保存子订单
                saveOrderInfo(subOrderInfo);
                //添加子订单到集合
                subOrderInfoList.add(subOrderInfo);
            }
        }
        //修改订单的状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        //关闭订单 flag=1的时候，指关闭orderInfo
        updateOrderStatus(orderId,ProcessStatus.CLOSED);

        if("2".equals(flag)){
            //发送消息关闭支付宝订单，或关闭交易记录
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }


}
