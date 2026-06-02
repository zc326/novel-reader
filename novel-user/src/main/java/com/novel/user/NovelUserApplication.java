package com.novel.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.novel.user", "com.novel.common"})
public class NovelUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovelUserApplication.class, args);
    }
}