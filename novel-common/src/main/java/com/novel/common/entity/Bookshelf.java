package com.novel.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("bookshelf")
public class Bookshelf {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    private Integer lastReadChapter;

    private Integer readProgress;

    private LocalDateTime readTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
