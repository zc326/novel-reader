package com.novel.bookshelf.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.novel.bookshelf.mapper.BookshelfMapper;
import com.novel.common.entity.Bookshelf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class BookshelfService extends ServiceImpl<BookshelfMapper, Bookshelf> {

    public List<Bookshelf> getByUserId(Long userId) {
        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("read_time");
        return baseMapper.selectList(wrapper);
    }

    public boolean isInBookshelf(Long userId, Long bookId) {
        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("book_id", bookId);
        return baseMapper.selectCount(wrapper) > 0;
    }

    public boolean removeFromBookshelf(Long userId, Long bookId) {
        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("book_id", bookId);

        Bookshelf bookshelf = baseMapper.selectOne(wrapper);
        if (bookshelf == null) {
            return false;
        }

        baseMapper.deleteById(bookshelf.getId());
        return true;
    }

    public boolean updateProgress(Long userId, Long bookId, Integer chapter, Integer progress) {
        QueryWrapper<Bookshelf> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("book_id", bookId);

        Bookshelf bookshelf = baseMapper.selectOne(wrapper);
        if (bookshelf == null) {
            return false;
        }

        bookshelf.setLastReadChapter(chapter);
        bookshelf.setReadProgress(progress);
        bookshelf.setReadTime(LocalDateTime.now());

        baseMapper.updateById(bookshelf);
        return true;
    }
}
