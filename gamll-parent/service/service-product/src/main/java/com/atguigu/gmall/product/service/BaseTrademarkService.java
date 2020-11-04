package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SpuInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-03-15 9:34
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {
    /**
     * 品牌列表分页
     * @param page
     * @return
     */
    IPage<BaseTrademark> getTrademarkByPage(Page<BaseTrademark> page);

    //查询所有销售属性信息
    List<BaseSaleAttr> getSaleAttrInfo();

    //查询品牌属性信息
    List<BaseTrademark> getTrademarkList();

    //保存spuInfo
    void saveSpuInfo(SpuInfo spuInfo);
}
