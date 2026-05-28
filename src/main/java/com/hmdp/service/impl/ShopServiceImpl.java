package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        // 缓存穿透
        //Shop shop = queryWithPassThrough(id);
        // 互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);
        // 逻辑过期缓存击穿
        Shop shop = queryWithLogicalExpire(id);
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

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    private Shop queryWithLogicalExpire(long id) {
        String key = CACHE_SHOP_KEY+id;

        String shopJSON = stringRedisTemplate.opsForValue().get(key);
        //缓存命中
        if (StrUtil.isBlank(shopJSON)) {
            return null;
        }
        // 命中
        RedisData redisData = JSONUtil.toBean(shopJSON, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            // 否，返回
            return shop;
        }
        // 是，拿锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
            // 拿到，新建线程重构缓存
            //double check
            shopJSON = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(shopJSON)) {
                // key被删了，直接从数据库重建
                shop = getById(id);
                try {
                    if (shop != null) {
                        saveShop2Redis(id, 20L);
                    }
                } catch (InterruptedException e) {
                    throw  new RuntimeException();
                } finally {
                    unLock(lockKey);
                }
                return shop;
            }

            redisData = JSONUtil.toBean(shopJSON,RedisData.class);
            expireTime = redisData.getExpireTime();
            shop = JSONUtil.toBean((JSONObject)redisData.getData(),Shop.class);
            if (!LocalDateTime.now().isAfter(expireTime)){
                return shop;
            }
            CACHE_REBUILD_EXECUTOR.submit(()->{
                //重建
                try {
                    this.saveShop2Redis(id,20L);
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }finally {
                    unLock(lockKey);
                }
            });
        }

        // 不管是否成功重建缓存，返回旧信息
        return shop;
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

    private void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        Shop shop = getById(id);
        Thread.sleep(200);

        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }
}
