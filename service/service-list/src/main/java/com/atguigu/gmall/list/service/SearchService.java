package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

/**
 * @author gao
 * @create 2020-04-27 21:22
 */
public interface SearchService {

    //上架商品列表
    void upperGoods(Long skuId);

    //下架商品列表
    void lowerGoods(Long skuId);

    //更新热度
    void incrHotScore(Long skuId);

    //根据用户输入条件查询数据
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
