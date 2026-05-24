package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机格式错误！");
        }

        String code = RandomUtil.randomNumbers(6);

//        session.setAttribute("code",code);
//        session.setAttribute("phone",phone);
        redisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        log.debug("发送验证码成功!验证码：{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //获取手机号和验证码
        String code = loginForm.getCode();
        String phone = loginForm.getPhone();

        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机格式错误！");
        }

        String cacheCode = redisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        //判断是否一致
//        Object cacheCode = session.getAttribute("code");
//        Object cachePhone = session.getAttribute("phone");
        //不一致，拒绝重试
        if(cacheCode == null||!cacheCode.equals(code)){
            return Result.fail("手机号或验证码错误！");
        }
        //查询用户
        User user = query().eq("phone", phone).one();
        //不存在，注册
        if(user==null){
            user = creatUserWithPhone(phone);
        }
        //存在，保存用户到session
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().
                        setIgnoreNullValue(true).
                        setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String key=LOGIN_USER_KEY+token;
        redisTemplate.opsForHash().putAll(key,map);
        redisTemplate.expire(key,LOGIN_USER_TTL,TimeUnit.SECONDS);
        return Result.ok(token);
    }

    private User creatUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomNumbers(7));
        save(user);
        return user;
    }
}
