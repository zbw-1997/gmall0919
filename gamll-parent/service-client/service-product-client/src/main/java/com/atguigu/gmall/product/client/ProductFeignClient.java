package com.atguigu.gmall.product.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-03-18 10:55
 */
@FeignClient(name = "service-product" ,fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {
    @ApiOperation(value = "根据skuId查询sku基本信息和图片列表")
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId);
    @ApiOperation(value = "根据skuId查询商品价格")
    @GetMapping("/api/product/inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);
    @ApiOperation(value = "通过三级分类id查询分类信息")
    @GetMapping("/api/product/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);
    @ApiOperation(value = "获取销售属性信息")
    @GetMapping("/api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId);
    @ApiOperation(value = "根据spuId 查询map 集合属性")
    @GetMapping("/api/product/inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId);

    @ApiOperation(value = "查询所有分类信息")
    @GetMapping("/api/product/getBaseCategoryList")
    public Result getBaseCategoryList();

    @ApiOperation(value = "通过品牌Id 集合来查询数据")
    @GetMapping("/api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademark(@PathVariable("tmId")Long tmId);

    @ApiOperation(value = "通过skuId 集合来查询数据")
    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId);
}
