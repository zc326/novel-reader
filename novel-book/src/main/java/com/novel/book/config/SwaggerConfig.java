package com.novel.book.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket bookApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("book-api")
                .apiInfo(apiInfo())
                .securitySchemes(securitySchemes())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.novel.book.controller"))
                .paths(PathSelectors.ant("/book/**"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("小说阅读器图书服务API")
                .description("图书上传、查询、章节管理接口")
                .version("1.0.0")
                .contact(new Contact("novel-reader", "", ""))
                .build();
    }

    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> securitySchemes = new ArrayList<>();
        securitySchemes.add(new ApiKey("Authorization", "Authorization", "header"));
        return securitySchemes;
    }
}