package com.hmdp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopServiceImpl shopService;

//    @Test
//    private void queryAll() {
////        String key = CACHE_SHOP_TYPE_KEY;
//        String typeJSON = stringRedisTemplate.opsForValue().get("user");
//        String jsonStr = JSONUtil.toJsonStr(typeJSON);
//        System.out.println(typeJSON);
//        System.out.println(jsonStr);
//    }

//    @Test
//     void test(){
//        shopService.saveShop2Redis(1L,10L);
//    }
}
