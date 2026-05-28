package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


@Component
public class CacheClient {

    private StringRedisTemplate stringRedisTemplate;
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    private <R,ID> R queryWithPassThrough(
            String preFix, ID id, Class<R> type, Function<ID,R> dbFunction,Long time, TimeUnit unit) {
        String key = preFix+id;

        String json = stringRedisTemplate.opsForValue().get(key);
        //缓存命中
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        if (json != null) {
            return null;
        }

        R r = dbFunction.apply(id);
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key,"",time, unit);
            return null;
        }

        this.set(key,r,time,unit);

        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    private <R,ID> R queryWithLogicalExpire(
            String keyPreFix, ID id, Class<R> type, Function<ID,R> dbFunction,Long time, TimeUnit unit,String lockPreFix) {
        String key = keyPreFix + id;

        String json = stringRedisTemplate.opsForValue().get(key);
        //缓存未命中
        if (StrUtil.isBlank(json)) {
            return null;
        }
        // 命中
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            // 否，返回
            return r;
        }
        // 是，拿锁
        String lockKey = lockPreFix + id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
            // 拿到，新建线程重构缓存
            //double check
            json = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(json)) {
                // key被删了，直接从数据库重建
                r = dbFunction.apply(id);
                if (r != null) {
                    setWithLogicalExpire(key,r,time,unit);
                }
                unLock(lockKey);
                return r;
            }

            redisData = JSONUtil.toBean(json,RedisData.class);
            expireTime = redisData.getExpireTime();
            r = JSONUtil.toBean((JSONObject)redisData.getData(),type);
            if (!LocalDateTime.now().isAfter(expireTime)){
                return r;
            }
            CACHE_REBUILD_EXECUTOR.submit(()->{
                //重建
                try {
                    R newR = dbFunction.apply(id);
                    if (newR !=null) {
                        setWithLogicalExpire(key, newR,time,unit);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }

        // 不管是否成功重建缓存，返回旧信息
        return r;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private  void unLock(String key){
        stringRedisTemplate.delete(key);
    }


}
