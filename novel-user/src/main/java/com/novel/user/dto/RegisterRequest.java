package com.novel.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ApiModel(description = "用户注册请求")
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    @ApiModelProperty(value = "用户名", example = "testuser")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    @ApiModelProperty(value = "密码", example = "123456")
    private String password;

    @ApiModelProperty(value = "邮箱", example = "test@example.com")
    private String email;

    @ApiModelProperty(value = "昵称", example = "测试用户")
    private String nickname;
}