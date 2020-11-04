package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Administrator
 * @create 2020-03-27 12:46
 */
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        //select * from table where skuId=? and userId=?
        //根据skuId 和 userId 在数据库中查询商品对象
        String cartKey = getCartKey(userId);
        // 判断缓存中是否有cartKey，先加载数据库中的数据放入缓存！
        if (!redisTemplate.hasKey(cartKey)) {
            loadCartCache(userId);
        }
        QueryWrapper<CartInfo> wrapper =new QueryWrapper<>();
        wrapper.eq("sku_id",skuId).eq("user_id",userId);
        CartInfo cartInfo = cartInfoMapper.selectOne(wrapper);
        //判断是否是第一次添加到购物车
        if(cartInfo!=null){
            //第二次添加到购物车，添加数量
            cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
            //实时价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfo.setCartPrice(skuPrice);
            //更新数据库
            cartInfoMapper.updateById(cartInfo);
        }else{
            //第一次加入购物车
            CartInfo cartInfo1 =new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuId(skuId);
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuName(skuInfo.getSkuName());
            //添加到数据库
            cartInfoMapper.insert(cartInfo1);
            cartInfo=cartInfo1;
        }
        //存入缓存中，为了再次查询时候快速查询
        //存对象在redis中用hash数据类型
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfo);
        getCartExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList =new ArrayList<>();
        //无论登录还是未登录都先查询缓存
        // 未登录：临时用户Id 获取未登录的购物车数据
        if (StringUtils.isEmpty(userId)) {
            cartInfoList = this.getCartList(userTempId);
            return cartInfoList;
        }

    /*
         1. 准备合并购物车
         2. 获取未登录的购物车数据
         3. 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同 则数量相加，合并完成之后，删除未登录的数据！
         4. 如果未登录购物车没有数据，则直接显示已登录的数据
          */
        //已登录
        if (!StringUtils.isEmpty(userId)) {
            List<CartInfo> cartInfoArrayList = this.getCartList(userTempId);
            if (!CollectionUtils.isEmpty(cartInfoArrayList)) {
                // 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同
                cartInfoList = this.mergeToCartList(cartInfoArrayList, userId);
                // 删除未登录购物车数据
                this.deleteCartList(userTempId);
            }

            // 如果未登录购物车中没用数据！
            if (StringUtils.isEmpty(userTempId) || CollectionUtils.isEmpty(cartInfoArrayList)) {
                // 根据什么查询？userId
                cartInfoList = this.getCartList(userId);
            }
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //修改数据库
        CartInfo cartInfo =new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> wrapper =new QueryWrapper<>();
        wrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.update(cartInfo,wrapper);
        //修改缓存
        String cartKey = getCartKey(userId);
        //获取到value
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);

        if(boundHashOperations.hasKey(skuId.toString())){
            CartInfo cartInfo1 = (CartInfo) boundHashOperations.get(skuId.toString());
            cartInfo1.setIsChecked(isChecked);
            //写入缓存
            boundHashOperations.put(skuId.toString(),cartInfo1);
            getCartExpire(cartKey);
        }
    }
    //删除购物车
    @Override
    public void deleteCart(String userId, Long skuId) {
        //删除数据库
        QueryWrapper<CartInfo> wrapper =new QueryWrapper<>();
        wrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.delete(wrapper);
        //删除缓存数据
        String cartKey = getCartKey(userId);
        BoundHashOperations hashOperations = redisTemplate.boundHashOps(cartKey);
        if(hashOperations.hasKey(skuId.toString())){
            hashOperations.delete(skuId.toString());
        }
    }
    //根据用户Id 查询购物车列表
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList =new ArrayList<>();
        //从数据库中查找
        String cartKey = getCartKey(userId);
        List<CartInfo> cartCacheList = redisTemplate.opsForHash().values(cartKey);
        if(!CollectionUtils.isEmpty(cartCacheList)){
            for (CartInfo cartInfo : cartCacheList) {
                if(cartInfo.getIsChecked().intValue()==1){
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }

    //删除未登录购物车数据
    private void deleteCartList(String userTempId) {
        // 删除数据库
        // delete from userInfo where userId = ?userTempId
        QueryWrapper queryWrapper = new QueryWrapper<CartInfo>();
        queryWrapper.eq("user_id", userTempId);
        cartInfoMapper.delete(queryWrapper);
        //删除缓存
        String cartKey = getCartKey(userTempId);
        Boolean flag = redisTemplate.hasKey(cartKey);
        if (flag){
            redisTemplate.delete(cartKey);
        }
    }

    //合并数据
    private List<CartInfo> mergeToCartList(List<CartInfo> cartListNoLogin, String userId) {
        //获取登录的数据
        List<CartInfo> cartListLogin = getCartList(userId);
        //需要根据商品id判断未登录数据中是否有重样的商品，所以把登录的数据转换成key-value的集合
        Map<Long, CartInfo> cartLoginMap = cartListLogin.stream().collect(Collectors.toMap(CartInfo::getSkuId,cartInfo -> cartInfo));
        for (CartInfo cartNoLogin : cartListNoLogin) {
            Long skuId = cartNoLogin.getSkuId();
            if(cartLoginMap.containsKey(skuId)){//如果有同样的商品
                //获取登录的数据
                CartInfo cartInfoLogin = cartLoginMap.get(skuId);
                //进行数量增长
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartNoLogin.getSkuNum());
                //合并勾选，以未登录的勾选为准
                if(cartNoLogin.getIsChecked().intValue()==1){
                    cartInfoLogin.setIsChecked(1);
                }
                cartInfoMapper.updateById(cartInfoLogin);//相同的商品进行更新
            }else{
                cartNoLogin.setId(null);//？？？
                cartNoLogin.setUserId(userId);
                cartInfoMapper.insert(cartNoLogin);//不同的商品进行添加
            }
        }
            //汇总数据就是再查一遍数据库并且存到redis中
            List<CartInfo> loadCartCache = loadCartCache(userId);
        return loadCartCache;
    }

    //获取购物车列表
    private List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList =new ArrayList<>();
        if(StringUtils.isEmpty(userId)) {
            return cartInfoList;
        }
        //从缓存中查
        String key =getCartKey(userId);
        cartInfoList = redisTemplate.opsForHash().values(key);
        if(!CollectionUtils.isEmpty(cartInfoList)){
            //按更新时间降序排列
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {

                    return o1.getId().toString().compareTo(o2.getId().toString());
                }
            });
        }
        //从数据库中查
        cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }
    //从数据库查询数据并存入缓存中
    @Override
    public List<CartInfo> loadCartCache(String userId) {
        QueryWrapper<CartInfo> wrapper =new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(cartInfoList)){
            return cartInfoList;
        }
        //放入缓存中
        Map<String,CartInfo> map =new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            //若放了一周，reidis的过期时间到了，但是在此中间改过价格，需要重新附上实时价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);
            map.put(cartInfo.getSkuId().toString(),cartInfo);
        }
        String cartKey = getCartKey(userId);
        //用putAll存的效率比put一条一条存储的效率快
        redisTemplate.opsForHash().putAll(cartKey,map);
        //设置过期时间
        getCartExpire(cartKey);
        return cartInfoList;
    }

    //设置过期时间
    private void getCartExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
    //定义key
    private String getCartKey(String userId) {
        String key = RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
        return key;
    }
}
