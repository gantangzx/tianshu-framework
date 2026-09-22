package com.gantang.tianshu.user.core;

public interface UserContext {

    String AUTHENTICATION_PREFIX = "Bearer ";

    String UID = "uid";

    UserDetails getUserDetail();

    String getToken();

    String getIp();
}
