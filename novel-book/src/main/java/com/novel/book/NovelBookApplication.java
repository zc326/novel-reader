package com.novel.book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.novel.book", "com.novel.common"})
public class NovelBookApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovelBookApplication.class, args);
    }
}