package com.program.bookie.models;

import java.io.Serializable;

public class LoginData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    public LoginData() {}

    public LoginData(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "LoginData{username='" + username + "'}"; // NIE loguj hasła!
    }
}
