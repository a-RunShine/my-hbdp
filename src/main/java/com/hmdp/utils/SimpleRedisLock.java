package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate redisTemplate;


    public SimpleRedisLock(String name, StringRedisTemplate redisTemplate) {
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

    public static final String LOCK_KEY_PREFIX = "lock:";
    public static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    public static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Boolean tryLock(Long timeOut) {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY_PREFIX + name, threadId, timeOut, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(LOCK_KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }

//    @Override
//    public void unlock() {
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        String id = redisTemplate.opsForValue().get(LOCK_KEY_PREFIX + name);
//
//        if (threadId.equals(id)) {
//            redisTemplate.delete(LOCK_KEY_PREFIX + name);
//        }
//    }
}
