package com.hmdp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hmdp.utils.RedisConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopServiceImpl shopService;

    @Autowired
    CacheClient cacheClient;

    @Autowired
    RedisIdWorker redisIdWorker;

    @Test
    void contextLoads() {
        Shop shop = shopService.getById(1);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+1,shop,10L, TimeUnit.SECONDS);
    }

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void idTest() throws InterruptedException {

        Runnable task = ()->{
            for (int i = 0; i < 100; i++) {
                long order = redisIdWorker.nextId("order");
                System.out.println("id:"+order);
            }
        };

        for (int i = 0; i < 300; i++) {
            es.execute(task);
        }
        es.shutdown();
        es.awaitTermination(10,TimeUnit.SECONDS);


    }

    // ===================== SimpleRedisLock 测试 =====================

    private static final String TEST_LOCK_NAME = "test:simpleLock";

    private String lockKey() {
        return "lock:" + TEST_LOCK_NAME;
    }

    @AfterEach
    void cleanLockKey() {
        stringRedisTemplate.delete(lockKey());
    }

    @Test
    void testBasicLockAndUnlock() {
        SimpleRedisLock lock = new SimpleRedisLock(TEST_LOCK_NAME, stringRedisTemplate);

        boolean acquired = lock.tryLock(10L);
        assertThat(acquired).isTrue();
        assertThat(stringRedisTemplate.hasKey(lockKey())).isTrue();

        lock.unlock();

        assertThat(stringRedisTemplate.hasKey(lockKey())).isFalse();
    }

    @Test
    void testReacquireAfterUnlock() {
        SimpleRedisLock lock = new SimpleRedisLock(TEST_LOCK_NAME, stringRedisTemplate);

        assertThat(lock.tryLock(10L)).isTrue();
        lock.unlock();

        assertThat(lock.tryLock(10L)).isTrue();
        lock.unlock();
    }

    @Test
    void testNonOwnerCannotUnlock() {
        SimpleRedisLock lock = new SimpleRedisLock(TEST_LOCK_NAME, stringRedisTemplate);

        lock.tryLock(10L);

        stringRedisTemplate.opsForValue().set(lockKey(), "other-owner-value");

        lock.unlock();

        assertThat(stringRedisTemplate.opsForValue().get(lockKey())).isEqualTo("other-owner-value");

        stringRedisTemplate.delete(lockKey());
    }

    @Test
    void testMutualExclusion() throws Exception {
        SimpleRedisLock lockA = new SimpleRedisLock(TEST_LOCK_NAME, stringRedisTemplate);
        assertThat(lockA.tryLock(10L)).isTrue();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean acquired = new AtomicBoolean(false);

        Thread t = new Thread(() -> {
            SimpleRedisLock lockB = new SimpleRedisLock(TEST_LOCK_NAME, stringRedisTemplate);
            acquired.set(lockB.tryLock(1L));
            latch.countDown();
        });
        t.start();

        latch.await(5, TimeUnit.SECONDS);

        assertThat(acquired.get()).isFalse();

        lockA.unlock();
    }

    @Test
    void testExpiredLockNotDeleted() throws Exception {
        SimpleRedisLock lock = new SimpleRedisLock(TEST_LOCK_NAME, stringRedisTemplate);

        lock.tryLock(1L);

        Thread.sleep(1500);

        stringRedisTemplate.opsForValue().set(lockKey(), "other-owner");

        lock.unlock();

        assertThat(stringRedisTemplate.opsForValue().get(lockKey())).isEqualTo("other-owner");

        stringRedisTemplate.delete(lockKey());
    }
}
