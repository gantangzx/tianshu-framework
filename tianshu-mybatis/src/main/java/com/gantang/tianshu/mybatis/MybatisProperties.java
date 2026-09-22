package com.gantang.tianshu.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis 增强配置属性。
 *
 * @author gantang
 */
@ConfigurationProperties(prefix = MybatisProperties.PREFIX)
public class MybatisProperties {

    public static final String PREFIX = "tianshu.mybatis";

    /** 数据库类型，用于分页方言。 */
    private DbType dbType = DbType.MYSQL;

    /** 是否启用分页插件。 */
    private boolean paginationEnabled = true;

    /** 是否启用乐观锁插件。 */
    private boolean optimisticLockEnabled = true;

    public DbType getDbType() {
        return dbType;
    }

    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }

    public boolean isPaginationEnabled() {
        return paginationEnabled;
    }

    public void setPaginationEnabled(boolean paginationEnabled) {
        this.paginationEnabled = paginationEnabled;
    }

    public boolean isOptimisticLockEnabled() {
        return optimisticLockEnabled;
    }

    public void setOptimisticLockEnabled(boolean optimisticLockEnabled) {
        this.optimisticLockEnabled = optimisticLockEnabled;
    }
}
