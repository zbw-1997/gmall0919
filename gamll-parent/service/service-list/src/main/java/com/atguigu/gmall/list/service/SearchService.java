package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

/**
 * @author Administrator
 * @create 2020-03-23 20:15
 */
public interface SearchService {
    //商品上架
    void upperGoods(Long skuId);

    //商品下架
    void lowerGoods(Long skuId);
    //商品热度
    void incrHotScore(Long skuId);
    //搜索列表
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
