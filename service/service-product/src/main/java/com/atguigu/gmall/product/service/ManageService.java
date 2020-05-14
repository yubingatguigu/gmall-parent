package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author gao
 * @create 2020-04-17 20:24
 */
public interface ManageService {

    //查询一级分类
    List<BaseCategory1> getCategory1();

    //根据一级分类查询二级分类
    List<BaseCategory2> getCategory2(Long category1Id);

    //根据二级分类查询三级分类
    List<BaseCategory3> getCategory3(Long category2Id);

    //查询平台属性，可根据一级分类，二级分类，三级分类
    List<BaseAttrInfo> getAttrInfoList(Long category1Id,Long category2Id,Long category3Id);

    //保存平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    //修改平台属性
    BaseAttrInfo getAttrInfo(Long attrId);

    //分页查询  根据分类id
    IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam,SpuInfo spuInfo);

    //销售属性
    List<BaseSaleAttr> getBaseSaleAttrList();

    //保存
    void saveSpuInfo(SpuInfo spuInfo);

    //获取图片信息
    List<SpuImage> getSpuImageList(Long spuId);

    //加载销售属性
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    //保存
    void saveSkuInfo(SkuInfo skuInfo);

    //分页查询库存单元表
    IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoParam);

    //上架
    void onSale(Long skuId);

    //下架
    void cancelSale(Long skuId);

    //根据skuId 获取SkuInfo
    SkuInfo getSkuInfo(Long skuId);

    //根据三级分类id 查询分类信息
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    //根据skuId 查询价格
    BigDecimal getSkuPrice(Long skuId);

    //根据spuId ，skuId 获取销售属性和销售属性值的信息
    List<SpuSaleAttr> getselectSpuSaleAttrListCheckBySku(@Param("skuId") Long skuId,@Param("spuId") Long spuId);

    //根据spuId 切换销售属性
    Map getSkuValueIdsMap(Long spuId);

    //获取全部分类信息
    List<JSONObject> getBaseCategoryList();

    //通过品牌id，查询数据
    BaseTrademark getTrademarkByTmId(Long tmId);

    //通过sku集合来查询数据
    List<BaseAttrInfo> getAttrList(Long skuId);
}
