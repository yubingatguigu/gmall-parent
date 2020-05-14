package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author gao
 * @create 2020-05-12 0:20
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderFeignClient orderFeignClient;

    //查询所有秒杀商品数据
    @GetMapping("/findAll")
    public Result findAll(){
        return Result.ok(seckillGoodsService.findAll());
    }

    //获取秒杀商品详情数据
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId){
        return Result.ok(seckillGoodsService.getSeckillGoodsById(skuId));
    }

    //skuIdStr 下单码
    @GetMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request){
        //将用户id进行MD5加密，加密后的字符串，就是一个下单码
        String userId = AuthContextHolder.getUserId(request);
        //用户要秒杀的商品
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsById(skuId);
        //判断
        if(null != seckillGoods){
            //获取下单码，在商品秒杀范围内才能获取，在活动之后，结束之前
            Date curTime = new Date();//获取当前系统时间
            if(DateUtil.dateCompare(seckillGoods.getStartTime(),curTime)&&DateUtil.dateCompare(curTime,seckillGoods.getEndTime())){
                //符合条件生成下单码
                String skuIdStr = MD5.encrypt(userId);
                //保存给skuStr返回给页面使用
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("获取下单码失败！");
    }

    //根据用户和商品id实现秒杀下单
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId,HttpServletRequest request){
        //检查下单码
        String skuIdStr = request.getParameter("skuIdStr");//页面提交过来的下单码
        //下单码生成的规则，md5将用户进行加密
        String userId = AuthContextHolder.getUserId(request);
        //根据后台规则生成下单码
        String skuIdStrRes = MD5.encrypt(userId);
        if(!skuIdStr.equals(skuIdStrRes)){
            //请求合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //获取状态位
        String status = (String) CacheHelper.get(skuId.toString());
        //判断状态位
        if(StringUtils.isEmpty(status)){
            //请求不合法
            return Result.build(null,ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //可以抢购
        if("1".equals(status)){
            //记录当前谁在抢购商品，自定义抢购用户的实体类
            UserRecode userRecode = new UserRecode();
            userRecode.setSkuId(skuId);
            userRecode.setUserId(userId);

            //需要将用户放到队列中进行排队
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
        }else {
            //说明商品已经售罄
            return Result.build(null,ResultCodeEnum.SECKILL_FINISH);
        }
        return Result.ok();
    }

    //轮询页面的状态
    @GetMapping("auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId,HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(skuId,userId);
    }

    //给下订单页面提供数据支持
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request){
        //显示收货人地址，送货清单，总金额等
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //获取收货地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        //获取用户购买的商品
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if(null == orderRecode){
            return Result.fail().message("非法操作");
        }
        //获取用户购买的商品
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        //声明一个集合来存储订单明细
        List<OrderDetail> detailArrayList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        //给订单明细赋值
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setSkuNum(orderRecode.getNum());//在页面显示用户秒杀的商品
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        //还需要将数据保存到数据库
        detailArrayList.add(orderDetail);

        //计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();

        //声明一个map集合
        Map<String, Object> map = new HashMap<>();
        map.put("userAddressList",userAddressList);
        map.put("detailArrayList",detailArrayList);
        map.put("totalAmount",orderInfo.getTotalAmount());
        map.put("totalNum",orderRecode.getNum());
        return Result.ok(map);

    }

    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //赋值用户id
        orderInfo.setUserId(Long.parseLong(userId));
        //获取购买的数据
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if(null==orderRecode){
            return Result.fail().message("非法参数");
        }
        //提交订单操作
        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if(null==orderId){
            return Result.fail().message("非法参数，下单失败");
        }
        //删除下单信息
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        //将用户真正的下单记录保存上
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId,orderId.toString());
        return Result.ok(orderId);
    }
}
