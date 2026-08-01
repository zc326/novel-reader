package com.novel.book.controller;

import com.novel.book.service.BookService;
import com.novel.common.annotation.AuthToken;
import com.novel.common.entity.Book;
import com.novel.common.entity.Chapter;
import com.novel.common.exception.BusinessException;
import com.novel.common.result.Result;
import com.novel.common.result.ResultCode;
import com.novel.common.utils.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam("file") MultipartFile file,

            @ApiParam(value = "图书名称", required = false, example = "斗破苍穹")
            @RequestParam(value = "bookName", required = false) String bookName,

            @ApiParam(value = "作者名称", required = false, example = "天蚕土豆")
            @RequestParam(value = "author", required = false) String author,

            @ApiParam(value = "图书简介", required = false, example = "这是一部玄幻小说...")
            @RequestParam(value = "description", required = false) String description) {

        Long userId = requireUserId();

        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_NOT_SELECTED);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".txt")) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEED);
        }

        try {
            Book book = bookService.uploadBook(file, bookName, author, description, userId);
            return Result.success("上传成功", book);
        } catch (Exception e) {
            log.error("上传图书失败", e);
            throw new BusinessException(ResultCode.BOOK_UPLOAD_FAILED.getCode(), "上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @AuthToken
    @ApiOperation(value = "获取图书列表", notes = "分页查询图书列表，支持关键词搜索")
    public Result<Map<String, Object>> getBookList(
            @ApiParam(value = "搜索关键词", example = "斗破苍穹") @RequestParam(required = false) String keyword,
            @ApiParam(value = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页数量", example = "20") @RequestParam(defaultValue = "20") Integer size) {

        Map<String, Object> result = bookService.getBookList(
                keyword,
                page != null ? page : 1,
                size != null ? size : 20
        );
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @AuthToken
    @ApiOperation(value = "获取图书详情", notes = "根据ID获取图书详细信息")
    public Result<Book> getBookDetail(@ApiParam(value = "图书ID", required = true, example = "1") @PathVariable Long id) {
        Book book = bookService.getBookById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        return Result.success(book);
    }

    @GetMapping("/{bookId}/chapters")
    @AuthToken
    @ApiOperation(value = "获取章节列表", notes = "获取图书的所有章节列表（不含内容）")
    public Result<List<Chapter>> getChapterList(
            @ApiParam(value = "图书ID", required = true, example = "1") @PathVariable Long bookId) {
        List<Chapter> chapters = bookService.getChapterList(bookId);
        return Result.success(chapters);
    }

    @GetMapping("/chapter/{chapterId}")
    @AuthToken
    @ApiOperation(value = "获取章节内容", notes = "获取指定章节的详细内容")
    public Result<Chapter> getChapterContent(
            @ApiParam(value = "章节ID", required = true, example = "1") @PathVariable Long chapterId) {
        Chapter chapter = bookService.getChapterById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ResultCode.CHAPTER_NOT_FOUND);
        }
        Chapter fullChapter = bookService.getChapterContent(chapter.getBookId(), chapter.getChapterNum());
        if (fullChapter == null) {
            throw new BusinessException(ResultCode.CHAPTER_NOT_FOUND);
        }
        return Result.success(fullChapter);
    }

    @DeleteMapping("/{id}")
    @AuthToken
    @ApiOperation(value = "删除图书", notes = "根据ID删除图书，仅图书所有者可以删除")
    public Result<String> deleteBook(@ApiParam(value = "图书ID", required = true, example = "1") @PathVariable Long id) {
        Long userId = requireUserId();
        boolean success = bookService.deleteBook(id, userId);
        if (!success) {
            throw new BusinessException(ResultCode.BOOK_DELETE_FAILED);
        }
        return Result.success("删除成功");
    }

    @PutMapping("/{id}")
    @AuthToken
    @ApiOperation(value = "更新图书信息", notes = "更新图书的基本信息（书名、作者、简介等）")
    public Result<Book> updateBook(
            @ApiParam(value = "图书ID", required = true, example = "1") @PathVariable Long id,
            @ApiParam(value = "图书名称", example = "斗破苍穹") @RequestParam(required = false) String bookName,
            @ApiParam(value = "作者名称", example = "天蚕土豆") @RequestParam(required = false) String author,
            @ApiParam(value = "图书简介", example = "这是一部玄幻小说...") @RequestParam(required = false) String description) {

        Long userId = requireUserId();

        Book updatedBook = bookService.updateBook(id, bookName, author, description, userId);
        if (updatedBook == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND.getCode(), "图书不存在或无权修改");
        }
        return Result.success("更新成功", updatedBook);
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
