package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.auth.adapter.port.TokenPort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT认证过滤器装配
 */
@Configuration
@EnableConfigurationProperties(AuthFilterProperties.class)
public class JwtAuthenticationFilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilter(TokenPort tokenPort,
                                                                                    AuthFilterProperties properties,
                                                                                    ObjectMapper objectMapper) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JwtAuthenticationFilter(tokenPort, properties, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
