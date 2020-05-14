package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author gao
 * @create 2020-04-27 21:26
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
