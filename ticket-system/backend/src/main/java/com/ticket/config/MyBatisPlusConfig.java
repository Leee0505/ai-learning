package com.ticket.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    // ── Pagination plugin — required for Page<T>.selectPage() to add LIMIT/OFFSET ──
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                long now = System.currentTimeMillis();
                Long userId = getCurrentUserId();
                this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
                this.strictInsertFill(metaObject, "createdDate", Long.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                Long userId = getCurrentUserId();
                this.strictUpdateFill(metaObject, "lastModifiedBy", Long.class, userId);
                this.strictUpdateFill(metaObject, "lastModifiedDate", Long.class, System.currentTimeMillis());
            }

            private Long getCurrentUserId() {
                try {
                    var auth = org.springframework.security.core.context.SecurityContextHolder
                            .getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof com.ticket.security.UserDetailsImpl principal) {
                        return principal.getUserId();
                    }
                } catch (Exception ignored) {}
                return 0L;
            }
        };
    }
}
