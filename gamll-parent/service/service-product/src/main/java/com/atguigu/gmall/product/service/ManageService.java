package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-03-13 14:58
 */
public interface ManageService {
    /**
     * 查询一级分类
     *
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 查询二级分类
     *
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 查询三级分类
     *
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 查询平台属性数据
     *
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存属性信息
     *
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    BaseAttrInfo getAttrInfo(Long attrId);

    IPage<SpuInfo> getSpuInfoByPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo);

    /**
     * //根据spu_id查询spu_image信息
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageInfo(Long spuId);

    List<SpuSaleAttr> spuSaleAttrList(Long spuId);

    void saveSkuInfo(SkuInfo skuInfo);

    IPage<SkuInfo> getSkuInfoByPage(Page<SkuInfo> page1);

    void onSale(Long skuId);

    void cancelSale(Long skuId);

    SkuInfo getSkuInfo(Long skuId);

    BigDecimal getSkuPrice(Long skuId);

    BaseCategoryView getCategoryView(Long category3Id);

    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId,Long spuId);

    Map getSkuValueIdsMap(Long spuId);

    //获取全部分类信息
    List<JSONObject> getBaseCategoryList();
    //通过品牌id查询数据
    BaseTrademark getTrademarkByTmId(Long tmId);
    //根据skuId查询平台属性
    List<BaseAttrInfo> getAttrList(Long skuId);

}
