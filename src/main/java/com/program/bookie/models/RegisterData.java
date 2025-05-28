package com.program.bookie.models;


import java.io.Serializable;

public class RegisterData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String firstname;
    private String lastname;
    private String username;
    private String password;



    public RegisterData(String firstname,String lastname,String username, String password) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.username = username;
        this.password = password;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
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