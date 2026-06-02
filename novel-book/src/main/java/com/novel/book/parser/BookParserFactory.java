package com.novel.book.parser;

import com.novel.common.parser.BookParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class BookParserFactory {

    @Autowired
    private List<BookParser> parsers;

    private Map<String, BookParser> parserMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        parserMap = parsers.stream()
                .collect(Collectors.toMap(
                        BookParser::getFileType,
                        parser -> parser,
                        (existing, replacement) -> existing
                ));
    }

    public BookParser getParser(String fileType) {
        BookParser parser = parserMap.get(fileType.toLowerCase());
        if (parser == null) {
            throw new RuntimeException("不支持的文件格式: " + fileType + ", 支持的格式: " + parserMap.keySet());
        }
        return parser;
    }

    public boolean isSupported(String fileType) {
        return parserMap.containsKey(fileType.toLowerCase());
    }
}
