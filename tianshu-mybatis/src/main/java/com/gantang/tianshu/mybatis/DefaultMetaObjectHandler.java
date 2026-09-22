package com.gantang.tianshu.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充处理器。
 *
 * @author gantang
 */
public class DefaultMetaObjectHandler implements MetaObjectHandler {

    private final CurrentUserResolver userResolver;

    public DefaultMetaObjectHandler(CurrentUserResolver userResolver) {
        this.userResolver = userResolver;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = this.userResolver.currentUserId();
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "status", StatusEnum.class, StatusEnum.NORMAL);
        if (userId != null) {
            strictInsertFill(metaObject, "createUser", Long.class, userId);
            strictInsertFill(metaObject, "updateUser", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = this.userResolver.currentUserId();
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        if (userId != null) {
            strictUpdateFill(metaObject, "updateUser", Long.class, userId);
        }
    }
}
