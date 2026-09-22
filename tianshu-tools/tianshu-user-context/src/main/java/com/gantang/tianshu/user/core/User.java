package com.gantang.tianshu.user.core;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class User implements UserDetails{

    private Long id;

    private String username;

    private String account;

    private String phone;


    @Override
    public Long getUserId() {
        return id;
    }

    @Override
    public String getUserName() {
        return username;
    }

    @Override
    public String getAccount() {
        return account;
    }

    @Override
    public String getPhone() {
        return phone;
    }
}
