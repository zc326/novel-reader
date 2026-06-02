-- ============================================
-- 小说阅读器数据库 - 支持逻辑删除
-- ============================================
CREATE DATABASE IF NOT EXISTS novel_reader DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE novel_reader;

-- ============================================
-- 用户表（支持逻辑删除）
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `email` VARCHAR(100) COMMENT '邮箱',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：1正常，0禁用',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_username` (`username`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 图书表（支持逻辑删除）
-- ============================================
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '图书ID',
    `book_name` VARCHAR(255) NOT NULL COMMENT '书名',
    `author` VARCHAR(100) COMMENT '作者',
    `description` TEXT COMMENT '简介',
    `cover_image` VARCHAR(500) COMMENT '封面图片',
    `upload_user_id` BIGINT(20) NOT NULL COMMENT '上传用户ID',
    `chapter_count` INT(11) DEFAULT 0 COMMENT '总章节数',
    `total_word_count` INT(11) DEFAULT 0 COMMENT '总字数',
    `file_size` BIGINT(20) COMMENT '原始文件大小(字节)',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：1正常，0下架',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_book_name` (`book_name`),
    INDEX `idx_author` (`author`),
    INDEX `idx_upload_user` (`upload_user_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- ============================================
-- 章节表（支持逻辑删除）
-- ============================================
DROP TABLE IF EXISTS `chapter`;
CREATE TABLE `chapter` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '章节ID',
    `book_id` BIGINT(20) NOT NULL COMMENT '所属图书ID',
    `chapter_num` INT(11) NOT NULL COMMENT '章节序号(从1开始)',
    `chapter_title` VARCHAR(255) COMMENT '章节标题',
    `content` LONGTEXT COMMENT '章节内容',
    `word_count` INT(11) DEFAULT 0 COMMENT '章节字数',
    `content_size` BIGINT(20) DEFAULT 0 COMMENT '内容字节数',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态：1正常，0屏蔽',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_chapter` (`book_id`, `chapter_num`),
    INDEX `idx_book_id` (`book_id`),
    INDEX `idx_book_num` (`book_id`, `chapter_num`),
    INDEX `idx_deleted` (`deleted`),
    FOREIGN KEY (`book_id`) REFERENCES `book`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='章节表';

-- ============================================
-- 书架表（支持逻辑删除）
-- ============================================
DROP TABLE IF EXISTS `bookshelf`;
CREATE TABLE `bookshelf` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `book_id` BIGINT(20) NOT NULL COMMENT '图书ID',
    `last_read_chapter` INT(11) DEFAULT 1 COMMENT '最后阅读章节',
    `read_progress` INT(11) DEFAULT 0 COMMENT '阅读进度百分比',
    `read_time` DATETIME COMMENT '最后阅读时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_book_id` (`book_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='书架表';

-- ============================================
-- 测试数据（密码都是123456）
-- ============================================
INSERT INTO `user` (`username`, `password`, `email`, `nickname`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@novel.com', '管理员'),
('test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'test@novel.com', '测试用户');