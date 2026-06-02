package com.novel.common.result;

public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    USERNAME_EXIST(1001, "用户名已存在"),
    USERNAME_OR_PASSWORD_ERROR(1002, "用户名或密码错误"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),

    FILE_NOT_SELECTED(2001, "请选择要上传的文件"),
    FILE_FORMAT_ERROR(2002, "仅支持txt格式的文件"),
    FILE_SIZE_EXCEED(2003, "文件大小不能超过200MB"),
    BOOK_UPLOAD_FAILED(2004, "图书上传失败"),
    BOOK_NOT_FOUND(2005, "图书不存在"),
    CHAPTER_NOT_FOUND(2006, "章节不存在"),
    BOOK_DELETE_FAILED(2007, "删除失败，无权限或图书不存在"),

    BOOK_ALREADY_IN_BOOKSHELF(3001, "图书已在书架中"),
    BOOKSHELF_NOT_IN(3002, "移除失败"),
    BOOKSHELF_UPDATE_FAILED(3003, "更新失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}