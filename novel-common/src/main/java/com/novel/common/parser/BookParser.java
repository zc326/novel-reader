package com.novel.common.parser;

import com.novel.common.entity.Chapter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface BookParser {

    boolean support(String fileType);

    List<Chapter> parse(InputStream input, Long bookId) throws IOException;

    default String getFileType() {
        return "txt";
    }
}
