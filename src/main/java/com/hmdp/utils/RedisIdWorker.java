package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final long BEGIN_TIME_STAMP = 1780099200L;
    private static final long COUNT_BITS = 32L;
    public long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        long nowEpochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp =  nowEpochSecond - BEGIN_TIME_STAMP;

        String format = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + format);


        return timeStamp << COUNT_BITS | count;
    }

//    public static void main(String[] args) {
//        LocalDateTime localDateTime = LocalDateTime.of(2026, 5, 30, 0, 0, 0);
//        long epochSecond = localDateTime.toEpochSecond(ZoneOffset.UTC);
//        System.out.println(epochSecond);
//    }
}
