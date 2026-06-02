package com.novel.user.controller;

import com.novel.common.annotation.AuthToken;
import com.novel.common.result.Result;
import com.novel.common.result.ResultCode;
import com.novel.common.utils.JwtUtil;
import com.novel.common.utils.UserContext;
import com.novel.user.dto.LoginRequest;
import com.novel.user.dto.RegisterRequest;
import com.novel.user.dto.UpdateUserRequest;
import com.novel.common.entity.User;
import com.novel.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Api(tags = "用户管理接口")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "注册新用户，用户名不能重复")
    public Result<String> register(@Validated @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "用户名密码登录，成功返回Token")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        User user = userService.login(request);
        if (user == null) {
            return Result.error(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        return Result.success(response);
    }

    @PostMapping("/info")
    @AuthToken
    @ApiOperation(value = "获取用户信息", notes = "获取当前登录用户信息")
    public Result<UserInfoResponse> getUserInfo() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setEmail(user.getEmail());
        return Result.success(response);
    }

    @PostMapping("/update")
    @AuthToken
    @ApiOperation(value = "更新用户信息", notes = "更新当前登录用户的信息，仅可更新昵称和邮箱")
    public Result<String> updateUserInfo(@Validated @RequestBody UpdateUserRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        userService.updateUser(userId, request);
        return Result.success("更新成功");
    }

    @ApiModel(description = "登录响应")
    public static class LoginResponse {
        @ApiModelProperty(value = "JWT认证令牌", example = "eyJhbGciOiJIUzUxMiJ9...")
        private String token;

        @ApiModelProperty(value = "用户ID", example = "1")
        private Long userId;

        @ApiModelProperty(value = "用户名", example = "zhangsan")
        private String username;

        @ApiModelProperty(value = "用户昵称", example = "张三")
        private String nickname;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    @ApiModel(description = "用户信息响应")
    public static class UserInfoResponse {
        @ApiModelProperty(value = "用户ID", example = "1")
        private Long userId;

        @ApiModelProperty(value = "用户名", example = "zhangsan")
        private String username;

        @ApiModelProperty(value = "用户昵称", example = "张三")
        private String nickname;

        @ApiModelProperty(value = "邮箱", example = "zhangsan@example.com")
        private String email;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}