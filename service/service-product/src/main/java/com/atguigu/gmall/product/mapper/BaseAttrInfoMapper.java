package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author gao
 * @create 2020-04-17 20:22
 */
@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {

    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("category1Id") Long category1Id, @Param("category2Id") Long category2Id,@Param("category3Id") Long category3Id);

    List<BaseAttrInfo> selectBaseAttrInfoListBySkuId(Long skuId);
}
