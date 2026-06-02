package com.novel.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.novel.common.entity.User;
import com.novel.common.utils.PasswordEncoderUtil;
import com.novel.user.dto.LoginRequest;
import com.novel.user.dto.RegisterRequest;
import com.novel.user.dto.UpdateUserRequest;
import com.novel.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private PasswordEncoderUtil passwordEncoder;

    public void register(RegisterRequest request) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        baseMapper.insert(user);
    }

    public User login(LoginRequest request) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        User user = baseMapper.selectOne(wrapper);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return null;
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        return user;
    }

    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        baseMapper.updateById(user);
    }
}