package com.novel.book.controller;

import com.novel.book.service.BookService;
import com.novel.common.annotation.AuthToken;
import com.novel.common.entity.Book;
import com.novel.common.entity.Chapter;
import com.novel.common.result.Result;
import com.novel.common.result.ResultCode;
import com.novel.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/book")
@Api(tags = "图书管理接口")
public class BookController {

    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024;

    @Autowired
    private BookService bookService;

    @PostMapping("/upload")
    @AuthToken
    @ApiOperation(value = "上传图书", notes = "上传TXT格式的小说文件，支持最大200MB，需要认证")
    public Result<Book> uploadBook(
            @ApiParam(value = "图书文件", required = true)
            @RequestParam("file") @NotNull MultipartFile file,

            @ApiParam(value = "图书名称", required = false, example = "斗破苍穹")
            @RequestParam(value = "bookName", required = false) String bookName,

            @ApiParam(value = "作者名称", required = false, example = "天蚕土豆")
            @RequestParam(value = "author", required = false) String author,

            @ApiParam(value = "图书简介", required = false, example = "这是一部玄幻小说...")
            @RequestParam(value = "description", required = false) String description) {

        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        if (file.isEmpty()) {
            return Result.error(ResultCode.FILE_NOT_SELECTED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".txt")) {
            return Result.error(ResultCode.FILE_FORMAT_ERROR);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error(ResultCode.FILE_SIZE_EXCEED);
        }

        try {
            Book book = bookService.uploadBook(file, bookName, author, description, userId);
            return Result.success("上传成功", book);
        } catch (Exception e) {
            log.error("上传图书失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/list")
    @AuthToken
    @ApiOperation(value = "获取图书列表", notes = "分页查询图书列表，支持关键词搜索")
    public Result<Map<String, Object>> getBookList(@RequestBody(required = false) BookListRequest request) {
        if (request == null) {
            request = new BookListRequest();
        }
        Map<String, Object> result = bookService.getBookList(
                request.getKeyword(),
                request.getPage() != null ? request.getPage() : 1,
                request.getSize() != null ? request.getSize() : 20
        );
        return Result.success(result);
    }

    @PostMapping("/detail")
    @AuthToken
    @ApiOperation(value = "获取图书详情", notes = "根据ID获取图书详细信息")
    public Result<Book> getBookDetail(@RequestBody BookDetailRequest request) {
        Book book = bookService.getBookById(request.getId());
        if (book == null) {
            return Result.error(ResultCode.BOOK_NOT_FOUND);
        }
        return Result.success(book);
    }

    @PostMapping("/chapters")
    @AuthToken
    @ApiOperation(value = "获取章节列表", notes = "获取图书的所有章节列表（不含内容）")
    public Result<List<Chapter>> getChapterList(@RequestBody ChapterListRequest request) {
        List<Chapter> chapters = bookService.getChapterList(request.getBookId());
        return Result.success(chapters);
    }

    @PostMapping("/chapter/content")
    @AuthToken
    @ApiOperation(value = "获取章节内容", notes = "获取指定章节的详细内容")
    public Result<Chapter> getChapterContent(@RequestBody ChapterContentRequest request) {
        Chapter chapter = bookService.getChapterById(request.getChapterId());
        if (chapter == null) {
            return Result.error(ResultCode.CHAPTER_NOT_FOUND);
        }
        Chapter fullChapter = bookService.getChapterContent(chapter.getBookId(), chapter.getChapterNum());
        if (fullChapter == null) {
            return Result.error(ResultCode.CHAPTER_NOT_FOUND);
        }
        return Result.success(fullChapter);
    }

    @PostMapping("/delete")
    @AuthToken
    @ApiOperation(value = "删除图书", notes = "根据ID删除图书，仅图书所有者可以删除")
    public Result<String> deleteBook(@RequestBody DeleteBookRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        try {
            bookService.deleteBook(request.getId(), userId);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除图书失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    @AuthToken
    @ApiOperation(value = "更新图书信息", notes = "更新图书的基本信息（书名、作者、简介等）")
    public Result<Book> updateBook(@RequestBody UpdateBookRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }
        
        if (request.getId() == null) {
            return Result.error("图书ID不能为空");
        }
        
        try {
            Book updatedBook = bookService.updateBook(
                request.getId(),
                request.getBookName(),
                request.getAuthor(),
                request.getDescription(),
                userId
            );
            
            if (updatedBook == null) {
                return Result.error("图书不存在或无权修改");
            }
            
            return Result.success("更新成功", updatedBook);
        } catch (Exception e) {
            log.error("更新图书失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @ApiModel(description = "图书列表请求")
    public static class BookListRequest {
        @ApiModelProperty(value = "搜索关键词", example = "斗破苍穹")
        private String keyword;

        @ApiModelProperty(value = "页码", example = "1")
        private Integer page;

        @ApiModelProperty(value = "每页数量", example = "20")
        private Integer size;

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }

    @ApiModel(description = "图书详情请求")
    public static class BookDetailRequest {
        @ApiModelProperty(value = "图书ID", required = true, example = "1")
        @NotNull(message = "图书ID不能为空")
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    @ApiModel(description = "章节列表请求")
    public static class ChapterListRequest {
        @ApiModelProperty(value = "图书ID", required = true, example = "1")
        @NotNull(message = "图书ID不能为空")
        private Long bookId;

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
    }

    @ApiModel(description = "章节内容请求")
    public static class ChapterContentRequest {
        @ApiModelProperty(value = "章节ID", required = true, example = "1")
        @NotNull(message = "章节ID不能为空")
        private Long chapterId;

        public Long getChapterId() { return chapterId; }
        public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    }

    @ApiModel(description = "删除图书请求")
    public static class DeleteBookRequest {
        @ApiModelProperty(value = "图书ID", required = true, example = "1")
        @NotNull(message = "图书ID不能为空")
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    @ApiModel(description = "更新图书请求")
    public static class UpdateBookRequest {
        @ApiModelProperty(value = "图书ID", required = true, example = "1")
        @NotNull(message = "图书ID不能为空")
        private Long id;

        @ApiModelProperty(value = "图书名称", example = "斗破苍穹")
        private String bookName;

        @ApiModelProperty(value = "作者名称", example = "天蚕土豆")
        private String author;

        @ApiModelProperty(value = "图书简介", example = "这是一部玄幻小说...")
        private String description;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getBookName() { return bookName; }
        public void setBookName(String bookName) { this.bookName = bookName; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}