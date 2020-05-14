package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author gao
 * @create 2020-04-27 21:24
 */
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    //上架 将数据库的数据放入到elaseticsearch中
    @Override
    public void upperGoods(Long skuId) {
        //将实体类中的数据放入到es中
        Goods goods = new Goods();
        //给goods赋值,先查询skuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

            goods.setId(skuInfo.getId());
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setTitle(skuInfo.getSkuName());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            //通过远程调用查询商品价格
//            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//            goods.setPrice(skuPrice.doubleValue());
            goods.setCreateTime(new Date());

        //通过skuInfo中的数据查询品牌id
        BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
        if(null!=trademark){
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
        }
        //通过skuInfo获取三级分类信息
        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        if(null!=categoryView){
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory1Name(categoryView.getCategory1Name());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());
        }
        //获取平台属性的信息
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
        if(null!= attrList && attrList.size()>0){
            //循环获取里面的数据
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                //赋值平台属性对象
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                //存入elaseticsearch中的平台属性名
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                //存储平台属性值的名称，先获取平台属性值集合的数据
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                //获取平台属性值的名称
                String valueName = attrValueList.get(0).getValueName();
                searchAttr.setAttrValue(valueName);
                //将每个销售属性值存储
                return searchAttr;
            }).collect(Collectors.toList());
            goods.setAttrs(searchAttrList);
        }
        //保存
        goodsRepository.save(goods);
    }

    //下架
    @Override
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        //定义一个key
        String hotKey = "hotScore";
        //hotScore增长之后的数值  zset是排名
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        //定义规则，复合规则的时候更新一次elasticsearch
        if (hotScore%10==0){
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);
        }
    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        //检索的基本思路
        //1.先制作dsl语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);

        //2.执行dsl语句  RestHighLevelClient是es客户端工具类
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //3，获取执行的结果  response中可以获取总条数
        SearchResponseVo responseVo = parseSearchResult(response);

        //设置分页相关的数据
        responseVo.setPageSize(searchParam.getPageSize());
        responseVo.setPageNo(searchParam.getPageNo());

        //获取总条数，可以存hits.total中获取
        //responseVo.setTotal();
        //设置总页数   totalPages = (total%pagesize==0?total/pagesize:total/pagesize+1)
        long totalPages = (responseVo.getTotal()+searchParam.getPageSize()-1)/searchParam.getPageSize();
        responseVo.setTotalPages(totalPages);

        //返回数据
        return responseVo;
    }

    //制作返回结果集
    private SearchResponseVo parseSearchResult(SearchResponse response) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /**
         *  private List<SearchResponseTmVo> trademarkList;
         *  private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
         *  private List<Goods> goodsList = new ArrayList<>();
         *  private Long total;//总记录数
         *  private Integer pageSize;//每页显示的内容
         *  private Integer pageNo;//当前页面
         *  private Long totalPages;
         */
        //品牌的数据通过聚合得到的
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();

        //获取品牌id，Aggregation接口中没有桶，要用它的实现 ParsedLongTerms
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");

        //从桶中获取数据
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {

            //获取品牌的id
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            searchResponseTmVo.setTmId(Long.parseLong(((Terms.Bucket) bucket).getKeyAsString()));

            //获取品牌的名称
            Map<String, Aggregation> tmIdSubAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            //tmNameAgg品牌名称的agg，品牌数据是String
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdSubAggregationMap.get("tmNameAgg");
            //获取到品牌的名称并赋值
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            //获取品牌的logo
            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) tmIdSubAggregationMap.get("tmLogoUrlAgg");
            String tmlogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmlogoUrl);

            //返回品牌
            return searchResponseTmVo;
        }).collect(Collectors.toList());

        //赋值品牌数据
        searchResponseVo.setTrademarkList(trademarkList);

        //获取平台属性数据，从聚合中获取  attrAgg的数据类型是nested
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //获取attrIdAgg 平台属性id 数据
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        //判断桶的集合不能为空
        if(null != buckets && buckets.size()>0){
            //循环遍历数据
            List<SearchResponseAttrVo> attrsList = buckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                //获取attrNameAgg中的数据 名称的数据类型是String
                ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                //赋值平台属性的名称
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                //赋值平台属性值集合，获取attrValueAgg
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
                //获取valueBuckets的数据，将集合转换为map，map的key，就是桶的key，通过key获取里面的数据，并将数据变为一个list的集合
                List<String> valueList = valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                searchResponseAttrVo.setAttrValueList(valueList);
                //返回平台属性对象
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            //获取平台属性
            searchResponseVo.setAttrsList(attrsList);
        }

        //声明一个存储商品的集合
        ArrayList<Goods> goodsList = new ArrayList<>();
        //获取商品数据 goodsList 品牌的数据需要从查询结果集中获取
        SearchHits hits = response.getHits();
        SearchHit[] subHits = hits.getHits();//hits :[{}]
        if(null != subHits && subHits.length>0){
            //循环遍历数据
            for (SearchHit subHit : subHits) {
                //获取商品的json字符串
                String goodsJson = subHit.getSourceAsString();
                //直接将json字符串变成Goods.class
                Goods goods = JSONObject.parseObject(goodsJson, Goods.class);
                //获取商品时，如果按照商品名称查询时，商品的名称显示的时候应为高亮，从高亮中获取商品名称
                if(subHit.getHighlightFields().get("title")!=null){
                    //说明当前用户查询是按照全文检索的方式查询，将高亮的值赋值个goods [0]高亮的时候title只有一个值
                    Text[] title = subHit.getHighlightFields().get("title").getFragments();
                    goods.setTitle(title[0].string());
                }
                //添加商品到集合
                goodsList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsList);
        //设置总记录数
        searchResponseVo.setTotal(hits.totalHits);
        return searchResponseVo;
    }

    //自动生成dsl语句
    private SearchRequest buildQueryDsl(SearchParam searchParam) {

        //创建查询器 ：{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //声明一个BoolQueryBuilder 对象 query：bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //判断查询关键字
        if(StringUtils.isNotEmpty(searchParam.getKeyword())){
            //创建MatchQueryBuilder对象
            //MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("title",searchParam.getKeyword());
            //boolQueryBuilder.must(matchQueryBuilder);
            //Operator.AND：表示title中的俩个字段都必须存在  Operator.OR：有其中一个即可
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQueryBuilder.must(title);
        }

        //设置品牌 trademark= 2:华为
        String trademark = searchParam.getTrademark();
        if(StringUtils.isNotEmpty(trademark)){
            //不为空，说明用户按照品牌查询
            String[] split = StringUtils.split(trademark, ":");
            //判断分割时候的数据格式
            if(split !=null && split.length==2){
                TermQueryBuilder tmId = QueryBuilders.termQuery("tmId", split[0]);
                boolQueryBuilder.filter(tmId);
            }
        }

        //设置分类id过滤  term:表示精确取值  terms：范围取值
        if(null != searchParam.getCategory1Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }

        if(null != searchParam.getCategory2Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }

        if(null != searchParam.getCategory3Id()){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }

        //平台属性Id 平台属性名，平台属性值名称
        String[] props = searchParam.getProps();
        if(null != props && props.length>0){
            //循环遍历
            for (String prop : props) {
                //props=23:4G:运行内存
                String[] split = StringUtils.split(prop, ":");
                //判断分割之后的数据格式是否正确
                if(null != split && split.length==3){
                    //构建查询语句
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    //匹配查询
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    //将 subBoolQuery 放入到 boolQuery
                    //nested：将平台属性，属性值作为独立的数据查询
                    boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));
                    //将 boolQuery 放入到总的查询器中
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }

        //执行 Query方法
        searchSourceBuilder.query(boolQueryBuilder);

        //构建分页  开始条数
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());

        // 排序   1:hotScore 2:price 3:
        String order = searchParam.getOrder();
        if(StringUtils.isNotEmpty(order)){
            //进行数据分割
            String[] split = StringUtils.split(order, ":");
            //判断格式是否匹配
            if(null != split && split.length==2){
                //设置排序规则    定义一个排序字段
                String field = null;
                switch (split[0]){
                    case "1":
                        field="hotScore";
                        break;
                    case "2":
                        field="price";
                        break;
                }
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                //默认排序是根据热度进行降序排列
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }

        //设置高亮，声明一个高亮对象，然后设置高亮的规则
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");//商品名称
        highlightBuilder.preTags("<span style=color:red>");//前缀
        highlightBuilder.postTags("</span>");//后缀
        searchSourceBuilder.highlighter(highlightBuilder);

        //设置聚合
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")//品牌id
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))//品牌名称
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));

        //将聚合的规则添加到查询器中
        searchSourceBuilder.aggregation(termsAggregationBuilder);

        //平台属性  设置nested的聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
                            .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")//平台属性的id
                            .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))//平台属性的名称
                            .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))//平台属性值
                            ));

        //设置有效的数据，查询的时候那些字段需要显示
        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price"},null);

        //设置索引库index，type
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);

        //打印dsl 语句
        String query = searchSourceBuilder.toString();
        System.out.println("dsl:"+query);

        return searchRequest;
    }
}
