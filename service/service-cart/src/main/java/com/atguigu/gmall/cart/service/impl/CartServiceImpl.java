package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.bouncycastle.est.CACertsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author gao
 * @create 2020-05-03 9:27
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {

        //获取购物车中的key
        String cartKey = getCartKey(userId);
        //判断缓存中是否有购物车的key
        if(!redisTemplate.hasKey(cartKey)){
            //查询数据库并添加到缓存中
            loadCartCache(userId);
        }
        //获取数据库的对象
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("sku_id",skuId).eq("user_id",userId);
        //看数据库中购物车是否有添加的商品
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQueryWrapper);

        if(null != cartInfoExist){
            //说明购物车中已经添加过当前商品
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //赋值一个实时价格，在数据库中不存在
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfoExist.setSkuPrice(skuPrice);
            //跟新数据库
            cartInfoMapper.updateById(cartInfoExist);
            //添加到缓存，添加完成之后  ，查询购物车列表直接走缓存，如果缓存过期了，才走缓存

        }else{
            //说明购物车中没有当前商品
            CartInfo cartInfo = new CartInfo();
            //给cartInfo赋值
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            //添加到数据库
            cartInfoMapper.insert(cartInfo);

            //添加到缓存，添加完成之后  ，查询购物车列表直接走缓存，如果缓存过期了，才走缓存
            cartInfoExist=cartInfo;
        }
        //添加到缓存，添加完成之后  ，查询购物车列表直接走缓存，如果缓存过期了，才走缓存
        //使用hash数据类型 hset（key，field，value） key = cartKey field= skuId value= 添加商品的字符串
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
        //购物车在缓存中有过期时间
        setCartKeyExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        //声明一个集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        //未登录临时用户id，
        if(StringUtils.isEmpty(userId)){
            //获取未登录的购物车数据
            cartInfoList = getCartList(userTempId);
            return cartInfoList;
        }
        //登录时的用户id
        if(!StringUtils.isEmpty(userId)){
            //查询未登录时购物车是否有数据
            List<CartInfo>  cartTempList= getCartList(userTempId);
            //当临时购物车不为空的情况下
            if(!CollectionUtils.isEmpty(cartTempList)){
                //登录+未登录  合并之后的数据
                cartInfoList = mergeToCartList(cartTempList,userId);
                //删除未登录的数据
                deleteCartList(userTempId);
            }
            //如果未登录的购物车没有数据
            if(CollectionUtils.isEmpty(cartTempList) || StringUtils.isEmpty(userTempId)){
                //获取登录时的购物车的数据
                cartInfoList = getCartList(userId);
            }

            return cartInfoList;
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //更改选中状态 修改数据库 第一个参数表示修改的内容，第二个参数表示条件
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.update(cartInfo,cartInfoQueryWrapper);
        //获取缓存的 key= user:userId:cart
        String cartKey = getCartKey(userId);
        //根据hash数据结构来获取数据
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        //判断商品在购物车中是否存在
        if(boundHashOperations.hasKey(skuId.toString())){
            //获取当前商品的id  对应的cartInfo
            CartInfo cartInfoUpd = (CartInfo) boundHashOperations.get(skuId.toString());
            //赋值选中状态
            cartInfoUpd.setIsChecked(isChecked);
            //修改之后的cartInfo 放入缓存
            boundHashOperations.put(skuId.toString(),cartInfoUpd);
            //每次修改完缓存后，设置过期时间
            setCartKeyExpire(cartKey);
        }
    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        //获取缓存中的key，先删除缓存，在删除数据库
        String cartKey = getCartKey(userId);
        //获取缓存
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        //判断缓存中是否有商品id
        if(boundHashOperations.hasKey(skuId.toString())){
            //如果有个key则删除
            boundHashOperations.delete(skuId.toString());
        }
        //删除数据库
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userId).eq("sku_id",skuId));
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //在展示购物车列表中去结算，做订单页面，直接从缓存中查询
        List<CartInfo> cartInfoList = new ArrayList<>();
        //定义缓存的key
        String cartKey = getCartKey(userId);
        //获取缓存中的数据
        List<CartInfo> cartCacheList = redisTemplate.opsForHash().values(cartKey);
        if(null != cartCacheList && cartCacheList.size()>0){
            //循环遍历购物车中的数据
            for (CartInfo cartInfo : cartCacheList) {
                //获取被选中的数据
                if(cartInfo.getIsChecked().intValue()==1){
                    //将被选择中的商品添加到集合中
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }

    //删除购物车的数据
    private void deleteCartList(String userTempId) {
        //未登录购物车的数据：一个存在缓存，一个存在数据库
        String cartKey = getCartKey(userTempId);
        Boolean aBoolean = redisTemplate.hasKey(cartKey);
        //缓存中有key，则删除
        if(aBoolean){
            redisTemplate.delete(cartKey);
        }
        //删除数据库
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userTempId));
    }

    //合并购物车
    private List<CartInfo> mergeToCartList(List<CartInfo> cartTempList, String userId) {
        //合并 登录+未登录
        //通过用户id，获取登数据
        List<CartInfo> cartLoginList = getCartList(userId);
        //合并条件时商品id
        //以skuId为key 以cartInfo 为value
        Map<Long, CartInfo> cartInfoMapLogin = cartLoginList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        //循环未登录的购物车数据
        for (CartInfo cartInfoNoLogin : cartTempList) {
            //取出未登录的商品id
            Long skuId = cartInfoNoLogin.getSkuId();
            //看登录购物车中是否有未登录的skuId
            if(cartInfoMapLogin.containsKey(skuId)){
                //获取登录数据
                CartInfo cartInfoLogin = cartInfoMapLogin.get(skuId);
                //商品数量相加
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                //合并的时候判断商品是否被勾选    未登录状态下有商品是勾选状态
                if(cartInfoNoLogin.getIsChecked().intValue()==1){
                    //登录状态下 商品因该为选中
                    cartInfoLogin.setIsChecked(1);
                }
                //更新数据库
                cartInfoMapper.updateById(cartInfoLogin);
            }else{
                //将数据直接插入数据库  赋值用户id
                cartInfoNoLogin.setUserId(userId);
                cartInfoMapper.insert(cartInfoNoLogin);
            }
        }
        //将合并之后的数据查询出来
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    //根据用户获取购物车
    private List<CartInfo> getCartList(String userId) {
        //声明一个集合对象来存储数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        //查询的用户id为空
        if(StringUtils.isEmpty(userId)){
            return cartInfoList;
        }
        //查询的用户不为空，先查询缓存，缓存没有，再查询数据库
        String cartKey = getCartKey(userId);
         cartInfoList = redisTemplate.opsForHash().values(cartKey);
         //判断集合是否为空
        if(null != cartInfoList && cartInfoList.size()>0){
            //展示购物车的时候会根据跟更新时间进行排序
            //当前项目没有更新时间，模拟一个，按照id进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                //比较器
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            //返回当前集合
            return cartInfoList;
        }else {
            //缓存没有数据，走数据库并放入缓存  根据用户id查询数据
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }

    }

    //根据用户id，查询数据库中的购物车数据，并添加到缓存
    public List<CartInfo> loadCartCache(String userId) {

        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(cartInfoQueryWrapper);
        //如果数据库中没有数据
        if(CollectionUtils.isEmpty(cartInfoList)){
            return cartInfoList;
        }
        //如果数据库中有购物车的列表，循环遍历，将集合的没有给cartInfo放入缓存
        HashMap<String, CartInfo> hashMap = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            //hash的数据结构：hset一次存入一条数据， hmset 一次存入多条数据
            //缓存失效
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            //将数据放入map中
            hashMap.put(cartInfo.getSkuId().toString(),cartInfo);
        }
        //在此将map放入缓存，获取缓存的key
        String cartKey = getCartKey(userId);
        redisTemplate.opsForHash().putAll(cartKey,hashMap);
        //设置缓存的过期时间
        setCartKeyExpire(cartKey);
        return cartInfoList;
    }

    //设置购物车的过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    //获取购物车中的key
    private String getCartKey (String userId){
        //区分谁的购物车 user：userId；cart
        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}
