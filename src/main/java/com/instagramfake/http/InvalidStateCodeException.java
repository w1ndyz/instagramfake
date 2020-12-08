package com.instagramfake.http;

public class InvalidStateCodeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int code;
    private final String content;

    public InvalidStateCodeException(int code, String msg, String content) {
        super(msg);
        this.code = code;
        this.content = content;
    }

    public int getCode() {
        return code;
    }

    public String getContent() {
        return content;
    }
}
