package com.novel.book.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novel.book.mapper.BookMapper;
import com.novel.book.mapper.ChapterMapper;
import com.novel.book.parser.BookParserFactory;
import com.novel.common.entity.Book;
import com.novel.common.entity.Chapter;
import com.novel.common.parser.BookParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BookService {

    private static final String HOT_BOOKS_KEY = "hot:books:week";

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private BookParserFactory bookParserFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    public Book uploadBook(MultipartFile file, String bookName, String author, String description, Long userId) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String fileType = "txt";

        if (originalFilename != null && originalFilename.contains(".")) {
            fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }

        // 只读取一次文件内容，后续元数据提取、编码检测、解析均复用该 byte[]
        byte[] fileBytes = file.getBytes();

        // 从文件内容中提取书名、作者和简介
        BookMetadata metadata = extractBookMetadata(fileBytes, fileType);
        log.info("文件元数据提取结果 - 书名: {}, 作者: {}, 简介长度: {}", 
                metadata.getBookName(), metadata.getAuthor(), 
                metadata.getDescription() != null ? metadata.getDescription().length() : 0);
        
        // 优先级：文件提取 > 用户输入 > 默认值
        // 如果文件提取到了书名，始终使用文件中的（即使用户也提供了）
        if (metadata.getBookName() != null && !metadata.getBookName().trim().isEmpty()) {
            bookName = metadata.getBookName();
            log.info("使用文件中提取的书名: {}", bookName);
        } else {
            // 文件没提取到，才使用用户输入的
            if (bookName == null || bookName.trim().isEmpty()) {
                bookName = originalFilename != null 
                    ? originalFilename.substring(0, originalFilename.lastIndexOf(".")) 
                    : "未命名书籍";
                log.info("使用文件名作为书名: {}", bookName);
            } else {
                log.info("使用用户提供的书名: {}", bookName);
            }
        }
        
        // 如果文件提取到了作者，始终使用文件中的
        if (metadata.getAuthor() != null && !metadata.getAuthor().trim().isEmpty()) {
            author = metadata.getAuthor();
            log.info("使用文件中提取的作者: {}", author);
        } else {
            // 文件没提取到，才使用用户输入的
            if (author == null || author.trim().isEmpty()) {
                author = "未知作者";
                log.info("使用默认作者: {}", author);
            } else {
                log.info("使用用户提供的作者: {}", author);
            }
        }
        
        // 如果文件提取到了简介，始终使用文件中的
        if (metadata.getDescription() != null && !metadata.getDescription().trim().isEmpty()) {
            description = metadata.getDescription();
            log.info("使用文件中提取的简介");
        } else {
            // 文件没提取到，才使用用户输入的
            if (description == null || description.trim().isEmpty()) {
                description = "";
            }
            log.info("使用用户提供的简介或空值");
        }

        Book book = new Book();
        book.setBookName(bookName.trim());
        book.setAuthor(author.trim());
        book.setDescription(description.trim());
        book.setUploadUserId(userId);
        book.setFileSize(file.getSize());
        book.setStatus(1);
        book.setCreateTime(LocalDateTime.now());
        bookMapper.insert(book);

        try {
            InputStream inputStream = detectAndGetInputStream(fileBytes);
            BookParser parser = bookParserFactory.getParser(fileType);
            List<Chapter> chapters = parser.parse(inputStream, book.getId());

            int totalWordCount = 0;
            for (Chapter chapter : chapters) {
                chapterMapper.insert(chapter);
                totalWordCount += chapter.getWordCount();
            }

            book.setChapterCount(chapters.size());
            book.setTotalWordCount(totalWordCount);
            bookMapper.updateById(book);

            // 添加到热门图书
            redisTemplate.opsForZSet().add(HOT_BOOKS_KEY, book.getId().toString(), 0);
            
            // 清除图书列表缓存，确保下次查询能获取最新数据
            clearBookListCache();
            
            log.info("图书上传成功，ID: {}, 书名: {}", book.getId(), book.getBookName());

        } catch (Exception e) {
            log.error("解析图书失败", e);
            throw new Exception("图书解析失败: " + e.getMessage());
        }

        return book;
    }

    private InputStream detectAndGetInputStream(byte[] fileBytes) throws IOException {
        byte[] headerBytes = new byte[3];
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);
        byteArrayInputStream.read(headerBytes);

        Charset charset = StandardCharsets.UTF_8;
        if (headerBytes[0] == (byte) 0xFF && headerBytes[1] == (byte) 0xFE) {
            charset = StandardCharsets.UTF_16LE;
        } else if (headerBytes[0] == (byte) 0xFE && headerBytes[1] == (byte) 0xFF) {
            charset = StandardCharsets.UTF_16BE;
        } else {
            String content = new String(fileBytes, 0, Math.min(10000, fileBytes.length), StandardCharsets.UTF_8);
            if (!content.equals(new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8))) {
                charset = Charset.forName("GBK");
            }
        }

        return new ByteArrayInputStream(fileBytes);
    }

    /**
     * 从文件内容中提取书名、作者和简介
     */
    private BookMetadata extractBookMetadata(byte[] fileBytes, String fileType) {
        BookMetadata metadata = new BookMetadata();
        
        if (!"txt".equalsIgnoreCase(fileType)) {
            return metadata;
        }
        
        try {
            // 复用已读取的文件内容，避免重复读取大文件
            byte[] bytes = fileBytes;
            int readLength = Math.min(2000, bytes.length);
            
            // 检测编码
            Charset charset = StandardCharsets.UTF_8;
            if (readLength >= 2) {
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                    charset = StandardCharsets.UTF_16LE;
                } else if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                    charset = StandardCharsets.UTF_16BE;
                } else {
                    // 简单检测是否为GBK
                    String testStr = new String(bytes, 0, readLength, StandardCharsets.UTF_8);
                    if (testStr.chars().filter(c -> c == '\ufffd').count() > 10) {
                        charset = Charset.forName("GBK");
                    }
                }
            }
            
            String content = new String(bytes, 0, readLength, charset);
            log.debug("文件前200字节: {}", content.substring(0, Math.min(200, content.length())).replaceAll("\n", "\\n"));
            
            String[] lines = content.split("\n");
            
            // 提取书名和作者（通常在前几行）
            for (int i = 0; i < Math.min(20, lines.length); i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                log.debug("检查第{}行: {}", i+1, line);
                
                // 匹配格式1：书名 作者：xxx 或 作者: xxx
                java.util.regex.Matcher matcher1 = java.util.regex.Pattern.compile(
                    "^(.+?)\\s*作者[：:]\\s*(.+)$").matcher(line);
                
                if (matcher1.matches()) {
                    metadata.setBookName(matcher1.group(1).trim());
                    metadata.setAuthor(matcher1.group(2).trim());
                    log.info("从文件中提取到 - 书名: {}, 作者: {}", metadata.getBookName(), metadata.getAuthor());
                    break;
                }
                
                // 匹配格式2：作者：xxx（单独一行）
                java.util.regex.Matcher matcher2 = java.util.regex.Pattern.compile(
                    "^作者[：:]\\s*(.+)$").matcher(line);
                
                if (matcher2.matches() && metadata.getBookName() != null) {
                    metadata.setAuthor(matcher2.group(1).trim());
                    log.info("从文件中提取到作者: {}", metadata.getAuthor());
                    break;
                }
            }
            
            // 提取简介（查找“内容简介”、“简介”等关键词）
            StringBuilder description = new StringBuilder();
            boolean inDescription = false;
            for (String line : lines) {
                String trimmed = line.trim();
                
                if (trimmed.matches(".*(?:内容简介|简介|内容提要|故事梗概)[：:].*")) {
                    inDescription = true;
                    // 如果冒号后面有内容，也加入
                    String afterColon = trimmed.replaceAll(".*(?:内容简介|简介|内容提要|故事梗概)[：:]\\s*", "");
                    if (!afterColon.isEmpty() && !afterColon.equals(trimmed)) {
                        description.append(afterColon).append("\n");
                    }
                    continue;
                }
                
                if (inDescription) {
                    // 遇到空行或新的章节标题时停止
                    if (trimmed.isEmpty() && description.length() > 50) {
                        break;
                    }
                    if (trimmed.matches("^第[一二三四五六七八九十百千万\\d]+[章节卷集部篇回].+")) {
                        break;
                    }
                    if (trimmed.matches("^第[一二三四五六七八九十百千万\\d]+卷.+")) {
                        break;
                    }
                    description.append(line).append("\n");
                }
            }
            
            if (description.length() > 0) {
                metadata.setDescription(description.toString().trim());
                log.info("从文件中提取到简介，长度: {}", metadata.getDescription().length());
            }
            
        } catch (Exception e) {
            log.warn("提取图书元数据失败: {}", e.getMessage());
        }
        
        return metadata;
    }
    
    /**
     * 图书元数据内部类
     */
    private static class BookMetadata {
        private String bookName;
        private String author;
        private String description;
        
        public String getBookName() { return bookName; }
        public void setBookName(String bookName) { this.bookName = bookName; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 清除图书列表缓存。
     * 使用 SCAN 游标迭代替代 KEYS，避免在生产 Redis 上阻塞实例。
     */
    private void clearBookListCache() {
        try {
            String pattern = "book:list:*";
            java.util.Set<String> keys = new java.util.HashSet<>();
            // SCAN 分批迭代，单次 SCAN_COUNT 100，避免一次性拉取全部 key 造成阻塞
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<java.util.Set<String>>) connection -> {
                org.springframework.data.redis.core.ScanOptions options = org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build();
                try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                } catch (java.io.IOException e) {
                    log.error("扫描图书列表缓存 key 失败", e);
                }
                return keys;
            });

            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("已清除 {} 个图书列表缓存", keys.size());
            }
        } catch (Exception e) {
            log.error("清除图书列表缓存失败", e);
        }
    }

    public Map<String, Object> getBookList(String keyword, int page, int size) {
        String cacheKey = "book:list:" + keyword + ":" + page + ":" + size;

        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Page<Book> bookPage = new Page<>(page, size);
        QueryWrapper<Book> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("book_name", keyword).or().like("author", keyword));
        }

        wrapper.orderByDesc("create_time");
        Page<Book> result = bookMapper.selectPage(bookPage, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("pages", result.getPages());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());

        redisTemplate.opsForValue().set(cacheKey, data, 1, TimeUnit.HOURS);

        return data;
    }

    public Book getBookById(Long id) {
        Book book = bookMapper.selectById(id);
        if (book != null) {
            redisTemplate.opsForZSet().incrementScore(HOT_BOOKS_KEY, id.toString(), 1);
        }
        return book;
    }

    public List<Chapter> getChapterList(Long bookId) {
        String cacheKey = "chapters:list:" + bookId;

        @SuppressWarnings("unchecked")
        List<Chapter> cached = (List<Chapter>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        QueryWrapper<Chapter> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", bookId);
        wrapper.eq("status", 1);
        wrapper.orderByAsc("chapter_num");

        List<Chapter> chapters = chapterMapper.selectList(wrapper);

        redisTemplate.opsForValue().set(cacheKey, chapters, 1, TimeUnit.HOURS);

        return chapters;
    }

    public Chapter getChapterContent(Long bookId, Integer chapterNum) {
        QueryWrapper<Chapter> wrapper = new QueryWrapper<>();
        wrapper.eq("book_id", bookId);
        wrapper.eq("chapter_num", chapterNum);
        wrapper.eq("status", 1);
        return chapterMapper.selectOne(wrapper);
    }

    public Chapter getChapterById(Long chapterId) {
        return chapterMapper.selectById(chapterId);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBook(Long bookId, Long userId) {
        Book book = bookMapper.selectById(bookId);

        if (book == null) {
            return false;
        }

        if (!book.getUploadUserId().equals(userId)) {
            log.warn("用户 {} 无权删除图书 {}", userId, bookId);
            return false;
        }

        QueryWrapper<Chapter> chapterWrapper = new QueryWrapper<>();
        chapterWrapper.eq("book_id", bookId);
        chapterMapper.delete(chapterWrapper);

        bookMapper.deleteById(bookId);

        // 清除缓存
        redisTemplate.delete("chapters:list:" + bookId);
        redisTemplate.opsForZSet().remove(HOT_BOOKS_KEY, bookId.toString());
        clearBookListCache();

        return true;
    }

    /**
     * 更新图书信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Book updateBook(Long bookId, String bookName, String author, String description, Long userId) {
        Book book = bookMapper.selectById(bookId);
        
        if (book == null) {
            log.warn("图书不存在: {}", bookId);
            return null;
        }
        
        // 检查权限：只有上传者可以修改
        if (!book.getUploadUserId().equals(userId)) {
            log.warn("用户 {} 无权修改图书 {}", userId, bookId);
            return null;
        }
        
        // 只更新非空字段
        if (bookName != null && !bookName.trim().isEmpty()) {
            book.setBookName(bookName.trim());
        }
        
        if (author != null && !author.trim().isEmpty()) {
            book.setAuthor(author.trim());
        }
        
        if (description != null) {
            book.setDescription(description.trim());
        }
        
        book.setUpdateTime(LocalDateTime.now());
        bookMapper.updateById(book);
        
        // 清除缓存
        clearBookListCache();
        
        log.info("图书 {} 更新成功", bookId);
        return book;
    }
}