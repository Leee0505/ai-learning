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

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(SecurityUtils.getCurrentTenantId());
            }

            @Override
            public String getTenantIdColumn() { return "tenant_id"; }

            @Override
            public boolean ignoreTable(String tableName) {
                // System / cross-tenant tables — always skip interceptor.
                // Tenant isolation for these is handled manually in the service layer
                // (supports tenant_id=NULL = system default visible to all tenants).
                if ("user".equalsIgnoreCase(tableName)
                        || "tenant".equalsIgnoreCase(tableName)
                        || "invite_token".equalsIgnoreCase(tableName)
                        || "reply_template".equalsIgnoreCase(tableName)
                        || "knowledge_article".equalsIgnoreCase(tableName)
                        || "survey_template".equalsIgnoreCase(tableName)
                        || "sla_config".equalsIgnoreCase(tableName)
                        // Survey child tables inherit tenant scope from parent
                        || "survey_page".equalsIgnoreCase(tableName)
                        || "survey_section".equalsIgnoreCase(tableName)
                        || "survey_question".equalsIgnoreCase(tableName)
                        || "survey_visibility_rule".equalsIgnoreCase(tableName)
                        || "survey_instance_page".equalsIgnoreCase(tableName)
                        || "survey_answer".equalsIgnoreCase(tableName)) {
                    return true;
                }
                // Regular tenant-scoped tables (ticket, survey_instance, notification, etc.)
                // Superadmin sees all tenants; everyone else gets tenant_id filter.
                if (SecurityUtils.isAdmin()) {
                    return SecurityUtils.getCurrentTenantIdOrNull() == null;
                }
                return false;
            }
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
