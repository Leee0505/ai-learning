package com.ticket.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.ticket.util.SecurityUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    // ThreadLocal flag: set by JwtAuthenticationFilter when current user is ROLE_ADMIN
    public static final ThreadLocal<Boolean> ADMIN_BYPASS = new ThreadLocal<>();

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                if (Boolean.TRUE.equals(ADMIN_BYPASS.get())) return null; // return null = skip filter
                Long tenantId = SecurityUtils.getCurrentTenantId();
                return new LongValue(tenantId != null ? tenantId : 1L);
            }

            @Override
            public String getTenantIdColumn() { return "tenant_id"; }

            @Override
            public boolean ignoreTable(String tableName) { return "tenant".equalsIgnoreCase(tableName); }
        }));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                long now = System.currentTimeMillis();
                Long userId = SecurityUtils.getCurrentUserId();
                this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
                this.strictInsertFill(metaObject, "createdDate", Long.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                Long userId = SecurityUtils.getCurrentUserId();
                this.strictUpdateFill(metaObject, "lastModifiedBy", Long.class, userId);
                this.strictUpdateFill(metaObject, "lastModifiedDate", Long.class, System.currentTimeMillis());
            }
        };
    }
}
