package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveNumberCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
        //Shop shop = queryWithPassThrough(id);
        //缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

    @Override
    public Result myUpdate(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空！");
        }

        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);

        return Result.ok(shop);
    }

    private Shop queryWithMutex(long id) {
        String key = CACHE_SHOP_KEY+id;

        String shopJSON = stringRedisTemplate.opsForValue().get(key);
        //缓存命中
        if (StrUtil.isNotBlank(shopJSON)) {
            return JSONUtil.toBean(shopJSON, Shop.class);
        }

        if (shopJSON != null) {
            return null;
        }

        Shop shop = null;
        String lockKey = "lock:shop:" + id;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }

            shopJSON = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopJSON)) {
                return JSONUtil.toBean(shopJSON, Shop.class);
            }

            shop = getById(id);
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(key,"",CACHE_SHOP_TTL, TimeUnit.MINUTES);
                return null;
            }

            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }

        return shop;
    }

    private Shop queryWithPassThrough(long id) {
        String key = CACHE_SHOP_KEY+id;

        String shopJSON = stringRedisTemplate.opsForValue().get(key);
        //缓存命中
        if (StrUtil.isNotBlank(shopJSON)) {
            return JSONUtil.toBean(shopJSON, Shop.class);
        }

        if (shopJSON != null) {
            return null;
        }

        Shop shop = getById(id);
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_NULL_TTL, TimeUnit.MINUTES);

        return shop;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private  void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
