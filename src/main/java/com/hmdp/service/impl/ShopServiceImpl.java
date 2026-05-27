package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
        String key = CACHE_SHOP_KEY+id;

        String shopJSON = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(shopJSON)) {
            Shop shop = JSONUtil.toBean(shopJSON, Shop.class);
            return Result.ok(shop);
        }

        Shop shop = getById(id);
        if (shop == null) {
            return Result.fail("商店不存在！");
        }

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),
                CACHE_SHOP_TTL, TimeUnit.MINUTES);

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
}
