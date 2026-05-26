package com.hmdp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Test
    public void queryAll() {
//        String key = CACHE_SHOP_TYPE_KEY;
        String typeJSON = stringRedisTemplate.opsForValue().get("user");
        String jsonStr = JSONUtil.toJsonStr(typeJSON);
        System.out.println(typeJSON);
        System.out.println(jsonStr);


    }
}
