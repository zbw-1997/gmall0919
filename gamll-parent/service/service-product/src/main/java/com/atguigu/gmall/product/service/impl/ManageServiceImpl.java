package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @create 2020-03-13 15:06
 */
@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;
    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        wrapper.eq("category1_id", category1Id);
        List<BaseCategory2> category2List = baseCategory2Mapper.selectList(wrapper);
        return category2List;
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();
        wrapper.eq("category2_id", category2Id);
        List<BaseCategory3> category3List = baseCategory3Mapper.selectList(wrapper);
        return category3List;
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.getBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    //添加或者修改商品属性信息
    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if (baseAttrInfo.getId() != null) {
            //修改基础属性
            baseAttrInfoMapper.updateById(baseAttrInfo);
        } else {
            //保存基础属性
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        //修改基础属性值:先删除在新增
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("attr_id", baseAttrInfo.getId());
        baseAttrValueMapper.delete(queryWrapper);
        //保存基础属性值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList != null && attrValueList.size() > 0) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    //修改属性值信息
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        List<BaseAttrValue> baseAttrValues = getAttrValueList(attrId);
        //把最新的属性值集合放到baseAttrInfo对象中
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> getSpuInfoByPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id", spuInfo.getCategory3Id());
        wrapper.orderByDesc("id");
        IPage<SpuInfo> iPage = spuInfoMapper.selectPage(spuInfoPage, wrapper);
        return iPage;
    }

    @Override
    public List<SpuImage> getSpuImageInfo(Long spuId) {
        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id", spuId));
    }

    @Override
    public List<SpuSaleAttr> spuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        //sku_info
        skuInfoMapper.insert(skuInfo);
        //sku_image
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }
        //SkuAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }
        //SkuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }

        }
        //发送消息到消息队列中
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuInfo.getId());
    }

    @Override
    public IPage<SkuInfo> getSkuInfoByPage(Page<SkuInfo> page1) {
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        IPage<SkuInfo> skuInfoIPage = skuInfoMapper.selectPage(page1, wrapper);
        return skuInfoIPage;

    }

    @Override
    @Transactional
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);
        //发送消息到消息队列中
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);

    }

    @Override
    @Transactional
    public void cancelSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);
        //发送消息到消息队列中
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);

    }
    //返回了sku基本信息和图片列表 数据库中有那个商品Id？
    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    public SkuInfo getSkuInfo(Long skuId) {
        return getSkuInfoDb(skuId);
    }

    private SkuInfo getSkuInfoByRedisson(Long skuId) {
        try {
            SkuInfo skuInfo =null;
            //定义key
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            //获取value
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if(skuInfo==null){
                //从数据库中获取，但避免缓存击穿，加锁
                //定义锁的key
                String skuLock = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(skuLock);
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if(res){
                    try {
                        skuInfo = getSkuInfoDb(skuId);
                        //避免缓存穿透
                        if(skuInfo==null){
                            SkuInfo skuInfo1 = new SkuInfo();
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SECKILL__TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }

                }else{
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }

            }else{
                if(skuInfo.getId()==null){
                    return null;
                }
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        return getSkuInfoDb(skuId);
    }

    private SkuInfo getSkuInfoByRedis(Long skuId) {
        try {
            SkuInfo skuInfo=null;
            //定义key
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            //获取value数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if(skuInfo==null){
                //直接从数据库中取数据，但是得加锁，避免缓存穿透
                //定义锁key
                String skuLock = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                //定义锁值uuid,防止误删
                String uuid = UUID.randomUUID().toString().replace("-", "");
                //上锁,同时设置失效时间，防止死锁
                Boolean res = redisTemplate.opsForValue().setIfAbsent(skuLock, uuid, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if(res){
                    skuInfo = getSkuInfoDb(skuId);
                    if(skuInfo==null){//如果数据库也无数据，避免缓存穿透
                        SkuInfo skuInfo1 = new SkuInfo();
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SECKILL__TIMEOUT,TimeUnit.SECONDS);
                    //使用lura脚本解锁
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    // 设置lua脚本返回的数据类型
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    // 设置lua脚本返回类型为Long
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    // 删除key 所对应的 value
                    redisTemplate.execute(redisScript, Arrays.asList(skuLock),uuid);
                    return skuInfo;
                }else{
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);

                }
            }else{
                // 如果用户查询的数据在数据库中根本不存在的时候第一次会将一个空对象直接放入缓存。
                // 那么第二次查询的时候，缓存中有一个空对象 防止缓存穿透
                if (null==skuInfo.getId()){
                    return null;
                }
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //避免redis宕机，直接从数据库中获取数据
        return getSkuInfoDb(skuId);
    }

    private SkuInfo getSkuInfoDb(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if(skuInfo!=null){
            List<SkuImage> skuImageList = skuImageMapper.selectList(new QueryWrapper<SkuImage>().eq("sku_id", skuId));
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }

    @Override
    @GmallCache(prefix = "SkuPrice")
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if(skuInfo!=null){
        BigDecimal price = skuInfo.getPrice();
        return price;
        }
        return new BigDecimal(0);
    }

    @Override
    @GmallCache(prefix = "CategoryView")
    public BaseCategoryView getCategoryView(Long category3Id) {
        BaseCategoryView baseCategoryView = baseCategoryViewMapper.selectById(category3Id);
        return baseCategoryView;
    }

    @Override
    @GmallCache(prefix = "SaleAttr")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }
    //选中销售属性值得状态
    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        HashMap<Object, Object> hashMap = new HashMap<>();
        List<Map> mapList = skuSaleAttrValueMapper.getSkuValueIdsMap(spuId);
        if(!CollectionUtils.isEmpty(mapList)){
            for (Map map : mapList) {
                hashMap.put(map.get("value_ids"),map.get("sku_id"));
            }
        }
        return hashMap;
    }

    @Override
    @GmallCache(prefix = "category")
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> jsonObjectList =new ArrayList<>();
        //查询分类的所有数据
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //根据category1进行分组
        Map<Long, List<BaseCategoryView>> collect1 = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //开始构建
        int index =1;
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : collect1.entrySet()) {
            //获取一级分类id
            Long categoryId1 = entry1.getKey();
            //获取一级分类名称
            String category1Name = entry1.getValue().get(0).getCategory1Name();
            //创建一个JSONOBJECT对象
            JSONObject jsonObject1 =new JSONObject();
            jsonObject1.put("index",index);
            jsonObject1.put("categoryId",categoryId1);
            jsonObject1.put("categoryName",category1Name);
            //jsonObject1.put("categoryChild",categoryChild);
            index++;
            //根据二级分类id进行分组
            Map<Long, List<BaseCategoryView>> collect2 = entry1.getValue().stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            // 声明一个二级分类的集合对象
            List<JSONObject> category2Child = new ArrayList<>();
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : collect2.entrySet()) {
                //获取二级分类id
                Long categoryId2 = entry2.getKey();
                String category2Name = entry2.getValue().get(0).getCategory2Name();
                JSONObject jsonObject2 =new JSONObject();
                jsonObject2.put("categoryId",categoryId2);
                jsonObject2.put("categoryName",category2Name);
                //jsonObject2.put("categoryChild",categoryChild);
                category2Child.add(jsonObject2);
                // 声明一个三级分类的集合对象
                List<JSONObject> category3Child = new ArrayList<>();
                entry2.getValue().stream().forEach(baseCategoryView -> {
                    Long category3Id = baseCategoryView.getCategory3Id();
                    String category3Name = baseCategoryView.getCategory3Name();
                    JSONObject jsonObject3 =new JSONObject();
                    jsonObject3.put("categoryId",category3Id);
                    jsonObject3.put("categoryName",category3Name);
                    category3Child.add(jsonObject3);
                });
                jsonObject2.put("categoryChild",category3Child);
            }
            jsonObject1.put("categoryChild",category2Child);
            jsonObjectList.add(jsonObject1);
        }

        return jsonObjectList;
    }

    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {

        BaseTrademark baseTrademark = baseTrademarkMapper.selectById(tmId);
        return baseTrademark;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {

        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.getAttrList(skuId);
        return baseAttrInfoList;
    }

    private List<BaseAttrValue> getAttrValueList(Long attrId) {
        //根据属性值id获取属性值信息
        QueryWrapper<BaseAttrValue> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_id", attrId);
        return baseAttrValueMapper.selectList(wrapper);
    }
}
