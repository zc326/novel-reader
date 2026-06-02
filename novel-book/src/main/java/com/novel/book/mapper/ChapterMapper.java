package com.novel.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.common.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {
}
