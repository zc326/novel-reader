package com.novel.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "用户登录请求")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @ApiModelProperty(value = "用户名", example = "test")
    private String username;

    @NotBlank(message = "密码不能为空")
    @ApiModelProperty(value = "密码", example = "123456")
    private String password;
}