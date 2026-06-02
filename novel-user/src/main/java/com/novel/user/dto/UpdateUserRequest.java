package com.novel.user.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "用户更新请求")
public class UpdateUserRequest {

    @ApiModelProperty(value = "用户昵称", example = "张三")
    private String nickname;

    @ApiModelProperty(value = "邮箱", example = "zhangsan@example.com")
    private String email;
}
