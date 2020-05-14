package com.atguigu.gmall.product.service.Impl;

import com.atguigu.gmall.product.service.TestService;

import io.swagger.models.auth.In;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author gao
 * @create 2020-04-23 17:42
 */
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void testLock() {

        //创建锁
        String skuId = "18";
        String lockkey = "lock"+skuId;
        //锁的每个商品
        RLock lock = redissonClient.getLock(lockkey);
        //加锁
//        lock.lock(10,TimeUnit.SECONDS);//10秒自动解锁
        lock.lock();
        //执行业务逻辑代码
        String value = redisTemplate.opsForValue().get("num");
        if(StringUtils.isBlank(value)) {
            return;
        }
        //将value变成int
        int num = Integer.parseInt(value);
        //将num放入redis中
        redisTemplate.opsForValue().set("num",String.valueOf(++num));
        //解锁
        lock.unlock();
    }

    @Override
    public String readLock() {
        //创建读写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock rLock = readWriteLock.readLock();
        //加锁
        rLock.lock(10,TimeUnit.SECONDS);
        //获取缓存中的数据
        String msg = redisTemplate.opsForValue().get("msg");
        return msg;
    }

    @Override
    public String writeLock() {
        //创建读写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock wLock = readWriteLock.writeLock();
        //加锁
        wLock.lock(10,TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("msg",UUID.randomUUID().toString());
        return "写数据完成。。。";
    }

//    @Override
//    public void testLock() {
//
//        //声明一个uuid 作为一个value放入key的对应值当中
//        String uuid = UUID.randomUUID().toString();
//        //每一个商品对应一把锁，而这把锁是由商品的id组成
//        String skuId = "18";//商品的skuId
//        String lockkey = "Lock"+skuId;//锁住每个商品的数据
//        //从redis中获取setnx 并设置过期时间，这种具有原子性
//        Boolean Lock = redisTemplate.opsForValue().setIfAbsent(lockkey,uuid,3, TimeUnit.SECONDS);
//        if(Lock) {
//            // 查询redis中的num值
//            String value = redisTemplate.opsForValue().get("num");
//            if (StringUtils.isBlank(value)) {
//                return;//没有就直接返回
//            }
//            //有就转成int
//            int num = Integer.parseInt(value);
//            //把redis中的值加1
//            redisTemplate.opsForValue().set("num", String.valueOf(++num));
//            //redisTemplate.expire("Lock",1,TimeUnit.SECONDS); //设置过期时间
//            //判断当前线程的uuid是否与自己的uuid值相等
////            if(uuid.equals(redisTemplate.opsForValue().get("Lock"))){
////                //删除锁
////                redisTemplate.delete("Lock");
////            }
//            //使用lua脚本来锁  lua脚本不支持集群
//            String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//            //使用lua脚本来执行
//            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//            redisScript.setScriptText(script);
//            //设置返回值类型，因为删除的0封装其数据类型，默认是string类型
//            redisScript.setResultType(Long.class);
//            //第一个参数是script脚本，第二判断的key，第三是所对应的值
//            redisTemplate.execute(redisScript, Arrays.asList(lockkey),uuid);
//
//        }else {
//            try {
//                //重试
//                Thread.sleep(1000);
//                testLock();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
