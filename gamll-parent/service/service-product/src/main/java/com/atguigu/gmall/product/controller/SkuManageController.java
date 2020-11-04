package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Administrator
 * @create 2020-03-16 18:38
 */
@RestController
@RequestMapping("admin/product")
public class SkuManageController {
    @Autowired
    private ManageService manageService;

    @ApiOperation(value = "根据spuId查询图片信息")
    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId) {
        return Result.ok(manageService.getSpuImageInfo(spuId));
    }

    @ApiOperation(value = "根据spuId获取销售属性")
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId) {
        return Result.ok(manageService.spuSaleAttrList(spuId));
    }

    @ApiOperation(value = "添加sku")
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    @ApiOperation(value = "获取sku分页列表")
    @GetMapping("list/{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit) {
        Page<SkuInfo> page1 = new Page<>(page, limit);
        IPage<SkuInfo> skuInfoIPage = manageService.getSkuInfoByPage(page1);
        return Result.ok(skuInfoIPage);
    }

    @ApiOperation(value = "商品上架")
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable("skuId") Long skuId) {
        manageService.onSale(skuId);
        return Result.ok();
    }

    @ApiOperation(value = "商品下架")
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable("skuId") Long skuId) {
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}
