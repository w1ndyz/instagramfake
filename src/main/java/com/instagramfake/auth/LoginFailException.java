package com.instagramfake.auth;

public class LoginFailException  extends RuntimeException {
    public LoginFailException(String message) {
        super(message);
    }
}
