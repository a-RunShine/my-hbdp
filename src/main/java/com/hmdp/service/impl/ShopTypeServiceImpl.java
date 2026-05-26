package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    StringRedisTemplate  stringRedisTemplate;

    @Override
    public Result queryAll() {
        String key = CACHE_SHOP_TYPE_KEY;

        String typeJSON = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(typeJSON)) {
            List<ShopType> list = JSONUtil.toList(JSONUtil.parseArray(typeJSON), ShopType.class);
            return Result.ok(list);
        }
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if(shopTypes==null||shopTypes.isEmpty()){
            return Result.fail("服务器错误！");
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypes));
        return Result.ok(shopTypes);
    }

}
