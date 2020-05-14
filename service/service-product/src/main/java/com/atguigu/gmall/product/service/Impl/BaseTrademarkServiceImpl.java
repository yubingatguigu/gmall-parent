package com.atguigu.gmall.product.service.Impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author gao
 * @create 2020-04-19 23:29
 */
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper, BaseTrademark> implements BaseTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam) {
        //商品品牌的分页查询
        QueryWrapper<BaseTrademark> baseTrademarkQueryWrapper = new QueryWrapper<>();
        baseTrademarkQueryWrapper.orderByDesc("id");
        return baseTrademarkMapper.selectPage(pageParam,baseTrademarkQueryWrapper);
    }

    @Override
    public List<BaseTrademark> getTrademarkList() {
        //查询商品品牌
        List<BaseTrademark> baseTrademarks = baseTrademarkMapper.selectList(null);
        return baseTrademarks;
    }

}
