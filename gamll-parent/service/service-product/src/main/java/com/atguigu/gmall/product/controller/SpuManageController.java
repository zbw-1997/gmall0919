package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-03-15 19:36
 */
@RestController
@RequestMapping("admin/product")
public class SpuManageController {
    @Autowired
    private BaseTrademarkService baseTrademarkService;

    @ApiOperation("查询基本销售属性信息")
    @GetMapping("baseSaleAttrList")
    public Result<List<BaseSaleAttr>> getSaleAttrInfo() {
        List<BaseSaleAttr> baseSaleAttrList = baseTrademarkService.getSaleAttrInfo();
        return Result.ok(baseSaleAttrList);
    }

    @ApiOperation("添加spu")
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        baseTrademarkService.saveSpuInfo(spuInfo);
        return Result.ok();
    }
}
