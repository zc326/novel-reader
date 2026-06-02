package com.novel.bookshelf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.novel.bookshelf", "com.novel.common"})
public class NovelBookshelfApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovelBookshelfApplication.class, args);
    }
}