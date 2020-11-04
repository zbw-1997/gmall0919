package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-03-13 16:15
 */
@Api(tags = "商品基础属性")
@RestController
@RequestMapping("admin/product")
//@CrossOrigin
public class ManageController {
    @Autowired
    private ManageService manageService;

    @ApiOperation(value = "查询一级分类")
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1() {
        List<BaseCategory1> category1List = manageService.getCategory1();
        return Result.ok(category1List);
    }

    @ApiOperation(value = "根据一级分类id,查询二级分类")
    @GetMapping("getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable Long category1Id) {
        List<BaseCategory2> category2 = manageService.getCategory2(category1Id);
        return Result.ok(category2);
    }

    @ApiOperation(value = "根据二级分类id,查询三级分类")
    @GetMapping("getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable Long category2Id) {
        List<BaseCategory3> category3 = manageService.getCategory3(category2Id);
        return Result.ok(category3);
    }

    @ApiOperation(value = "查询商品基础属性")
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(@PathVariable Long category1Id,
                                                   @PathVariable Long category2Id,
                                                   @PathVariable Long category3Id) {
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(attrInfoList);
    }

    @ApiOperation(value = "添加商品基础属性和基础属性值")
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    @ApiOperation(value = "根据属性值id获取属性值信息（修改属性值信息）")
    @GetMapping("getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable Long attrId) {
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return Result.ok(attrValueList);
    }

    @ApiOperation(value = "根据三级分类id分页查询商品信息")
    @GetMapping("{page}/{limit}")
    public Result index(@ApiParam(name = "page", value = "当前页", required = true) @PathVariable Long page,
                        @ApiParam(name = "limit", value = "每页记录数", required = true) @PathVariable Long limit,
                        @ApiParam(name = "spuInfo", value = "查询对象", required = false) SpuInfo spuInfo) {
        Page<SpuInfo> spuInfoPage = new Page<>(page, limit);
        IPage<SpuInfo> iPage = manageService.getSpuInfoByPage(spuInfoPage, spuInfo);
        return Result.ok(iPage);
    }

}
