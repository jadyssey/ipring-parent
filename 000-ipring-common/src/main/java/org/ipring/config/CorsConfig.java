package org.ipring.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 通用跨域配置
 *
 * @author lgj
 * @date 8/2/2023
 */
@Configuration
public class CorsConfig {

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 1 设置访问源地址。  表示接受任意域名的请求
        corsConfiguration.addAllowedOriginPattern(CorsConfiguration.ALL);
        // 2 设置访问源请求头。 放行哪些原始请求头部信息
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        // 3 设置访问源请求方法。 表明服务器支持的所有跨域请求的方法
        corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);

        //该字段可选。用来指定本次预检请求的有效期，单位为秒。在有效期间，不用发出另一条预检请求。
        corsConfiguration.setMaxAge(1800L);
        //该字段可选。如果服务器不要发送Cookie，删除该字段即可
        corsConfiguration.setAllowCredentials(true);
        return corsConfiguration;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对接口配置跨域设置
        source.registerCorsConfiguration("/**", buildConfig());
        //有多个filter时此处设置改CorsFilter的优先执行顺序
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}