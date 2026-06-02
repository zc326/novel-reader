package com.novel.book.parser;

import com.novel.common.entity.Chapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TxtBookParser implements com.novel.common.parser.BookParser {

    // 支持多种章节标题格式，捕获组1=序号，捕获组2=标题
    private static final Pattern CHAPTER_PATTERN_1 = Pattern.compile("^\\s*第([一二三四五六七八九十百千万\\d]+)[章节卷集部篇回]\\s+(.+)");
    private static final Pattern CHAPTER_PATTERN_2 = Pattern.compile("^\\s*(Chapter|CHAPTER|chap)[\\s\\.]*([\\d]+)[\\s\\.\\uff0e]*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAPTER_PATTERN_3 = Pattern.compile("^\\s*([\\d]+)[\\.、]\\s+(.+)");

    @Override
    public boolean support(String fileType) {
        return "txt".equalsIgnoreCase(fileType);
    }

    @Override
    public String getFileType() {
        return "txt";
    }

    @Override
    public List<Chapter> parse(InputStream input, Long bookId) throws IOException {
        // 检测编码并回退流指针
        Charset charset = detectCharset(input);
        
        // 使用检测到的编码创建Reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset));

        List<Chapter> chapters = new ArrayList<>();
        StringBuilder currentContent = new StringBuilder();
        String currentTitle = null;
        int chapterNum = 0;
        String line;
        boolean hasFoundFirstChapter = false;

        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            
            // 检测是否为章节标题
            String chapterTitle = detectChapterTitle(trimmedLine);

            if (chapterTitle != null) {
                // 如果已经找到过章节，保存上一章的内容
                if (hasFoundFirstChapter && currentTitle != null) {
                    Chapter chapter = createChapter(bookId, chapterNum, currentTitle, currentContent.toString());
                    chapters.add(chapter);
                    log.info("解析章节: {} - 内容长度: {}", currentTitle, chapter.getContent().length());
                    currentContent = new StringBuilder();
                }
                
                // 开始新章节
                chapterNum++;
                currentTitle = chapterTitle;
                hasFoundFirstChapter = true;
            } else {
                // 累积章节内容（包括第一章之前的内容）
                if (!trimmedLine.isEmpty()) {
                    currentContent.append(line).append("\n");
                } else if (currentContent.length() > 0) {
                    // 保留段落间的空行
                    currentContent.append("\n");
                }
            }
        }

        // 保存最后一章
        if (currentTitle != null) {
            Chapter lastChapter = createChapter(bookId, chapterNum, currentTitle, currentContent.toString());
            chapters.add(lastChapter);
            log.info("解析章节: {} - 内容长度: {}", currentTitle, lastChapter.getContent().length());
        }

        // 如果没有检测到任何章节，将整个文件作为一章
        if (chapters.isEmpty()) {
            String fullContent = currentContent.toString();
            if (fullContent.isEmpty()) {
                log.warn("未能解析到任何内容，返回空章节");
                fullContent = "暂无内容";
            }
            Chapter chapter = createChapter(bookId, 1, "第一章", fullContent);
            chapters.add(chapter);
            log.info("未检测到章节标题，将整个文件作为第一章 - 内容长度: {}", chapter.getContent().length());
        }

        reader.close();
        log.info("总共解析出 {} 个章节", chapters.size());
        return chapters;
    }

    /**
     * 检测章节标题，返回纯标题（不包含"第一章"等前缀）
     * @param line 文本行
     * @return 纯标题，如"山边小村"；如果不是章节标题则返回null
     */
    private String detectChapterTitle(String line) {
        // 模式1: 第X章/节/卷... 标题
        Matcher matcher1 = CHAPTER_PATTERN_1.matcher(line);
        if (matcher1.matches()) {
            return matcher1.group(2).trim(); // 只返回标题部分
        }

        // 模式2: Chapter X. 标题
        Matcher matcher2 = CHAPTER_PATTERN_2.matcher(line);
        if (matcher2.matches()) {
            return matcher2.group(3).trim(); // 只返回标题部分
        }

        // 模式3: 1. 标题 或 1、标题
        Matcher matcher3 = CHAPTER_PATTERN_3.matcher(line);
        if (matcher3.matches()) {
            return matcher3.group(2).trim(); // 只返回标题部分
        }

        return null;
    }

    private Chapter createChapter(Long bookId, int chapterNum, String chapterTitle, String content) {
        Chapter chapter = new Chapter();
        chapter.setBookId(bookId);
        chapter.setChapterNum(chapterNum);
        chapter.setChapterTitle(chapterTitle);
        chapter.setContent(cleanContent(content));
        chapter.setWordCount(calculateWordCount(content));
        chapter.setContentSize((long) content.getBytes(StandardCharsets.UTF_8).length);
        chapter.setStatus(1);
        chapter.setCreateTime(LocalDateTime.now());
        return chapter;
    }

    private String cleanContent(String content) {
        if (content == null) return "";
        return content.replaceAll("\\r\\n\\n+", "\n\n").replaceAll("\\n\\n+", "\n\n").trim();
    }

    private int calculateWordCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.replaceAll("\\s", "").length();
    }

    private Charset detectCharset(InputStream input) throws IOException {
        // 使用足够大的 PushbackInputStream 缓冲区
        PushbackInputStream pushbackInputStream = new PushbackInputStream(input, 8192);
        
        // 先读取少量字节检查BOM头
        byte[] bomCheck = new byte[4];
        int bomRead = pushbackInputStream.read(bomCheck);
        
        if (bomRead > 0) {
            pushbackInputStream.unread(bomCheck, 0, bomRead);
        }

        // 检查BOM头
        if (bomRead >= 2 && bomCheck[0] == (byte) 0xFF && bomCheck[1] == (byte) 0xFE) {
            log.info("检测到文件编码: UTF-16LE (BOM)");
            return StandardCharsets.UTF_16LE;
        } else if (bomRead >= 2 && bomCheck[0] == (byte) 0xFE && bomCheck[1] == (byte) 0xFF) {
            log.info("检测到文件编码: UTF-16BE (BOM)");
            return StandardCharsets.UTF_16BE;
        } else if (bomRead >= 3 && bomCheck[0] == (byte) 0xEF && bomCheck[1] == (byte) 0xBB && bomCheck[2] == (byte) 0xBF) {
            log.info("检测到文件编码: UTF-8 (BOM)");
            return StandardCharsets.UTF_8;
        }

        // 没有BOM头，读取更多内容进行编码检测
        byte[] buffer = new byte[8192];
        int bytesRead = pushbackInputStream.read(buffer);
        
        if (bytesRead > 0) {
            pushbackInputStream.unread(buffer, 0, bytesRead);
            
            // 尝试用UTF-8解码
            String utf8Str = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            // 尝试用GBK解码
            String gbkStr = new String(buffer, 0, bytesRead, Charset.forName("GBK"));
            
            // 统计无效字符（替换字符 \ufffd）数量
            long utf8InvalidCount = utf8Str.chars().filter(c -> c == '\ufffd').count();
            long gbkInvalidCount = gbkStr.chars().filter(c -> c == '\ufffd').count();
            
            // 选择无效字符更少的编码
            if (utf8InvalidCount <= gbkInvalidCount) {
                log.info("检测到文件编码: UTF-8 (内容特征)");
                return StandardCharsets.UTF_8;
            } else {
                log.info("检测到文件编码: GBK (内容特征)");
                return Charset.forName("GBK");
            }
        }

        log.info("默认使用UTF-8编码");
        return StandardCharsets.UTF_8;
    }
}