package com.novel.bookshelf.controller;

import com.novel.bookshelf.service.BookshelfService;
import com.novel.common.annotation.AuthToken;
import com.novel.common.entity.Bookshelf;
import com.novel.common.exception.BusinessException;
import com.novel.common.result.Result;
import com.novel.common.result.ResultCode;
import com.novel.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/bookshelf")
@Api(tags = "书架管理接口")
public class BookshelfController {

    @Autowired
    private BookshelfService bookshelfService;

    @PostMapping("/add")
    @AuthToken
    @ApiOperation(value = "添加图书到书架", notes = "将图书添加到当前用户的书架，需要认证")
    public Result<String> addToBookshelf(@RequestBody AddToBookshelfRequest request) {
        Long userId = requireUserId();

        if (bookshelfService.isInBookshelf(userId, request.getBookId())) {
            throw new BusinessException(ResultCode.BOOK_ALREADY_IN_BOOKSHELF);
        }

        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setUserId(userId);
        bookshelf.setBookId(request.getBookId());
        bookshelf.setLastReadChapter(1);
        bookshelf.setReadProgress(0);

        bookshelfService.save(bookshelf);

        return Result.success("添加成功");
    }

    @GetMapping("/list")
    @AuthToken
    @ApiOperation(value = "获取书架列表", notes = "获取当前用户的书架列表，需要认证")
    public Result<List<Bookshelf>> getBookshelf() {
        Long userId = requireUserId();
        List<Bookshelf> result = bookshelfService.getByUserId(userId);
        return Result.success(result);
    }

    @PostMapping("/remove")
    @AuthToken
    @ApiOperation(value = "从书架移除图书", notes = "从当前用户的书架中移除图书，需要认证")
    public Result<String> removeFromBookshelf(@RequestBody RemoveFromBookshelfRequest request) {
        Long userId = requireUserId();

        boolean success = bookshelfService.removeFromBookshelf(userId, request.getBookId());
        if (!success) {
            throw new BusinessException(ResultCode.BOOKSHELF_NOT_IN);
        }

        return Result.success("移除成功");
    }

    @PostMapping("/progress")
    @AuthToken
    @ApiOperation(value = "更新阅读进度", notes = "更新当前用户的阅读进度，需要认证")
    public Result<String> updateReadProgress(@RequestBody UpdateProgressRequest request) {
        Long userId = requireUserId();

        bookshelfService.updateProgress(userId, request.getBookId(),
                request.getChapterId().intValue(), request.getProgress());

        return Result.success("更新成功");
    }

    @ApiModel(description = "添加到书架请求")
    public static class AddToBookshelfRequest {
        @ApiModelProperty(value = "图书ID", required = true, example = "1")
        @NotNull(message = "图书ID不能为空")
        private Long bookId;

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
    }

    @ApiModel(description = "从书架移除请求")
    public static class RemoveFromBookshelfRequest {
        @ApiModelProperty(value = "图书ID", required = true, example = "1")
        @NotNull(message = "图书ID不能为空")
        private Long bookId;

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
    }

    @ApiModel(description = "更新阅读进度请求")
    public static class UpdateProgressRequest {
        @ApiModelProperty(value = "图书ID", required = true, example = "1")
        @NotNull(message = "图书ID不能为空")
        private Long bookId;

        @ApiModelProperty(value = "章节ID", required = true, example = "1")
        @NotNull(message = "章节ID不能为空")
        private Long chapterId;

        @ApiModelProperty(value = "阅读进度百分比", example = "50")
        private Integer progress;

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public Long getChapterId() { return chapterId; }
        public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }
    }

    /**
     * 从 UserContext 获取当前登录用户ID，缺失则抛出业务异常（由全局处理器统一返回401）。
     */
    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}