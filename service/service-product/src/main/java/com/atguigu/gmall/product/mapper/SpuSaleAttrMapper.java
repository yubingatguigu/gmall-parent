package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author gao
 * @create 2020-04-20 1:50
 */
@Mapper
public interface SpuSaleAttrMapper extends BaseMapper<SpuSaleAttr> {

    List<SpuSaleAttr> selectSpuSaleAttr(Long spuId);

    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(Long skuId, Long spuId);
}
