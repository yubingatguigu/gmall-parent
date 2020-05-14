package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author gao
 * @create 2020-04-25 0:42
 */
@Component
@Aspect
public class GmallCacheAspect {

    //引入redis,redissonClient
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //利用环绕通知来获取对应的数据
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point){
        //声明一个Object
        Object result  = null;
        //获取传递过来的参数
        Object[] args = point.getArgs();
        //获取方法上的签名，查看方法上是否有注解
        MethodSignature methodSignature  = (MethodSignature) point.getSignature();
        //获取注解
        GmallCache gmallCache = methodSignature.getMethod().getAnnotation(GmallCache.class);
        //获取参数的前缀
        String prefix = gmallCache.prefix();
        //组成缓存的key sku：[]
        String key = prefix+ Arrays.asList(args).toString();
        //从缓存中获取数据
        result=cacheHit(key,methodSignature);
        //判断 缓存中有数据
        if(result!=null){
            return result;
        }
        //缓存中没有数据  分布锁
        RLock lock = redissonClient.getLock(key + ":lock");
        try{
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            if(res){
                //获取分布式锁，从数据库获取数据
                result = point.proceed(point.getArgs());
                //潘顿result是否为空，防止穿透
                if(null==result){
                    Object o = new Object();//下面的方法判断了数据空
                    redisTemplate.opsForValue().set(key,JSONObject.toJSONString(o), RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                    return o;
                }
                redisTemplate.opsForValue().set(key,JSONObject.toJSONString(result), RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                return result;
            }else {
                Thread.sleep(1000);
                return cacheHit(key,methodSignature);
            }
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            //解锁
            lock.unlock();
        }
        return result;
    }

    //从缓存获取数据
    private Object cacheHit(String key,MethodSignature methodSignature) {
        //有key
        String cache = (String) redisTemplate.opsForValue().get(key);
        //判断当前字符串是否有值
        if(StringUtils.isEmpty(cache)){
            //方法的缓存类型是什么，缓存的类型就是什么
            Class returnType = methodSignature.getReturnType();
            //现在将cache的返回值类型转换成方法的返回值类型即可
            return JSONObject.parseObject(cache,returnType);
        }
        return null;
    }

}
