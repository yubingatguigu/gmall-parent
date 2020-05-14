package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author gao
 * @create 2020-04-25 0:35
 */
@Target(ElementType.METHOD)//注解使用在方法上
@Retention(RetentionPolicy.RUNTIME)//当前注解的生命周期
public @interface GmallCache {

    //定义一个字段，作为缓存的key使用  缓存的key右sku：组成  sku：是缓存key的一部分
    String prefix() default "cache";

}
