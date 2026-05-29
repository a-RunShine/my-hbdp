package com.hmdp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopServiceImpl shopService;

    @Autowired
    CacheClient cacheClient;

    @Test
    void contextLoads() {
        Shop shop = shopService.getById(1);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+1,shop,10L, TimeUnit.SECONDS);
    }
}
