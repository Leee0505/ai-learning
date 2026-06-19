package com.ticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiMetricsInterceptor apiMetricsInterceptor;

    public WebMvcConfig(ApiMetricsInterceptor apiMetricsInterceptor) {
        this.apiMetricsInterceptor = apiMetricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiMetricsInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/admin/monitor/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Content-Length", "Accept")
                .allowCredentials(true);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect /swagger-ui.html to SpringDoc's actual Swagger UI entry point
        registry.addRedirectViewController("/swagger-ui.html", "/swagger-ui/index.html");
    }
}
