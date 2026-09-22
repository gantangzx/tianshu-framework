package com.gantang.tianshu.user.core;

import java.io.Serializable;

public interface UserDetails extends Serializable {

    Long getUserId();

    String getUserName();

    String getAccount();

    String getPhone();
}
