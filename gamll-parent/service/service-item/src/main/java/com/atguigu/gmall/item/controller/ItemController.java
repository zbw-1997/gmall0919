package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


/**
 * @author Administrator 刚才你重启
 * @create 2020-03-17 14:51
 */
@RestController
@RequestMapping("api/item")
public class ItemController {
    @Autowired
    private ItemService itemService;

    /**
     * @param skuId
     * @return
     */
    @GetMapping("{skuId}")
    public Result<Map> getSkuInfoBySkuId(@PathVariable Long skuId) {
        Map<String, Object> result = itemService.getSkuInfoBySkuId(skuId);
        return Result.ok(result);
    }


}
