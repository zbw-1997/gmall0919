package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author Administrator
 * @create 2020-03-23 20:29
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
