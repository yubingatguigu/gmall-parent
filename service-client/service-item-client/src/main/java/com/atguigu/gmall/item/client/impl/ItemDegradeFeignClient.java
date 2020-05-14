package com.atguigu.gmall.item.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.stereotype.Component;

/**
 * @author gao
 * @create 2020-04-23 14:39
 */
@Component
public class ItemDegradeFeignClient implements ItemFeignClient {

    @Override
    public Result getItem(Long skuId) {
        return null;
    }

}
