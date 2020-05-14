package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author gao
 * @create 2020-04-19 23:28
 * IService包含基本的CRUD，增删改查
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {

    //商品品牌的查询 带分页
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam);

    //查询商品品牌
    List<BaseTrademark> getTrademarkList();

}
