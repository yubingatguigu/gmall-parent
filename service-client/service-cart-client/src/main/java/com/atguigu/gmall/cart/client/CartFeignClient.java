package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.cart.client.impl.CartDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author gao
 * @create 2020-05-04 19:44
 */
@FeignClient(name = "service-cart",fallback = CartDegradeFeignClient.class)
public interface CartFeignClient {

    @PostMapping("/api/cart/addToCart/{skuId}/{skuNum}")
    Result addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum);

    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable String userId);

    @GetMapping("/api/cart/loadCartCache/{userId}")
    Result loadCartCache(@PathVariable("userId") String userId);
}
