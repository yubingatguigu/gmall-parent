package com.atguigu.gmall.product.service.Impl;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author gao
 * @create 2020-04-17 20:34
 */
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {

        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        baseCategory2QueryWrapper.eq("category1_id",category1Id);
        return baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {

        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id",category2Id);
        return baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {

        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id,category2Id,category3Id);
    }

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        if(baseAttrInfo.getId()!=null){
            //修改
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else{
            //新增
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        //修改平台属性值时  先删除原来的数据在出入新数据
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueQueryWrapper);

        //得到的页面所要保存的属性值  插入新增平台属性值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

        if (attrValueList!=null && attrValueList.size()>0){
            //循环遍历
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //获取平台属性id个attrId
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        //selelct * from base_attr_info where id = attrId 根据id获取平台信息
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {

        //根据分类id，查询数据
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        spuInfoQueryWrapper.orderByDesc("id");//降序
        IPage<SpuInfo> spuInfoIPage = spuInfoMapper.selectPage(pageParam, spuInfoQueryWrapper);
        return  spuInfoIPage;

    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrs = baseSaleAttrMapper.selectList(null);
        return baseSaleAttrs;
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
        // spuInfo 商品表
        spuInfoMapper.insert(spuInfo);
        //spuSaleAttr 销售属性表
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(spuSaleAttrList!=null&&spuSaleAttrList.size()>0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
                //spuSaleAttrValue 销售属性值表
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if(spuSaleAttrValueList!=null&&spuSaleAttrValueList.size()>0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
        //spuImage 商品图片表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(spuImageList!=null&&spuImageList.size()>0){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);

            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        //获取图片信息
        QueryWrapper<SpuImage> spuImageQueryWrapper = new QueryWrapper<>();
        spuImageQueryWrapper.eq("spu_id",spuId);
        List<SpuImage> spuImages = spuImageMapper.selectList(spuImageQueryWrapper);
        return spuImages;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        //加载动态销售属性
        return spuSaleAttrMapper.selectSpuSaleAttr(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
//      skuInfo 库存单元表 --- spuInfo！
        skuInfoMapper.insert(skuInfo);
//      skuImage 库存单元图片表 --- spuImage!
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(skuImageList!=null&&skuImageList.size()>0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
//      skuSaleAttrValue sku销售属性值表{sku与销售属性值的中间表} --- skuInfo ，spuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if(skuSaleAttrValueList!=null&&skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
//      skuAttrValue sku与平台属性值的中间表 --- skuInfo ，baseAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if(skuAttrValueList!=null&&skuAttrValueList.size()>0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

        //发送消息到rabbitmq
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuInfo.getId());
    }

    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoParam) {
        //分页查询 库存单元表
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoParam, skuInfoQueryWrapper);
    }

    @Override
    public void onSale(Long skuId) {
        //上架
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(1);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        //发送消息到rabbitmq
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }

    @Override
    public void cancelSale(Long skuId) {
        //下架
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(0);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        //发送消息到rabbitmq
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);
    }

    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    @Override
    public SkuInfo getSkuInfo(Long skuId) {

        //return getSkuInfoRedisson(skuId);
        return getSkuInfoDB(skuId);
    }

    //利用redisson分布式锁查询数据
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //定义一个商品的key = sku:+skuId+:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            //从缓存中获取数据
            skuInfo= (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if(null == skuInfo){//走数据库
                //利用redisson定义分布式锁
                String lockkey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockkey);
                //加锁
                boolean flag = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if(flag){
                    try{
                        //执行业务逻辑代码
                        skuInfo = getSkuInfoDB(skuId);
                        if(null==skuInfo){//为了防止击穿，创建一个空对象，数据库的数据为空
                            SkuInfo skuInfo1 = new SkuInfo();
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        //不为空
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SECKILL__TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e){
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                }else {
                    //为获取到分布式锁，其他线程等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //记录缓存数据宕机的日志
                        e.printStackTrace();
                    }
                    //调用方法
                    return getSkuInfo(skuId);
                }
            }else {
                //查询一个在数据库不存在的数据时，我们会放入一个空对象到缓存中，其实我们想要的不是一个空对象而是对象的属性也是右值的
                if(null == skuInfo.getId()){
                    return null;
                }
                //走缓存
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //为了防止缓存宕机
        return getSkuInfoRedis(skuId);
    }

    //利用redis 获取分布式锁 查询数据
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            //整合流程

            //定义一个商品的key = sku:+skuId+:info
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            //从缓存中获取数据
            skuInfo= (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if(skuInfo==null){
                //走DB，查询数据放入缓存,注意添加分布式锁
                //定义一个分布式锁 key = sku:+skuId+:lock
                String lockkey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                //获取一个随机字符串
                String uuid = UUID.randomUUID().toString();
                //为了防止缓存击穿，执行分布式锁命令
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockkey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                //判断锁是否添加成功
                if(isExist){
                    //获取到分布式锁，走缓存DB，查询数据放入缓存
                    System.out.println("创建分布式否成功");
                    skuInfo = getSkuInfoDB(skuId);
                    //判断数据库中的数据是否为空
                    if(skuInfo==null){
                        //为了防止缓存穿透，赋值一个空对象到缓存中
                        SkuInfo skuInfo1 = new SkuInfo();
                        //放入缓存的超时时间,空对象的超时时间为1天，最好不宜太长
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    //从数据库中查出的数据不为空
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    //删除锁 ，定义lua脚本
                    String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setScriptText(script);
                    redisScript.setResultType(Long.class);
                    //根据锁的key，找锁的值，进行删除
                    redisTemplate.execute(redisScript, Arrays.asList(lockkey),uuid);
                    return skuInfo;
                }else {
                    //为获取到分布式锁，其他线程等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //记录缓存数据宕机的日志
                        e.printStackTrace();
                    }
                    //调用方法
                    return getSkuInfo(skuId);
                }
            }else{
                //查询一个在数据库不存在的数据时，我们会放入一个空对象到缓存中，其实我们想要的不是一个空对象而是对象的属性也是右值的
                if(null == skuInfo.getId()){
                    return null;
                }
                //走缓存
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getSkuInfoDB(skuId);
    }

    //根据skuId查询数据库中的数据
    private SkuInfo getSkuInfoDB(Long skuId) {
        //根据skuId获取SkuInfo
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if(skuInfo!=null){
            //根据skuId查询图片列表结合\
            QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
            skuImageQueryWrapper.eq("sku_id",skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageQueryWrapper);
            skuInfo.setSkuImageList(skuImageList);
        }

        return skuInfo;
    }

    @GmallCache(prefix = "categoryViewByCategory3Id:")
    @Override
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        //根据三级分类id查询分类信息
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @GmallCache(prefix = "skuPrice:")
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        //根据skuId 查询价格
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (null!=skuInfo){
            return skuInfo.getPrice();//返回价格
        }else {
            return new BigDecimal("0");//返回初始值
        }
    }

    @GmallCache(prefix = "spuSaleAttrListCheckBySku:")
    @Override
    public List<SpuSaleAttr> getselectSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        //根据spuId ，skuId 获取销售属性和销售属性值的信息
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    @GmallCache(prefix = "skuValueIdsMap:")
    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        //根据spuId 切换销售属性
        Map<Object, Object> hashMap = new HashMap<>();
        List<Map> mapList = skuSaleAttrValueMapper.getSaleAttrValuesBySpu(spuId);
        if (mapList!=null && mapList.size()>0){
            for (Map map : mapList) {
                hashMap.put(map.get("value_ids"),map.get("sku_id"));
            }
        }
        return hashMap;
    }

    @GmallCache(prefix = "baseCategoryList:")
    @Override
    public List<JSONObject> getBaseCategoryList() {
        //声明一个集合
        ArrayList<JSONObject> list = new ArrayList<>();
        //获取所有的分类信息，组装数据，条件分类id，为主外键    封装数据在视图中
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //遍历集合，按照一级分类id 进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //定义一个index
        int index = 1;
        //获取一级分类数据，一级分类id，一级分类名称
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            //获取一级分类id
            Long category1Id = entry1.getKey();
            //创建一个一级的JSONObject对象
            JSONObject category1 = new JSONObject();
            category1.put("categoryId",category1Id);
            category1.put("index",index);
            //获取一级分类下面的所有集合
            List<BaseCategoryView> category2List = entry1.getValue();
            String category1Name = category2List.get(0).getCategory1Name();
            category1.put("categoryName",category1Name);
            //迭代index
            index++;
            //获取二级分类数据
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //声明二级分类对象的集合
            ArrayList<JSONObject> category2Child  = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                // 获取二级分类Id
                Long category2Id = entry2.getKey();
                //创建一个二级的JSONObject对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                // 获取二级分类下的所有集合
                List<BaseCategoryView> category3List = entry2.getValue();
                category2.put("categoryName",category3List.get(0).getCategory2Name());
                //将二级分类的数据添加到二级分类的集合中
                category2Child.add(category2);
                //获取三级分类信息
                ArrayList<JSONObject> category3Child = new ArrayList<>();
                //遍历三级分类的数据
                category3List.stream().forEach(category3View->{
                    //声明一个三级分类的对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",category3View.getCategory3Id());
                    category3.put("categoryName",category3View.getCategory3Name());
                    //将三级分类数据添加到三级分类集合中
                    category3Child.add(category3);
                });
                //二级中的categoryChild添加到三级分类数据
                category2.put("categoryChild",category3Child);
            }
            //二级中的categoryChild添加到一级分类数据
            category1.put("categoryChild",category2Child);
            //将所有的数据添加到一级中
            list.add(category1);
        }
        return list;
    }

    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        //根据品牌id查询数据
       return baseTrademarkMapper.selectById(tmId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        //根据sku集合来查询数据
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }


    //select * from base_attr_value where attr_id =id 根据平台属性值集合对象获取平台信息
    private List<BaseAttrValue> getAttrValueList(Long attrId){
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",attrId);
        return baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
    }
}
